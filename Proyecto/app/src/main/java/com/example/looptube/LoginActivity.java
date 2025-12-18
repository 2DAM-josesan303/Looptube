package com.example.looptube;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.looptube.models.Usuario;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnRegister;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        mDatabase = FirebaseDatabase.getInstance().getReference("usuarios");

        btnLogin.setOnClickListener(v -> loginUser());
        btnRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );
    }

    private void loginUser() {
        String emailInput = etEmail.getText().toString().trim();
        String passwordInput = etPassword.getText().toString().trim();

        if (emailInput.isEmpty() || passwordInput.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                boolean usuarioEncontrado = false;

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Usuario usuario = ds.getValue(Usuario.class);
                    if (usuario != null && emailInput.equals(usuario.email)) {
                        usuarioEncontrado = true;

                        if (passwordInput.equals(usuario.contraseña_hash)) {
                            // Contraseña correcta, ir a Activity según rol
                            Intent intent;
                            if ("admin".equals(usuario.rol)) {
                                intent = new Intent(LoginActivity.this, AdminActivity.class);
                            } else {
                                intent = new Intent(LoginActivity.this, MainActivity.class);
                            }

                            // Pasamos el uid del usuario al MainActivity
                            intent.putExtra("uid_usuario", ds.getKey()); // ds.getKey() es el id del nodo del usuario en Firebase
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this,
                                    "Contraseña incorrecta", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    }
                }

                if (!usuarioEncontrado) {
                    Toast.makeText(LoginActivity.this,
                            "Usuario no registrado", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(LoginActivity.this,
                        "Error al acceder a la base de datos", Toast.LENGTH_SHORT).show();
            }
        });
    }
}