package com.example.looptube;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.Intent;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.looptube.models.Cancion;
import com.example.looptube.models.Lista;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private EditText etUrl;
    private Button btnCargar, btnAñadirCola;
    private WebView webBuscador;
    private TextView tvTitulo;
    private ImageButton btnPrev, btnNext, btnMenu, btnGuardarFavorito;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    private Cancion cancionActual;

    private ArrayList<String> cola = new ArrayList<>();
    private int indiceActual = -1;

    private String currentChannel = "Canal desconocido";
    private String currentThumbnail = "url_miniatura_placeholder";

    private DatabaseReference dbCanciones;
    private DatabaseReference dbListas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etUrl = findViewById(R.id.etUrl);
        btnCargar = findViewById(R.id.btnCargar);
        btnAñadirCola = findViewById(R.id.btnAñadirCola);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        btnMenu = findViewById(R.id.btnMenu);
        btnGuardarFavorito = findViewById(R.id.btnGuardarFavorito);
        ImageButton btnAñadirALista = findViewById(R.id.btnAñadirALista);
        btnAñadirALista.setOnClickListener(v -> mostrarDialogoAñadirALista());
        tvTitulo = findViewById(R.id.tvTitulo);
        webBuscador = findViewById(R.id.webBuscador);
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);

        webBuscador.getSettings().setJavaScriptEnabled(true);
        webBuscador.addJavascriptInterface(new WebAppInterface(), "Android");
        webBuscador.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                view.loadUrl("javascript:Android.setTitle(document.title)");
                view.loadUrl("javascript:(function(){ " +
                        "var channel = document.querySelector('#owner-name a, .ytd-channel-name a');" +
                        "Android.setChannel(channel ? channel.innerText : 'Canal desconocido');" +
                        "})();");
                view.loadUrl("javascript:(function(){ " +
                        "var vid = new URL(window.location.href).searchParams.get('v');" +
                        "if (vid) Android.setThumbnail('https://img.youtube.com/vi/' + vid + '/hqdefault.jpg');" +
                        "})();");
                // Intento de reproducir automáticamente con sonido
                view.evaluateJavascript(
                        "var video = document.querySelector('video');" +
                                "if (video) { video.muted = false; video.play().catch(e => console.log('Autoplay fallido:', e)); }",
                        null
                );
            }
        });

        webBuscador.loadUrl("https://m.youtube.com");

        dbCanciones = FirebaseDatabase.getInstance().getReference("canciones");
        dbListas = FirebaseDatabase.getInstance().getReference("listas");

        cargarColaDesdeFirebase();

        // Traído desde FavoritosActivity
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("videoId")) {
            String videoId = intent.getStringExtra("videoId");
            if (videoId != null && !videoId.isEmpty()) {
                reproducirVideo(videoId);

                currentChannel = intent.hasExtra("channelName")
                        ? intent.getStringExtra("channelName")
                        : "Canal desconocido";

                currentThumbnail = intent.hasExtra("thumbnailUrl")
                        ? intent.getStringExtra("thumbnailUrl")
                        : "url_miniatura_placeholder";

                cancionActual = new Cancion(
                        tvTitulo.getText().toString(),
                        videoId,
                        currentChannel,
                        currentThumbnail
                );

                if (!cola.contains(videoId)) {
                    cola.add(videoId);
                    indiceActual = cola.size() - 1;
                } else {
                    cola.remove(videoId);
                    cola.add(videoId);
                    indiceActual = cola.size() - 1;
                }
            }
        }

        btnCargar.setOnClickListener(v -> {
            String url = etUrl.getText().toString().trim();
            String id = extraerYoutubeId(url);
            if (id == null) {
                Toast.makeText(this, "URL no válida", Toast.LENGTH_SHORT).show();
                return;
            }
            reproducirVideo(id);

            cancionActual = new Cancion(
                    tvTitulo.getText().toString(),
                    id,
                    currentChannel,
                    currentThumbnail
            );

            if (!cola.contains(id)) {
                cola.add(id);
                indiceActual = cola.size() - 1;
            }
        });

        btnAñadirCola = findViewById(R.id.btnAñadirCola);

        btnAñadirCola.setOnClickListener(v -> {
            String url = webBuscador.getUrl();
            String videoId = extraerYoutubeId(url);

            if (videoId == null) {
                Toast.makeText(MainActivity.this, "No se pudo obtener el video", Toast.LENGTH_SHORT).show();
                return;
            }

            Cancion c = new Cancion(
                    tvTitulo.getText().toString(),
                    videoId,
                    currentChannel,
                    currentThumbnail
            );

            c.asegurarCanal();

            agregarACola(c);
        });
        btnPrev.setOnClickListener(v -> {
            if (indiceActual > 0) {
                indiceActual--;
                reproducirVideo(cola.get(indiceActual));
            } else Toast.makeText(this, "No hay video anterior", Toast.LENGTH_SHORT).show();
        });

        btnNext.setOnClickListener(v -> {
            if (indiceActual < cola.size() - 1) {
                indiceActual++;
                reproducirVideo(cola.get(indiceActual));
            } else Toast.makeText(this, "No hay más videos en la cola", Toast.LENGTH_SHORT).show();
        });

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_favoritos) {
                startActivity(new Intent(MainActivity.this, FavoritosActivity.class));
            }
            if (item.getItemId() == R.id.nav_listas) {
                startActivity(new Intent(MainActivity.this, ListasReproduccionActivity.class));
            }
            return true;
        });

        btnGuardarFavorito.setOnClickListener(v -> {
            String url = webBuscador.getUrl();
            String id = extraerYoutubeId(url);
            if (id == null) {
                Toast.makeText(this, "No se pudo obtener el video", Toast.LENGTH_SHORT).show();
                return;
            }

            Cancion c = new Cancion(tvTitulo.getText().toString(), id, currentChannel, currentThumbnail);
            c.asegurarCanal();

            dbCanciones.push().setValue(c)
                    .addOnSuccessListener(a -> Toast.makeText(this, "Añadido a favoritos", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Error al añadir favorito", Toast.LENGTH_SHORT).show());
        });
    }

    private void agregarACola(Cancion cancion) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("canciones");

        String id = ref.push().getKey();

        HashMap<String, Object> data = new HashMap<>();
        data.put("youtubeId", cancion.youtubeId);
        data.put("titulo", cancion.titulo);
        data.put("canal", cancion.canal);
        data.put("url_miniatura", cancion.url_miniatura);
        data.put("id_video", 0);
        data.put("id_lista", 0);

        ref.child(id).setValue(data)
                .addOnSuccessListener(a -> {
                    Toast.makeText(MainActivity.this, "Añadido a la cola", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Error al añadir", Toast.LENGTH_SHORT).show();
                });
    }

    private void mostrarDialogoAñadirALista() {
        String videoId = extraerYoutubeId(webBuscador.getUrl());
        if (videoId == null) {
            Toast.makeText(this, "No se pudo obtener el video", Toast.LENGTH_SHORT).show();
            return;
        }

        dbListas.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<String> nombresListas = new ArrayList<>();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    if (ds.exists()) {
                        nombresListas.add(ds.getKey());
                    }
                }

                nombresListas.add("➕ Crear nueva lista");

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Añadir a lista de reproducción");

                ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this,
                        android.R.layout.simple_list_item_1, nombresListas);

                builder.setAdapter(adapter, (dialog, which) -> {
                    String seleccion = nombresListas.get(which);
                    if (seleccion.equals("➕ Crear nueva lista")) {
                        mostrarDialogoCrearLista(videoId);
                    } else {
                        añadirCancionALista(videoId, seleccion);
                    }
                });

                builder.show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Error al cargar listas", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarDialogoCrearLista(String videoId) {
        EditText input = new EditText(this);
        input.setHint("Nombre de la lista");

        new AlertDialog.Builder(this)
                .setTitle("Nueva lista de reproducción")
                .setView(input)
                .setPositiveButton("Crear", (dialog, which) -> {
                    String nombreLista = input.getText().toString().trim();
                    if (nombreLista.isEmpty()) {
                        Toast.makeText(this, "Nombre no válido", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Crear la referencia a la nueva lista en "listas"
                    DatabaseReference listaRef = dbListas.child(nombreLista);

                    // Crear el objeto Cancion actual
                    Cancion c = new Cancion(tvTitulo.getText().toString(), videoId, currentChannel, currentThumbnail);

                    // Añadir la canción directamente al nuevo nodo
                    Map<String, Object> cancionMap = new HashMap<>();
                    cancionMap.put("titulo", c.titulo);
                    cancionMap.put("canal", c.canal);
                    cancionMap.put("url_miniatura", c.url_miniatura);
                    cancionMap.put("youtubeId", c.youtubeId);

                    Map<String, Object> nuevaLista = new HashMap<>();
                    nuevaLista.put(videoId, cancionMap);

                    listaRef.setValue(nuevaLista)
                            .addOnSuccessListener(a -> Toast.makeText(this, "Lista creada y canción añadida", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Error al crear lista", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void añadirCancionALista(String videoId, String nombreLista) {
        Cancion c = new Cancion(tvTitulo.getText().toString(), videoId, currentChannel, currentThumbnail);

        DatabaseReference refCancion = dbListas.child(nombreLista).child(videoId);

        refCancion.setValue(c)
                .addOnSuccessListener(a -> Toast.makeText(this, "Añadido a la lista", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Error al añadir a la lista", Toast.LENGTH_SHORT).show());
    }

    // =====================================================
    //   FUNCIONES YA EXISTENTES
    // =====================================================
    private void cargarColaDesdeFirebase() {
        dbCanciones.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                cola.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String videoId = ds.child("youtubeId").getValue(String.class);
                    if (videoId != null) cola.add(videoId);
                }

                // Si hay canciones y no hay una reproduciéndose, reproducir la primera
                if (!cola.isEmpty() && indiceActual == -1) {
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
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.closeDrawer(GravityCompat.START);
        else if (webBuscador.canGoBack()) webBuscador.goBack();
        else super.onBackPressed();
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void setTitle(String title) {
            runOnUiThread(() -> {
                tvTitulo.setText(title);
                if (cancionActual != null) {
                    cancionActual.titulo = title;
                }

                if ("Canal desconocido".equals(currentChannel)) {
                    int primerGuion = title.indexOf(" - ");
                    if (primerGuion != -1) currentChannel = title.substring(0, primerGuion).trim();
                }
            });
        }

        @JavascriptInterface
        public void setChannel(String canal) {
            runOnUiThread(() -> currentChannel = canal);
            if (cancionActual != null) {
                cancionActual.canal = canal;
            }
        }

        @JavascriptInterface
        public void setThumbnail(String url) {
            runOnUiThread(() -> currentThumbnail = url);
            if (cancionActual != null) {
                cancionActual.url_miniatura = url;
            }
        }
    }
}