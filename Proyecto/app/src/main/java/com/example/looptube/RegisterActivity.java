package com.example.looptube;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.example.looptube.models.Usuario;

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

        // Validaciones básicas
        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }

        // Registrar usuario
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();
                        String nombre = email.split("@")[0]; // Generar nombre a partir del email

                        Usuario usuario = new Usuario();
                        usuario.nombre = nombre;
                        usuario.email = email;
                        usuario.contraseña_hash = password;
                        usuario.rol = "usuario";

                        // Guardar en Firebase
                        mDatabase.child("usuarios").child(userId).setValue(usuario)
                                .addOnCompleteListener(dbTask -> {
                                    if (dbTask.isSuccessful()) {
                                        Toast.makeText(this, "Usuario registrado en Firebase", Toast.LENGTH_SHORT).show();
                                        DatabaseReference usuarioRef = mDatabase.child("usuarios").child(userId);
                                        usuarioRef.get().addOnSuccessListener(snapshot -> {
                                            Usuario usuarioGuardado = snapshot.getValue(Usuario.class);
                                            if (usuarioGuardado != null) {
                                                if ("admin".equals(usuarioGuardado.rol)) {
                                                    startActivity(new Intent(this, AdminActivity.class));
                                                } else {
                                                    startActivity(new Intent(this, MainActivity.class));
                                                }
                                                finish(); // cerrar RegisterActivity
                                            }
                                        });
                                    } else {
                                        Toast.makeText(this, "Error al guardar en Firebase: " + dbTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });

                        // Guardar en SQLite
                        com.example.looptube.database.AppDatabase db = Room.databaseBuilder(
                                        getApplicationContext(),
                                        com.example.looptube.database.AppDatabase.class,
                                        "looptube_db")
                                .allowMainThreadQueries()
                                .build();

                        db.dao().insertarUsuario(usuario);

                        //Toast.makeText(this, "Usuario registrado en SQLite", Toast.LENGTH_SHORT).show();

                    } else {
                        Toast.makeText(this, "Error al registrar: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}