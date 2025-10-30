package com.example.looptube;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.example.looptube.models.Usuario;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnRegister;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        //comprobacion usuario logeado
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            comprobarRolUsuario(currentUser.getUid());
        }

        btnLogin.setOnClickListener(v -> loginUser());
        btnRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();
                        comprobarRolUsuario(userId);
                    } else {
                        Toast.makeText(this, "Usuario no registrado o contraseña incorrecta", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void comprobarRolUsuario(String userId) {
        DatabaseReference usuarioRef = mDatabase.child("usuarios").child(userId);
        usuarioRef.get().addOnSuccessListener(snapshot -> {
            Usuario usuario = snapshot.getValue(Usuario.class);
            if (usuario != null) {
                if ("admin".equals(usuario.rol)) {
                    startActivity(new Intent(this, AdminActivity.class));
                } else {
                    startActivity(new Intent(this, MainActivity.class));
                }
                finish();
            } else {
                Toast.makeText(this, "Error al obtener los datos del usuario", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error al acceder a la base de datos", Toast.LENGTH_SHORT).show();
        });
    }
}
