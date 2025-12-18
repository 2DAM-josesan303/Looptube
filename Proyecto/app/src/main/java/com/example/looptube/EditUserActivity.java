package com.example.looptube;

import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.looptube.models.Usuario;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class EditUserActivity extends AppCompatActivity {

    private EditText etEmail, etPassword, etNombre;
    private Spinner spinnerRol;
    private Button btnGuardar, btnChangePhoto;
    private ImageView ivProfile;

    private DatabaseReference mDatabase;
    private String firebaseId;
    private Usuario usuarioActual;
    private Uri nuevaFotoPerfilUri;

    private ActivityResultLauncher<String> seleccionarImagenLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.editar);

        etEmail = findViewById(R.id.etEmail);
        etNombre = findViewById(R.id.etNombre);
        etPassword = findViewById(R.id.etPassword);
        spinnerRol = findViewById(R.id.spinnerRol);
        btnGuardar = findViewById(R.id.btnGuardarCambios);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);
        ivProfile = findViewById(R.id.ivProfile);

        // 🔹 No modificable
        spinnerRol.setEnabled(false);

        mDatabase = FirebaseDatabase.getInstance().getReference("usuarios");

        firebaseId = getIntent().getStringExtra("firebaseId");
        if (firebaseId == null || firebaseId.isEmpty()) {
            Toast.makeText(this, "Error: usuario no encontrado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Lanzador de selector de imagen
        seleccionarImagenLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        nuevaFotoPerfilUri = uri;
                        // Mostrar la nueva foto en el ImageView
                        Glide.with(EditUserActivity.this)
                                .load(uri)
                                .circleCrop()
                                .into(ivProfile);
                    }
                }
        );

        btnChangePhoto.setOnClickListener(v -> seleccionarImagenLauncher.launch("image/*"));

        cargarDatosUsuario();

        btnGuardar.setOnClickListener(v -> guardarCambios());
    }

    private void cargarDatosUsuario() {
        mDatabase.child(firebaseId).get().addOnSuccessListener(snapshot -> {
            usuarioActual = snapshot.getValue(Usuario.class);
            if (usuarioActual != null) {
                etEmail.setText(usuarioActual.email);
                etNombre.setText(usuarioActual.nombre);
                etPassword.setText(usuarioActual.contraseña_hash);

                // Mostrar foto actual
                if (usuarioActual.fotoPerfil != null && !usuarioActual.fotoPerfil.equals("default")) {
                    Glide.with(this)
                            .load(Uri.parse(usuarioActual.fotoPerfil))
                            .circleCrop()
                            .into(ivProfile);
                } else {
                    ivProfile.setImageResource(R.drawable.circle_background);
                }

                // Mostrar rol (no editable)
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_item,
                        new String[]{usuarioActual.rol});
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerRol.setAdapter(adapter);

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
        String nuevoNombre = etNombre.getText().toString().trim();
        String nuevaPassword = etPassword.getText().toString().trim();

        if (nuevoEmail.isEmpty() || nuevaPassword.isEmpty() || nuevoNombre.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Preparamos los cambios en un Map
        Map<String, Object> cambios = new HashMap<>();
        cambios.put("email", nuevoEmail);
        cambios.put("nombre", nuevoNombre);
        cambios.put("contraseña_hash", nuevaPassword);

        if (nuevaFotoPerfilUri != null) {
            Uri uriInterna = copiarImagenInterna(nuevaFotoPerfilUri);
            if (uriInterna != null) {
                cambios.put("fotoPerfil", uriInterna.toString());
            }
        }

        mDatabase.child(firebaseId).updateChildren(cambios)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Usuario actualizado correctamente", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al actualizar: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private Uri copiarImagenInterna(Uri uriOriginal) {
        try {
            InputStream in = getContentResolver().openInputStream(uriOriginal);
            File archivo = new File(getFilesDir(), "perfil_" + System.currentTimeMillis() + ".jpg");
            OutputStream out = new FileOutputStream(archivo);

            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            in.close();
            out.close();
            return Uri.fromFile(archivo);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}