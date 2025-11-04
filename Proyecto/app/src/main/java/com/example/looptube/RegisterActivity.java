package com.example.looptube;


import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.example.looptube.models.Usuario;

import java.util.List;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    private EditText etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registro);

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            Toast.makeText(this, "Ya estás registrado. Redirigiendo al inicio de sesión...", Toast.LENGTH_LONG).show();
            // Redirigir al login
            startActivity(new Intent(this, LoginActivity.class));
            finish(); // Cerrar RegisterActivity
            return;
        }

        mDatabase = FirebaseDatabase.getInstance().getReference();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();
                        String nombre = email.split("@")[0];

                        Usuario usuario = new Usuario();
                        usuario.nombre = nombre;
                        usuario.email = email;
                        usuario.contraseña_hash = password;
                        usuario.rol = "usuario";

                        // Guardar en Firebase
                        mDatabase.child("usuarios").child(userId).setValue(usuario)
                                .addOnCompleteListener(dbTask -> {
                                    if (dbTask.isSuccessful()) {
                                        Toast.makeText(this, "Usuario Registrado", Toast.LENGTH_LONG).show();

                                        mAuth.signOut();

                                        // Redirigir al login
                                        Intent intent = new Intent(this, LoginActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Toast.makeText(this, "Error al guardar en Firebase: " + dbTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });

                        // Guardar también en SQLite (opcional)
                        com.example.looptube.AppDatabase db = Room.databaseBuilder(
                                        getApplicationContext(),
                                        com.example.looptube.AppDatabase.class,
                                        "looptube_db")
                                .allowMainThreadQueries()
                                .build();
                        Log.d("SQLiteDebug", "Intentando insertar usuario en SQLite...");

                        try {
                            db.dao().insertarUsuario(usuario);
                            Log.d("SQLiteDebug", "Usuario insertado correctamente en SQLite: " + usuario.email);
                        } catch (Exception e) {
                            Log.e("SQLiteDebug", "Error al insertar usuario en SQLite", e);
                        }
                        List<Usuario> usuarios = db.dao().obtenerUsuarios();
                        Log.d("SQLiteDebug", "Usuarios actuales en SQLite: " + usuarios.size());
                        for (Usuario u : usuarios) {
                            Log.d("SQLiteDebug", "→ " + u.id + " | " + u.email + " | " + u.rol);
                        }

                    } else {
                        Toast.makeText(this, "Error al registrar: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}