package com.example.looptube;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.looptube.Adaptadores.UsuarioAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.example.looptube.models.Usuario;

import java.util.ArrayList;
import java.util.List;

public class AdminActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    private ListView listViewUsuarios;
    private ImageButton btnAgregarUsuario;
    private Button btnSalir;

    private List<Usuario> UsuariosList = new ArrayList<>();
    private UsuarioAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.administrador);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("usuarios");

        listViewUsuarios = findViewById(R.id.listViewUsuarios);
        btnAgregarUsuario = findViewById(R.id.btnAgregarUsuario);
        btnSalir = findViewById(R.id.btnSalir);

        adapter = new UsuarioAdapter(this, UsuariosList, new UsuarioAdapter.OnUsuarioActionListener() {
            @Override
            public void onEditar(Usuario usuario) {
                Intent intent = new Intent(AdminActivity.this, EditUserActivity.class);
                intent.putExtra("userId", usuario.id);
                startActivity(intent);
            }

            @Override
            public void onEliminar(Usuario usuario) {
                new AlertDialog.Builder(AdminActivity.this)
                        .setTitle("Eliminar usuario")
                        .setMessage("¿Seguro que quieres eliminar al usuario " + usuario.nombre + "?")
                        .setPositiveButton("Sí", (dialog, which) -> {
                            mDatabase.child(String.valueOf(usuario.id)).removeValue()
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(AdminActivity.this, "Usuario eliminado", Toast.LENGTH_SHORT).show();
                                        UsuariosList.remove(usuario);
                                        adapter.notifyDataSetChanged();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(AdminActivity.this, "Error al eliminar: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                    );
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            }
        });

        listViewUsuarios.setAdapter(adapter);

        // Botón agregar usuario
        btnAgregarUsuario.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, RegisterActivity.class);
            intent.putExtra("creandoUsuario", true);
            startActivity(intent);
        });

        // Botón salir
        btnSalir.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        cargarUsuarios();
    }

    private void cargarUsuarios() {
        mDatabase.get().addOnSuccessListener(snapshot -> {
            UsuariosList.clear();
            for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                Usuario usuario = userSnapshot.getValue(Usuario.class);
                if (usuario != null) {
                    UsuariosList.add(usuario);
                }
            }
            adapter.notifyDataSetChanged();
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Error al cargar usuarios: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }
}