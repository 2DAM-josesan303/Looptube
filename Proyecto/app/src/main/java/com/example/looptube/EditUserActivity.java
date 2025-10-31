package com.example.looptube;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.looptube.models.Usuario;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class EditUserActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Spinner spinnerRol;
    private Button btnGuardar;
    private DatabaseReference mDatabase;
    private int userId;
    private Usuario usuarioActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.editar);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        spinnerRol = findViewById(R.id.spinnerRol);
        btnGuardar = findViewById(R.id.btnGuardarCambios);

        mDatabase = FirebaseDatabase.getInstance().getReference("usuarios");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"admin", "usuario"}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRol.setAdapter(adapter);

        userId = getIntent().getIntExtra("userId", -1);
        if (userId == -1) {
            Toast.makeText(this, "Error: usuario no encontrado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        cargarDatosUsuario();

        btnGuardar.setOnClickListener(v -> guardarCambios());
    }

    private void cargarDatosUsuario() {
        mDatabase.child(String.valueOf(userId)).get().addOnSuccessListener(snapshot -> {
            usuarioActual = snapshot.getValue(Usuario.class);
            if (usuarioActual != null) {
                etEmail.setText(usuarioActual.email);
                etPassword.setText(usuarioActual.contraseña_hash);

                if (usuarioActual.rol != null) {
                    ArrayAdapter adapter = (ArrayAdapter) spinnerRol.getAdapter();
                    int pos = adapter.getPosition(usuarioActual.rol);
                    if (pos >= 0) spinnerRol.setSelection(pos);
                }
            } else {
                Toast.makeText(this, "No se encontraron datos del usuario", Toast.LENGTH_SHORT).show();
                finish();
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Error al cargar usuario: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    private void guardarCambios() {
        if (usuarioActual == null) return;

        String nuevoEmail = etEmail.getText().toString().trim();
        String nuevaPassword = etPassword.getText().toString().trim();
        String nuevoRol = spinnerRol.getSelectedItem().toString();

        if (nuevoEmail.isEmpty() || nuevaPassword.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        usuarioActual.email = nuevoEmail;
        usuarioActual.contraseña_hash = nuevaPassword;
        usuarioActual.rol = nuevoRol;

        mDatabase.child(String.valueOf(userId)).setValue(usuarioActual)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Usuario actualizado correctamente", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al actualizar: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}