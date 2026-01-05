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

import com.bumptech.glide.Glide;
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
    private ImageButton btnMenu, btnPerfil;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    private String uidUsuario;
    private String nombreLista;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_canciones_lista);
        uidUsuario = getIntent().getStringExtra("uid_usuario");
        if (uidUsuario == null) {
            Toast.makeText(this, "No se recibió el UID del usuario", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        nombreLista = getIntent().getStringExtra("nombre_lista");
        if (nombreLista == null) {
            Toast.makeText(this, "No se recibió el nombre de la lista", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvTituloLista = findViewById(R.id.tvTituloCancionesLista);
        rvCanciones = findViewById(R.id.rvCancionesLista);
        btnMenu = findViewById(R.id.btnMenu);
        btnPerfil = findViewById(R.id.btnPerfil);
        drawerLayout = findViewById(R.id.drawerLayoutCancionesLista);
        navigationView = findViewById(R.id.navigationViewCancionesLista);

        tvTituloLista.setText(nombreLista);
        cargarFotoPerfilUsuario();

        rvCanciones.setLayoutManager(new LinearLayoutManager(this));


        // <editor-fold desc="CancionesListaAdapter">
        adapter = new CancionesListaAdapter(canciones, new CancionesListaAdapter.Listener() {
            @Override
            public void onPlay(Cancion c) {
                Intent i = new Intent(CancionesListaActivity.this, MainActivity.class);
                i.putExtra("uid_usuario", uidUsuario);
                i.putExtra("videoId", c.youtubeId);
                i.putExtra("channelName", c.canal);
                i.putExtra("thumbnailUrl", c.url_miniatura);
                i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); /* Reutiliza el MainActivity sin crearla de nuevo*/
                startActivity(i);
            }

            @Override
            public void onDelete(Cancion c, int pos) {
                mostrarDialogoEliminar(c, pos);
            }
        });
        // </editor-fold>

        rvCanciones.setAdapter(adapter);

        dbCanciones = FirebaseDatabase.getInstance()
                .getReference("usuarios")
                .child(uidUsuario)
                .child("listas")
                .child(nombreLista);

        cargarCancionesDeLista();

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // <editor-fold desc="Menu lateral">
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_busqueda) {
                Intent i = new Intent(CancionesListaActivity.this, MainActivity.class);
                i.putExtra("uid_usuario", uidUsuario);
                startActivity(i);
            } else if (id == R.id.nav_favoritos) {
                Intent i = new Intent(CancionesListaActivity.this, FavoritosActivity.class);
                i.putExtra("uid_usuario", uidUsuario);
                startActivity(i);
            } else if (id == R.id.nav_listas) {
                Intent i = new Intent(CancionesListaActivity.this, ListasReproduccionActivity.class);
                i.putExtra("uid_usuario", uidUsuario);
                startActivity(i);
            } else if (id == R.id.nav_logout) {
                Intent logoutIntent = new Intent(CancionesListaActivity.this, LoginActivity.class);
                logoutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(logoutIntent);
                finish();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
        // </editor-fold>
    }

    private void cargarCancionesDeLista() {
        dbCanciones.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                canciones.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Cancion c = ds.getValue(Cancion.class);
                    if (c != null) canciones.add(c);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CancionesListaActivity.this, "Error al cargar canciones", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarDialogoEliminar(Cancion c, int pos) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar canción")
                .setMessage("¿Quieres eliminar \"" + c.titulo + "\" de esta lista?")
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarCancionDeLista(c.youtubeId, pos))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarCancionDeLista(String videoId, int pos) {
        DatabaseReference refCancion = FirebaseDatabase.getInstance()
                .getReference("usuarios")
                .child(uidUsuario)
                .child("listas")
                .child(nombreLista)
                .child(videoId);

        refCancion.removeValue()
                .addOnSuccessListener(a -> {
                    canciones.remove(pos);
                    adapter.notifyItemRemoved(pos);
                    Toast.makeText(this, "Canción eliminada", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error al eliminar", Toast.LENGTH_SHORT).show());
    }

    private void cargarFotoPerfilUsuario() {
        DatabaseReference refFoto = FirebaseDatabase.getInstance()
                .getReference("usuarios")
                .child(uidUsuario)
                .child("fotoPerfil");

        refFoto.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    btnPerfil.setImageResource(R.drawable.circle_background);
                    return;
                }
                String uriString = snapshot.getValue(String.class);
                if (uriString != null && !uriString.equals("default")) {
                    Glide.with(CancionesListaActivity.this)
                            .load(uriString)
                            .circleCrop()
                            .into(btnPerfil);
                } else {
                    btnPerfil.setImageResource(R.drawable.circle_background);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CancionesListaActivity.this, "Error al cargar foto de perfil", Toast.LENGTH_SHORT).show();
            }
        });
    }
}