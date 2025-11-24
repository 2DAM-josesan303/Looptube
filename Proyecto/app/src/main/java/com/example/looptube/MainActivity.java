package com.example.looptube;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.Intent;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import com.example.looptube.models.Cancion;

public class MainActivity extends AppCompatActivity {

    private EditText etUrl;
    private Button btnCargar, btnAñadirCola;
    private WebView webBuscador;
    private TextView tvTitulo;
    private ImageButton btnPrev, btnNext, btnMenu, btnGuardarFavorito;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    private ArrayList<String> cola = new ArrayList<>();
    private int indiceActual = -1;

    // Firebase
    private DatabaseReference dbCanciones;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ------------------------------
        // Inicializar vistas
        // ------------------------------
        etUrl = findViewById(R.id.etUrl);
        btnCargar = findViewById(R.id.btnCargar);
        btnAñadirCola = findViewById(R.id.btnAñadirCola);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        btnMenu = findViewById(R.id.btnMenu);
        btnGuardarFavorito = findViewById(R.id.btnGuardarFavorito);
        tvTitulo = findViewById(R.id.tvTitulo);
        webBuscador = findViewById(R.id.webBuscador);
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);

        // ------------------------------
        // Configurar WebView
        // ------------------------------
        webBuscador.getSettings().setJavaScriptEnabled(true);
        webBuscador.addJavascriptInterface(new WebAppInterface(), "Android");
        webBuscador.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                view.loadUrl("javascript:Android.setTitle(document.title);");
            }
        });
        webBuscador.loadUrl("https://m.youtube.com");

        // ------------------------------
        // Inicializar Firebase
        // ------------------------------
        dbCanciones = FirebaseDatabase.getInstance().getReference("canciones");

        // ------------------------------
        // Cargar la cola desde Firebase
        // ------------------------------
        cargarColaDesdeFirebase();

        // ------------------------------
        // Reproducir video si viene desde FavoritosActivity
        // ------------------------------
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("videoId")) {
            String videoId = intent.getStringExtra("videoId");
            if (videoId != null && !videoId.isEmpty()) {
                reproducirVideo(videoId);
                if (!cola.contains(videoId)) {
                    cola.add(videoId);
                    indiceActual = cola.size() - 1;
                }
            }
        }

        // ------------------------------
        // Botón Reproducir
        // ------------------------------
        btnCargar.setOnClickListener(v -> {
            String url = etUrl.getText().toString().trim();
            String id = extraerYoutubeId(url);
            if (id == null) {
                Toast.makeText(this, "URL no válida", Toast.LENGTH_SHORT).show();
                return;
            }
            reproducirVideo(id);
            if (!cola.contains(id)) {
                cola.add(id);
                indiceActual = cola.size() - 1;
            }
        });

        // ------------------------------
        // Botón Añadir a Cola (Firebase)
        // ------------------------------
        btnAñadirCola.setOnClickListener(v -> {
            String url = etUrl.getText().toString().trim();
            String id = extraerYoutubeId(url);
            if (id == null) {
                Toast.makeText(this, "URL no válida", Toast.LENGTH_SHORT).show();
                return;
            }

            // Añadir a cola local
            cola.add(id);

            // Crear objeto Cancion con datos básicos
            Cancion c = new Cancion(tvTitulo.getText().toString(), id, "Canal Desconocido", "url_miniatura_placeholder");

            // Añadir a Firebase
            dbCanciones.child(id).setValue(c)
                    .addOnSuccessListener(a -> Toast.makeText(MainActivity.this, "Añadido a la cola y Firebase", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Error al añadir a Firebase", Toast.LENGTH_SHORT).show());
        });

        // ------------------------------
        // Botones Anterior/Siguiente
        // ------------------------------
        btnPrev.setOnClickListener(v -> {
            if (indiceActual > 0) {
                indiceActual--;
                reproducirVideo(cola.get(indiceActual));
            } else {
                Toast.makeText(this, "No hay video anterior", Toast.LENGTH_SHORT).show();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (indiceActual < cola.size() - 1) {
                indiceActual++;
                reproducirVideo(cola.get(indiceActual));
            } else {
                Toast.makeText(this, "No hay más videos en la cola", Toast.LENGTH_SHORT).show();
            }
        });

        // ------------------------------
        // Botón del menú lateral
        // ------------------------------
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // ------------------------------
        // Manejar selección del menú
        // ------------------------------
        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_favoritos) {
                startActivity(new Intent(MainActivity.this, FavoritosActivity.class));
                drawerLayout.closeDrawer(GravityCompat.START);
            }
            return true;
        });

        // ------------------------------
        // Botón Guardar Favorito
        // ------------------------------
        btnGuardarFavorito.setOnClickListener(v -> {
            String titulo = tvTitulo.getText().toString();
            String url = webBuscador.getUrl();
            String id = extraerYoutubeId(url);

            if (id == null) {
                Toast.makeText(MainActivity.this, "No se pudo obtener el video", Toast.LENGTH_SHORT).show();
                return;
            }

            Cancion c = new Cancion(titulo, id, "Canal Desconocido", "url_miniatura_placeholder");

            FirebaseDatabase.getInstance().getReference("favoritos")
                    .push()
                    .setValue(c)
                    .addOnSuccessListener(a -> Toast.makeText(MainActivity.this, "Añadido a favoritos", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Error al añadir favorito", Toast.LENGTH_SHORT).show());
        });
    }

    // ------------------------------
    // Cargar cola desde Firebase
    // ------------------------------
    private void cargarColaDesdeFirebase() {
        dbCanciones.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                cola.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    cola.add(ds.getKey()); // Guardamos solo el ID del video
                }
                if (!cola.isEmpty()) {
                    indiceActual = 0;
                    reproducirVideo(cola.get(indiceActual));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void reproducirVideo(String id) {
        webBuscador.loadUrl("https://m.youtube.com/watch?v=" + id);
    }

    private String extraerYoutubeId(String url) {
        try {
            if (url.contains("youtu.be/")) {
                String id = url.substring(url.indexOf("youtu.be/") + 9);
                if (id.contains("?")) id = id.substring(0, id.indexOf("?"));
                return id;
            }
            if (url.contains("watch?v=")) {
                String id = url.substring(url.indexOf("watch?v=") + 8);
                if (id.contains("&")) id = id.substring(0, id.indexOf("&"));
                return id;
            }
            if (url.contains("embed/")) {
                String id = url.substring(url.indexOf("embed/") + 6);
                if (id.contains("?")) id = id.substring(0, id.indexOf("?"));
                return id;
            }
        } catch (Exception ignored) {}
        return null;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (webBuscador.canGoBack()) {
            webBuscador.goBack();
        } else {
            super.onBackPressed();
        }
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void setTitle(String title) {
            runOnUiThread(() -> tvTitulo.setText(title));
        }
    }
}