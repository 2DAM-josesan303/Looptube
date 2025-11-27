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
import com.example.looptube.models.Lista;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class ListasReproduccionActivity extends AppCompatActivity {

    private RecyclerView rv;
    private PlaylistAdapter adapter;
    private final List<String> listas = new ArrayList<>();

    private DatabaseReference ref;
    private ImageButton btnNuevaLista;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton btnMenu;

    //Caracteres no permitidos por Firebase
    private final String INVALID_CHARS = ".#$[]";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listas_reproduccion);

        ref = FirebaseDatabase.getInstance().getReference("listas_reproduccion");

        rv = findViewById(R.id.rvListasReproduccion);
        rv.setLayoutManager(new LinearLayoutManager(this));

        btnNuevaLista = findViewById(R.id.btnNuevaLista);

        drawerLayout = findViewById(R.id.drawerLayoutListas);
        navigationView = findViewById(R.id.navigationViewListas);
        btnMenu = findViewById(R.id.btnMenu);

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_listas) {
                // Ya estamos en Listas → solo cerrar drawer
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (id == R.id.nav_busqueda) {
                startActivity(new Intent(ListasReproduccionActivity.this, MainActivity.class));
            } else if (id == R.id.nav_favoritos) {
                startActivity(new Intent(ListasReproduccionActivity.this, FavoritosActivity.class));
            }

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
        cargarListas();

        btnNuevaLista.setOnClickListener(v -> mostrarDialogoCrearLista());
    }

    // 📌 Diálogo para crear una nueva lista
    private void mostrarDialogoCrearLista() {
        EditText input = new EditText(this);
        input.setHint("Nombre de la lista");

        // ❗ Filtro para impedir caracteres prohibidos
        InputFilter filtro = new InputFilter() {
            public CharSequence filter(CharSequence src, int start, int end, Spanned dest, int dstart, int dend) {
                for (int i = start; i < end; i++) {
                    if (INVALID_CHARS.contains(String.valueOf(src.charAt(i)))) {
                        return "";
                    }
                }
                return null;
            }
        };
        input.setFilters(new InputFilter[]{filtro});

        new AlertDialog.Builder(this)
                .setTitle("Nueva lista de reproducción")
                .setMessage("Introduce el nombre de la lista:")
                .setView(input)
                .setPositiveButton("Crear", (dialog, which) -> {
                    String nombre = input.getText().toString().trim();

                    if (nombre.isEmpty()) {
                        Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // ❌ Evitar duplicados
                    if (listas.contains(nombre)) {
                        Toast.makeText(this, "La lista ya existe", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Creamos objeto Lista
                    Lista nuevaLista = new Lista("uid1", nombre);

                    ref.child(nombre).setValue(nuevaLista)
                            .addOnSuccessListener(a -> {
                                Toast.makeText(this, "Lista creada", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error al crear lista", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // 🗑 Confirmación antes de eliminar lista
    private void mostrarDialogoEliminarLista(String nombre) {

        new AlertDialog.Builder(this)
                .setTitle("Eliminar lista")
                .setMessage("¿Seguro que quieres eliminar la lista \"" + nombre + "\"?")
                .setPositiveButton("Eliminar", (dialog, which) -> {

                    ref.child(nombre).removeValue()
                            .addOnSuccessListener(a -> {
                                Toast.makeText(this, "Lista eliminada", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error al eliminar la lista", Toast.LENGTH_SHORT).show();
                            });

                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // 🔄 Cargar listas desde Firebase
    private void cargarListas() {
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listas.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Lista lista = ds.getValue(Lista.class);
                    if (lista != null) listas.add(lista.nombre);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}