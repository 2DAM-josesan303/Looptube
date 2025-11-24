package com.example.looptube;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.looptube.Adaptadores.FavoritoAdapter;
import com.example.looptube.models.Cancion;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class FavoritosActivity extends AppCompatActivity {

    private RecyclerView rvFavoritos, rvHistorial;
    private ImageButton btnBorrarHistorial;

    private FavoritoAdapter favoritoAdapter;
    private ArrayList<Cancion> listaFavoritos = new ArrayList<>();

    private DatabaseReference dbFavoritos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.favoritos_historial);

        rvFavoritos = findViewById(R.id.rvFavoritos);
        rvHistorial = findViewById(R.id.rvHistorial);
        btnBorrarHistorial = findViewById(R.id.btnBorrarHistorial);

        rvFavoritos.setLayoutManager(new LinearLayoutManager(this));
        rvHistorial.setLayoutManager(new LinearLayoutManager(this));

        dbFavoritos = FirebaseDatabase.getInstance().getReference("favoritos");

        // ================================
        //      ADAPTADOR FAVORITOS
        // ================================
        favoritoAdapter = new FavoritoAdapter(listaFavoritos, new FavoritoAdapter.Listener() {
            @Override
            public void onPlay(Cancion c) {
                // Enviar ID a main
                Intent i = new Intent(FavoritosActivity.this, MainActivity.class);
                i.putExtra("videoId", c.youtubeId);
                startActivity(i);
            }

            @Override
            public void onDelete(Cancion c, int pos) {
                eliminarFavorito(c, pos);
            }
        });

        rvFavoritos.setAdapter(favoritoAdapter);

        cargarFavoritos();

        btnBorrarHistorial.setOnClickListener(v -> {
            FirebaseDatabase.getInstance().getReference("historial")
                    .removeValue()
                    .addOnSuccessListener(a -> Toast.makeText(this, "Historial borrado", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Error al borrar historial", Toast.LENGTH_SHORT).show());
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
                        c.key = d.getKey(); // Guardamos la clave para eliminar
                        listaFavoritos.add(c);
                    }
                }

                favoritoAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void eliminarFavorito(Cancion c, int pos) {
        if (c.key == null) {
            Toast.makeText(this, "Error: clave nula", Toast.LENGTH_SHORT).show();
            return;
        }

        dbFavoritos.child(c.key)
                .removeValue()
                .addOnSuccessListener(a -> {
                    listaFavoritos.remove(pos);
                    favoritoAdapter.notifyItemRemoved(pos);
                    Toast.makeText(FavoritosActivity.this, "Eliminado", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(FavoritosActivity.this, "Error al eliminar", Toast.LENGTH_SHORT).show()
                );
    }
}