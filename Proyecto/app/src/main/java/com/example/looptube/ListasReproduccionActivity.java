package com.example.looptube;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.looptube.Adaptadores.PlaylistAdapter;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListasReproduccionActivity extends AppCompatActivity {

    private RecyclerView rv;
    private PlaylistAdapter adapter;
    private final List<String> listas = new ArrayList<>();

    private DatabaseReference ref;
    private ImageButton btnNuevaLista;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton btnMenu, btnPerfil;

    private String uidUsuario;
    private final String INVALID_CHARS = ".#$[]";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listas_reproduccion);

        Intent intent = getIntent();
        uidUsuario = intent.getStringExtra("uid_usuario");
        if (uidUsuario == null) {
            Toast.makeText(this, "No se recibió el UID del usuario", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        rv = findViewById(R.id.rvListasReproduccion);
        rv.setLayoutManager(new LinearLayoutManager(this));

        btnNuevaLista = findViewById(R.id.btnNuevaLista);
        drawerLayout = findViewById(R.id.drawerLayoutListas);
        navigationView = findViewById(R.id.navigationViewListas);
        btnMenu = findViewById(R.id.btnMenu);
        btnPerfil = findViewById(R.id.btnPerfil);

        cargarFotoPerfilUsuario();

        ref = FirebaseDatabase.getInstance()
                .getReference("usuarios")
                .child(uidUsuario)
                .child("listas");

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // <editor-fold desc="Menu lateral">
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_listas) {
                drawerLayout.closeDrawer(GravityCompat.START);

            } else if (id == R.id.nav_busqueda) {
                Intent i = new Intent(ListasReproduccionActivity.this, MainActivity.class);
                i.putExtra("uid_usuario", uidUsuario);
                startActivity(i);

            } else if (id == R.id.nav_favoritos) {
                Intent i = new Intent(ListasReproduccionActivity.this, FavoritosActivity.class);
                i.putExtra("uid_usuario", uidUsuario);
                startActivity(i);
            /* Limpia la pila de activitys y crea una nueva pila inicializando el login */
            } else if (id == R.id.nav_logout) {
                Intent logoutIntent = new Intent(ListasReproduccionActivity.this, LoginActivity.class);
                logoutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(logoutIntent);
                finish();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
        // </editor-fold>

        // <editor-fold desc="ListasAdapter">
        adapter = new PlaylistAdapter(listas, new PlaylistAdapter.Listener() {
            @Override
            public void onClickLista(String nombre) {
                Intent i = new Intent(ListasReproduccionActivity.this, CancionesListaActivity.class);
                i.putExtra("uid_usuario", uidUsuario);
                i.putExtra("nombre_lista", nombre);
                startActivity(i);
            }

            @Override
            public void onDelete(String nombre, int pos) {
                mostrarDialogoEliminarLista(nombre);
            }
        });
        // </editor-fold>

        rv.setAdapter(adapter);

        cargarListasConEjemplo();

        btnNuevaLista.setOnClickListener(v -> mostrarDialogoCrearLista());
    }

    private void cargarListasConEjemplo() {
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listas.clear();
                boolean listaEjemploExiste = false;

                for (DataSnapshot ds : snapshot.getChildren()) {
                    listas.add(ds.getKey());
                    if ("Lista_Ejemplo".equals(ds.getKey())) listaEjemploExiste = true;
                }
                /* Si no hubiese ningun lista creada se añade una por defecto*/
                if (!listaEjemploExiste) {
                    Map<String, Object> cancionEjemplo = new HashMap<>();
                    cancionEjemplo.put("titulo", "The Pointer Sisters - Hot Together (Official Audio) - YouTube");
                    cancionEjemplo.put("canal", "The Pointer Sisters");
                    cancionEjemplo.put("url_miniatura", "https://img.youtube.com/vi/H3Aay-47ZT0/hqdefault.jpg");
                    cancionEjemplo.put("youtubeId", "H3Aay-47ZT0");

                    Map<String, Object> listaEjemplo = new HashMap<>();
                    listaEjemplo.put("H3Aay-47ZT0", cancionEjemplo);

                    ref.child("Lista_Ejemplo").setValue(listaEjemplo)
                            .addOnSuccessListener(a -> {
                                listas.add("Lista_Ejemplo");
                                adapter.notifyItemInserted(listas.size() - 1);
                            });
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ListasReproduccionActivity.this, "Error al cargar listas", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarDialogoCrearLista() {
        EditText input = new EditText(this);
        input.setHint("Nombre de la lista");

        InputFilter filtro = (src, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) {
                if (INVALID_CHARS.contains(String.valueOf(src.charAt(i)))) return "";
            }
            return null;
        };
        input.setFilters(new InputFilter[]{filtro});

        new AlertDialog.Builder(this)
                .setTitle("Nueva lista de reproducción")
                .setMessage("Introduce el nombre de la lista:")
                .setView(input)
                .setPositiveButton("Crear", (dialog, which) -> {
                    String nombre = input.getText().toString().trim();
                    if (nombre.isEmpty() || listas.contains(nombre)) return;

                    Map<String, Object> nuevaLista = new HashMap<>();
                    ref.child(nombre).setValue(nuevaLista)
                            .addOnSuccessListener(a -> {
                                listas.add(nombre);
                                adapter.notifyItemInserted(listas.size() - 1);
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Error al crear lista", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDialogoEliminarLista(String nombre) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar lista")
                .setMessage("¿Seguro que quieres eliminar \"" + nombre + "\"?")
                .setPositiveButton("Eliminar", (dialog, which) ->
                        ref.child(nombre).removeValue()
                                .addOnSuccessListener(a -> {
                                    listas.remove(nombre);
                                    adapter.notifyDataSetChanged();
                                }))
                .setNegativeButton("Cancelar", null)
                .show();
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
                /* Mediante la libreria glide se genera en la cache una version de la imagen seleccionadad en formato de circulo y centrada*/
                if (uriString != null && !uriString.equals("default")) {
                    Glide.with(ListasReproduccionActivity.this)
                            .load(uriString)
                            .circleCrop()
                            .into(btnPerfil);
                } else {
                    btnPerfil.setImageResource(R.drawable.circle_background);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ListasReproduccionActivity.this, "Error al cargar foto de perfil", Toast.LENGTH_SHORT).show();
            }
        });
    }
}