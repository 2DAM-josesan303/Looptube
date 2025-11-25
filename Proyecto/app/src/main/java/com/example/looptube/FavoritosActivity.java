package com.example.looptube;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.looptube.Adaptadores.FavoritoAdapter;
import com.example.looptube.Adaptadores.HistorialAdapter;
import com.example.looptube.models.Cancion;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class FavoritosActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton btnMenu, btnBorrarHistorial;
    private RecyclerView rvFavoritos, rvHistorial;

    private ArrayList<Cancion> listaFavoritos = new ArrayList<>();
    private ArrayList<Cancion> listaHistorial = new ArrayList<>();
    private FavoritoAdapter favoritoAdapter;
    private HistorialAdapter historialAdapter;

    private DatabaseReference dbFavoritos;
    private DatabaseReference dbCanciones;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.favoritos_historial);

        // Inicializar vistas
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        btnMenu = findViewById(R.id.btnMenu);
        btnBorrarHistorial = findViewById(R.id.btnBorrarHistorial);
        rvFavoritos = findViewById(R.id.rvFavoritos);
        rvHistorial = findViewById(R.id.rvHistorial);

        rvFavoritos.setLayoutManager(new LinearLayoutManager(this));
        rvHistorial.setLayoutManager(new LinearLayoutManager(this));

        dbFavoritos = FirebaseDatabase.getInstance().getReference("favoritos");
        dbCanciones = FirebaseDatabase.getInstance().getReference("canciones");

        // -----------------------------
        // Adapter Favoritos
        // -----------------------------
        favoritoAdapter = new FavoritoAdapter(listaFavoritos, new FavoritoAdapter.Listener() {
            @Override
            public void onPlay(Cancion c) {
                c.asegurarCanal();
                dbCanciones.child(c.youtubeId).setValue(c)
                        .addOnSuccessListener(a -> {
                            Intent i = new Intent(FavoritosActivity.this, MainActivity.class);
                            i.putExtra("videoId", c.youtubeId);
                            i.putExtra("channelName", c.canal);
                            i.putExtra("thumbnailUrl", c.url_miniatura);
                            startActivity(i);
                        })
                        .addOnFailureListener(e -> Toast.makeText(FavoritosActivity.this, "Error al añadir a la cola", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onDelete(Cancion c, int pos) {
                eliminarFavorito(c);
            }
        });
        rvFavoritos.setAdapter(favoritoAdapter);

        // -----------------------------
        // Adapter Historial
        // -----------------------------
        historialAdapter = new HistorialAdapter(listaHistorial, c -> {
            c.asegurarCanal();
            dbCanciones.child(c.youtubeId).setValue(c)
                    .addOnSuccessListener(a -> {
                        Intent i = new Intent(FavoritosActivity.this, MainActivity.class);
                        i.putExtra("videoId", c.youtubeId);
                        i.putExtra("channelName", c.canal);
                        i.putExtra("thumbnailUrl", c.url_miniatura);
                        startActivity(i);
                    })
                    .addOnFailureListener(e -> Toast.makeText(FavoritosActivity.this, "Error al reproducir la canción", Toast.LENGTH_SHORT).show());
        });
        rvHistorial.setAdapter(historialAdapter);

        cargarFavoritos();
        cargarHistorial();

        // -----------------------------
        // Botón borrar cola
        // -----------------------------
        btnBorrarHistorial.setOnClickListener(v -> mostrarDialogoEliminacion(
                "Borrar Historial",
                "¿Seguro que quieres borrar todo el historial?",
                () -> {
                    dbCanciones.removeValue()
                            .addOnSuccessListener(a -> {
                                listaHistorial.clear();
                                historialAdapter.notifyDataSetChanged();
                                Toast.makeText(FavoritosActivity.this,
                                        "Historial borrado correctamente", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> Toast.makeText(FavoritosActivity.this,
                                    "Error al borrar la historial: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
        ));

        // -----------------------------
        // Botón menú lateral
        // -----------------------------
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_favoritos) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (item.getItemId() == R.id.nav_busqueda) {
                startActivity(new Intent(FavoritosActivity.this, MainActivity.class));
            }
            return true;
        });
    }

    private void cargarFavoritos() {
        dbFavoritos.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaFavoritos.clear();
                for (DataSnapshot d : snapshot.getChildren()) {
                    Cancion c = d.getValue(Cancion.class);
                    if (c != null) {
                        c.key = d.getKey();
                        listaFavoritos.add(c);
                    }
                }
                favoritoAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void cargarHistorial() {
        dbCanciones.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaHistorial.clear();
                for (DataSnapshot d : snapshot.getChildren()) {
                    Cancion c = d.getValue(Cancion.class);
                    if (c != null) {
                        c.key = d.getKey();
                        listaHistorial.add(c);
                    }
                }
                historialAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void eliminarFavorito(Cancion c) {
        if (c.key == null) {
            Toast.makeText(this, "Error: clave nula", Toast.LENGTH_SHORT).show();
            return;
        }

        mostrarDialogoEliminacion(
                "Eliminar favorito",
                "¿Seguro que quieres eliminar " + c.titulo + " de tus favoritos?",
                () -> dbFavoritos.child(c.key)
                        .removeValue()
                        .addOnSuccessListener(a -> {
                            Toast.makeText(FavoritosActivity.this, "Eliminado de favoritos", Toast.LENGTH_SHORT).show();
                            cargarFavoritos();
                        })
                        .addOnFailureListener(e -> Toast.makeText(FavoritosActivity.this, "Error al eliminar", Toast.LENGTH_SHORT).show())
        );
    }

    private void mostrarDialogoEliminacion(String titulo, String mensaje, Runnable accionEliminar) {
        new androidx.appcompat.app.AlertDialog.Builder(FavoritosActivity.this)
                .setTitle(titulo)
                .setMessage(mensaje)
                .setPositiveButton("Sí", (dialog, which) -> accionEliminar.run())
                .setNegativeButton("Cancelar", null)
                .show();
    }
}