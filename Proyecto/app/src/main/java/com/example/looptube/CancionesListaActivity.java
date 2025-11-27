package com.example.looptube;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.looptube.Adaptadores.CancionesListaAdapter;
import com.example.looptube.models.Cancion;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class CancionesListaActivity extends AppCompatActivity {

    private RecyclerView rvCanciones;
    private CancionesListaAdapter adapter;
    private List<Cancion> canciones = new ArrayList<>();

    private DatabaseReference dbCanciones;

    private TextView tvTituloLista;
    private ImageButton btnMenu;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    private int listaId; // id_lista de la lista seleccionada

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_canciones_lista);

        // Obtener id_lista y nombre de la lista desde el intent
        listaId = getIntent().getIntExtra("id_lista", -1);
        String nombreLista = getIntent().getStringExtra("nombre_lista");

        // Inicializar vistas
        tvTituloLista = findViewById(R.id.tvTituloCancionesLista);
        rvCanciones = findViewById(R.id.rvCancionesLista);
        btnMenu = findViewById(R.id.btnMenu);
        drawerLayout = findViewById(R.id.drawerLayoutCancionesLista);
        navigationView = findViewById(R.id.navigationViewCancionesLista);

        tvTituloLista.setText(nombreLista);

        // Configurar RecyclerView
        rvCanciones.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CancionesListaAdapter(canciones, new CancionesListaAdapter.Listener() {
            @Override
            public void onPlay(Cancion c) {
                Toast.makeText(CancionesListaActivity.this,
                        "Reproduciendo: " + c.titulo, Toast.LENGTH_SHORT).show();
                // Aquí puedes implementar la reproducción real
            }

            @Override
            public void onDelete(Cancion c, int pos) {
                mostrarDialogoEliminar(c, pos);
            }
        });
        rvCanciones.setAdapter(adapter);

        // Firebase
        dbCanciones = FirebaseDatabase.getInstance().getReference("canciones");
        cargarCancionesDeLista();

        // Botón de menú lateral
        btnMenu.setOnClickListener(v -> {
            if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        // Configurar navegación del drawer
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_busqueda) {
                startActivity(new Intent(CancionesListaActivity.this, MainActivity.class));
            } else if (id == R.id.nav_favoritos) {
                startActivity(new Intent(CancionesListaActivity.this, FavoritosActivity.class));
            } else if (id == R.id.nav_listas) {
                // Si esta Activity fuera la de Listas, cerrar drawer en lugar de abrirla
                drawerLayout.closeDrawer(GravityCompat.START);
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    // Cargar canciones de la lista desde Firebase
    private void cargarCancionesDeLista() {
        dbCanciones.orderByChild("id_lista").equalTo(listaId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        canciones.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Cancion c = ds.getValue(Cancion.class);
                            if (c != null) {
                                c.asegurarCanal(); // Si tienes lógica para asegurarlo
                                canciones.add(c);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(CancionesListaActivity.this,
                                "Error al cargar canciones", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Mostrar diálogo para eliminar canción
    private void mostrarDialogoEliminar(Cancion c, int pos) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar canción")
                .setMessage("¿Quieres eliminar \"" + c.titulo + "\" de esta lista?")
                .setPositiveButton("Eliminar", (dialog, which) -> {

                    dbCanciones.child(c.youtubeId)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    Cancion cancionFirebase = snapshot.getValue(Cancion.class);
                                    if (cancionFirebase != null && cancionFirebase.id_lista == listaId) {
                                        snapshot.getRef().removeValue()
                                                .addOnSuccessListener(a -> {
                                                    canciones.remove(pos);
                                                    adapter.notifyItemRemoved(pos);
                                                    Toast.makeText(CancionesListaActivity.this,
                                                            "Canción eliminada", Toast.LENGTH_SHORT).show();
                                                })
                                                .addOnFailureListener(e ->
                                                        Toast.makeText(CancionesListaActivity.this,
                                                                "Error al eliminar", Toast.LENGTH_SHORT).show()
                                                );
                                    } else {
                                        Toast.makeText(CancionesListaActivity.this,
                                                "La canción no pertenece a esta lista", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Toast.makeText(CancionesListaActivity.this,
                                            "Error al acceder a la base de datos", Toast.LENGTH_SHORT).show();
                                }
                            });

                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}