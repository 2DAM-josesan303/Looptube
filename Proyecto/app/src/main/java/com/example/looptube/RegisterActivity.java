package com.example.looptube;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.example.looptube.models.Usuario;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    private EditText etEmail, etPassword, etConfirmPassword;
    private Button btnRegister, btnChangePhoto;

    private Uri fotoSeleccionadaUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registro);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);

        // <editor-fold desc="Metodo para abrir la galeria">
        ActivityResultLauncher<String> seleccionarImagenLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.GetContent(),
                        uri -> {
                            if (uri != null) {
                                fotoSeleccionadaUri = uri;
                                Toast.makeText(this, "Imagen seleccionada", Toast.LENGTH_SHORT).show();
                            }
                        }
                );
        btnChangePhoto.setOnClickListener(v ->
                seleccionarImagenLauncher.launch("image/*")
        );
        // </editor-fold>

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
                .addOnSuccessListener(authResult -> {

                    String userId = authResult.getUser().getUid();

                    Usuario usuario = new Usuario();
                    usuario.nombre = email.split("@")[0];
                    usuario.email = email;
                    usuario.contraseña_hash = password;
                    usuario.rol = "usuario";
                    /* Si hay una foto seleccionada guardamos la Uri en el objeto de Usuario*/
                    if (fotoSeleccionadaUri != null) {
                        Uri uriInterna = copiarImagenInterna(fotoSeleccionadaUri);
                        usuario.fotoPerfil = uriInterna != null
                                ? uriInterna.toString()
                                : "default";
                    } else {
                        usuario.fotoPerfil = "default";
                    }

                    guardarUsuarioEnFirebase(userId, usuario);
                    guardarUsuarioEnSQLite(usuario);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    /* Almacena la imagen en el almacenamiento interno a traves de la uri de la galeria (content: "URI") para posteriormente mediante un buffer copiar la imagen byte por byte y cambiarle el formato a file:///data/data/com.example.looptube/files/perfil_1767621425026.jpg */
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

    private void guardarUsuarioEnFirebase(String userId, Usuario usuario) {
        mDatabase.child("usuarios").child(userId).setValue(usuario)
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "Usuario registrado", Toast.LENGTH_LONG).show();
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                });
    }

    private void guardarUsuarioEnSQLite(Usuario usuario) {
        try {
            AppDatabase db = Room.databaseBuilder(
                            getApplicationContext(),
                            AppDatabase.class,
                            "looptube_db")
                    .allowMainThreadQueries()
                    .build();

            db.dao().insertarUsuario(usuario);

            List<Usuario> usuarios = db.dao().obtenerUsuarios();
            Log.d("SQLiteDebug", "Usuarios SQLite: " + usuarios.size());
        } catch (Exception e) {
            Log.e("SQLiteDebug", "Error SQLite", e);
        }
    }
}