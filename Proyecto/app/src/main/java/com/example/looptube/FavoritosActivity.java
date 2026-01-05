package com.example.looptube;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import com.bumptech.glide.Glide;
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
    private ImageButton btnMenu, btnBorrarHistorial, btnPerfil;
    private RecyclerView rvFavoritos, rvHistorial;

    private ArrayList<Cancion> listaFavoritos = new ArrayList<>();
    private ArrayList<Cancion> listaHistorial = new ArrayList<>();
    private FavoritoAdapter favoritoAdapter;
    private HistorialAdapter historialAdapter;

    private DatabaseReference dbFavoritos;
    private DatabaseReference dbCanciones;

    private String uidUsuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.favoritos_historial);

        Intent intent = getIntent();
        uidUsuario = intent.getStringExtra("uid_usuario");
        if (uidUsuario == null) {
            Toast.makeText(this, "No se recibió el UID del usuario", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        btnMenu = findViewById(R.id.btnMenu);
        btnBorrarHistorial = findViewById(R.id.btnBorrarHistorial);
        btnPerfil = findViewById(R.id.btnPerfil);
        rvFavoritos = findViewById(R.id.rvFavoritos);
        rvHistorial = findViewById(R.id.rvHistorial);

        rvFavoritos.setLayoutManager(new LinearLayoutManager(this));
        rvHistorial.setLayoutManager(new LinearLayoutManager(this));

        cargarFotoPerfilUsuario();

        dbFavoritos = FirebaseDatabase.getInstance()
                .getReference("usuarios")
                .child(uidUsuario)
                .child("favoritos");

        dbCanciones = FirebaseDatabase.getInstance()
                .getReference("usuarios")
                .child(uidUsuario)
                .child("historial");

        // <editor-fold desc="Adapter Favoritos">
        favoritoAdapter = new FavoritoAdapter(listaFavoritos, new FavoritoAdapter.Listener() {
            @Override
            public void onPlay(Cancion c) {
                c.asegurarCanal();
                dbCanciones.child(c.youtubeId).setValue(c)
                        .addOnSuccessListener(a -> {
                            Intent i = new Intent(FavoritosActivity.this, MainActivity.class);
                            i.putExtra("uid_usuario", uidUsuario);
                            i.putExtra("videoId", c.youtubeId);
                            i.putExtra("channelName", c.canal);
                            i.putExtra("thumbnailUrl", c.url_miniatura);

                            i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

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
        // </editor-fold>

        // <editor-fold desc="Adapter Historial">
        historialAdapter = new HistorialAdapter(listaHistorial, new HistorialAdapter.Listener() {
            @Override
            public void onPlay(Cancion c) {
                c.asegurarCanal();
                dbCanciones.child(c.youtubeId).setValue(c)
                        .addOnSuccessListener(a -> {
                            Intent i = new Intent(FavoritosActivity.this, MainActivity.class);
                            i.putExtra("uid_usuario", uidUsuario);
                            i.putExtra("videoId", c.youtubeId);
                            i.putExtra("channelName", c.canal);
                            i.putExtra("thumbnailUrl", c.url_miniatura);

                            i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

                            startActivity(i);
                        })
                        .addOnFailureListener(e -> Toast.makeText(FavoritosActivity.this, "Error al reproducir la canción", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onDelete(Cancion c, int position) {
                if (c.key != null) {
                    dbCanciones.child(c.key)
                            .removeValue()
                            .addOnSuccessListener(a -> {
                                listaHistorial.remove(position);
                                historialAdapter.notifyItemRemoved(position);
                                Toast.makeText(FavoritosActivity.this, "Canción eliminada del historial", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> Toast.makeText(FavoritosActivity.this, "Error al eliminar", Toast.LENGTH_SHORT).show());
                } else {
                    Toast.makeText(FavoritosActivity.this, "Error: clave nula", Toast.LENGTH_SHORT).show();
                }
            }
        });
        // </editor-fold>

        rvHistorial.setAdapter(historialAdapter);

        cargarFavoritos();
        cargarHistorial();

        btnBorrarHistorial.setOnClickListener(v -> mostrarDialogoEliminacion(
                "Borrar Historial",
                "¿Seguro que quieres borrar todo el historial?",
                () -> {
                    dbCanciones.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot child : snapshot.getChildren()) {
                                child.getRef().removeValue();
                            }
                            listaHistorial.clear();
                            historialAdapter.notifyDataSetChanged();
                            Toast.makeText(FavoritosActivity.this, "Historial borrado correctamente", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
        ));

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // <editor-fold desc="Menu lateral">
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_favoritos) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (id == R.id.nav_busqueda) {
                Intent i = new Intent(FavoritosActivity.this, MainActivity.class);
                i.putExtra("uid_usuario", uidUsuario);
                startActivity(i);
            } else if (id == R.id.nav_listas) {
                Intent i = new Intent(FavoritosActivity.this, ListasReproduccionActivity.class);
                i.putExtra("uid_usuario", uidUsuario);
                startActivity(i);
            } else if (id == R.id.nav_logout) {
                SharedPreferences prefs = getSharedPreferences("MisPrefs", MODE_PRIVATE);
                prefs.edit().remove("uid_usuario").apply();

                Intent logoutIntent = new Intent(FavoritosActivity.this, LoginActivity.class);
                logoutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(logoutIntent);
                finish();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
        // </editor-fold>
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
        dbCanciones.addListenerForSingleValueEvent(new ValueEventListener() {
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
                () -> dbFavoritos.child(c.key).removeValue()
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

    private void cargarFotoPerfilUsuario() {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("usuarios")
                .child(uidUsuario)
                .child("fotoPerfil");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    btnPerfil.setImageResource(R.drawable.circle_background);
                    return;
                }
                String uriString = snapshot.getValue(String.class);
                if (uriString != null && !uriString.equals("default")) {
                    Glide.with(FavoritosActivity.this)
                            .load(uriString)
                            .circleCrop()
                            .into(btnPerfil);
                } else {
                    btnPerfil.setImageResource(R.drawable.circle_background);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(FavoritosActivity.this, "Error al cargar foto de perfil", Toast.LENGTH_SHORT).show();
            }
        });
    }
}