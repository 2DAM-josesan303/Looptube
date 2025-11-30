package com.example.looptube;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
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

import com.example.looptube.Adaptadores.PlaylistAdapter;
import com.example.looptube.models.Cancion;
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
    private ImageButton btnMenu;

    private final String INVALID_CHARS = ".#$[]";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listas_reproduccion);

        // 🔹 Firebase "listas"
        ref = FirebaseDatabase.getInstance().getReference("listas");

        rv = findViewById(R.id.rvListasReproduccion);
        rv.setLayoutManager(new LinearLayoutManager(this));

        btnNuevaLista = findViewById(R.id.btnNuevaLista);
        drawerLayout = findViewById(R.id.drawerLayoutListas);
        navigationView = findViewById(R.id.navigationViewListas);
        btnMenu = findViewById(R.id.btnMenu);

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_listas) drawerLayout.closeDrawer(GravityCompat.START);
            else if (id == R.id.nav_busqueda)
                startActivity(new Intent(this, MainActivity.class));
            else if (id == R.id.nav_favoritos)
                startActivity(new Intent(this, FavoritosActivity.class));
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        adapter = new PlaylistAdapter(listas, new PlaylistAdapter.Listener() {
            @Override
            public void onClickLista(String nombre) {
                Intent i = new Intent(ListasReproduccionActivity.this, CancionesListaActivity.class);
                i.putExtra("nombre_lista", nombre);
                startActivity(i);
            }

            @Override
            public void onDelete(String nombre, int pos) {
                mostrarDialogoEliminarLista(nombre);
            }
        });

        rv.setAdapter(adapter);

        // 🔹 Cargar listas y crear lista de ejemplo si no existe
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

                // Crear lista de ejemplo si no existe
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
                    if (nombre.isEmpty()) return;
                    if (listas.contains(nombre)) return;

                    Map<String, Object> nuevaLista = new HashMap<>();
                    ref.child(nombre).setValue(nuevaLista)
                            .addOnSuccessListener(a -> {
                                listas.add(nombre);
                                adapter.notifyItemInserted(listas.size() - 1);

                                Map<String, Object> cancionEjemplo = new HashMap<>();
                                cancionEjemplo.put("titulo", "The Pointer Sisters - Hot Together (Official Audio) - YouTube");
                                cancionEjemplo.put("canal", "The Pointer Sisters");
                                cancionEjemplo.put("url_miniatura", "https://img.youtube.com/vi/H3Aay-47ZT0/hqdefault.jpg");
                                cancionEjemplo.put("youtubeId", "H3Aay-47ZT0");

                                Map<String, Object> listaConCancion = new HashMap<>();
                                listaConCancion.put("H3Aay-47ZT0", cancionEjemplo);

                                DatabaseReference refListasLista = FirebaseDatabase.getInstance()
                                        .getReference("listas").child(nombre);
                                refListasLista.setValue(listaConCancion);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error al crear lista", Toast.LENGTH_SHORT).show();
                            });
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
}