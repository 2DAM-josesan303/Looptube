package com.example.looptube;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.example.looptube.models.Usuario;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    private EditText etEmail, etPassword;
    private Button btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registro); // Asegúrate de tener este layout

        // Inicializa Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Inicializa vistas
        etEmail = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Completa los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();

                        // Guarda datos en Firebase Database
                        Usuario usuario = new Usuario(userId, email, password, "usuario");
                        mDatabase.child("usuarios").child(userId).setValue(usuario)
                                .addOnCompleteListener(dbTask -> {
                                    if (dbTask.isSuccessful()) {
                                        Toast.makeText(this, "Usuario registrado correctamente", Toast.LENGTH_SHORT).show();
                                        // TODO: redirigir a otra actividad o guardar en SQLite
                                    } else {
                                        Toast.makeText(this, "Error al guardar en DB: " + dbTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(this, "Error al registrar: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}