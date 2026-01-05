package com.example.looptube;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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

import com.bumptech.glide.Glide;
import com.example.looptube.models.Cancion;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private EditText etUrl;
    private Button btnCargar, btnAñadirCola;
    private ImageButton btnGuardarFavorito, btnPerfil, btnPrev, btnNext, btnMenu, btnAñadirALista;
    private WebView webBuscador;
    private TextView tvTitulo;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    private Cancion cancionActual;
    private ArrayList<String> cola = new ArrayList<>();
    private int indiceActual = -1;

    private String currentChannel = "Canal desconocido";
    private String currentThumbnail = "url_miniatura_placeholder";

    private String uidUsuario;
    private DatabaseReference dbCanciones;
    private DatabaseReference dbListas;
    private DatabaseReference dbFavoritos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inicializarVistas();

        uidUsuario = getIntent().getStringExtra("uid_usuario");
        if (uidUsuario == null) {
            Toast.makeText(this, "No se recibió el UID del usuario", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        inicializarFirebase();
        configurarWebView();
        cargarColaDesdeFirebase();
        cargarFotoPerfilUsuario();
        configurarBotones();
        configurarDrawer();

        manejarIntentVideo(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String nuevoUid = intent.getStringExtra("uid_usuario");
        if (nuevoUid != null) uidUsuario = nuevoUid;
        inicializarFirebase();
        manejarIntentVideo(intent);
    }

    private void manejarIntentVideo(Intent intent) {
        if (intent == null) return;

        uidUsuario = intent.getStringExtra("uid_usuario");
        if (uidUsuario == null) return;

        String videoId = intent.getStringExtra("videoId");
        String channelName = intent.getStringExtra("channelName");
        String thumbnailUrl = intent.getStringExtra("thumbnailUrl");

        if (videoId != null && !videoId.isEmpty()) {
            reproducirVideo(videoId);
            currentChannel = (channelName != null) ? channelName : "Canal desconocido";
            currentThumbnail = (thumbnailUrl != null) ? thumbnailUrl : "url_miniatura_placeholder";

            cancionActual = new Cancion(tvTitulo.getText().toString(), videoId, currentChannel, currentThumbnail);

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

    private void inicializarVistas() {
        etUrl = findViewById(R.id.etUrl);
        btnCargar = findViewById(R.id.btnCargar);
        btnAñadirCola = findViewById(R.id.btnAñadirCola);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        btnMenu = findViewById(R.id.btnMenu);
        btnGuardarFavorito = findViewById(R.id.btnGuardarFavorito);
        btnPerfil = findViewById(R.id.btnPerfil);
        btnAñadirALista = findViewById(R.id.btnAñadirALista);
        webBuscador = findViewById(R.id.webBuscador);
        tvTitulo = findViewById(R.id.tvTitulo);
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
    }

    private void inicializarFirebase() {
        dbCanciones = FirebaseDatabase.getInstance().getReference("usuarios").child(uidUsuario).child("historial");
        dbListas = FirebaseDatabase.getInstance().getReference("usuarios").child(uidUsuario).child("listas");
        dbFavoritos = FirebaseDatabase.getInstance().getReference("usuarios").child(uidUsuario).child("favoritos");
    }

    /* Mediante el uso de WebAppInterface se inyecciona codigo javascript para conseguir titulo del video, Nombre del canal y Miniatura del video para posteriormente cargar la URL de youtube al WebView*/
    private void configurarWebView() {
        webBuscador.getSettings().setJavaScriptEnabled(true);
        webBuscador.addJavascriptInterface(new WebAppInterface(), "Android");
        webBuscador.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (view == null) return;

                /* Nombre del Video*/
                view.loadUrl("javascript:Android.setTitle(document.title)");
                /* Nombre del canal*/
                view.loadUrl("javascript:(function(){ " +
                        "var channel = document.querySelector('#owner-name a, .ytd-channel-name a');" +
                        "Android.setChannel(channel ? channel.innerText : 'Canal desconocido');" +
                        "})();");
                /* Miniatura del Video*/
                view.loadUrl("javascript:(function(){ " +
                        "var vid = new URL(window.location.href).searchParams.get('v');" +
                        "if (vid) {" +
                        "  Android.setThumbnail('https://img.youtube.com/vi/' + vid + '/hqdefault.jpg');" +
                        "  Android.onNewVideoDetected(vid);" +
                        "}" +
                        "})();");
            }
        });
        webBuscador.loadUrl("https://m.youtube.com");
    }

    private void configurarBotones() {
        btnCargar.setOnClickListener(v -> {
            String url = etUrl.getText().toString().trim();
            String id = extraerYoutubeId(url);
            if (id == null) {
                Toast.makeText(this, "URL no válida", Toast.LENGTH_SHORT).show();
                return;
            }
            reproducirVideo(id);
            cancionActual = new Cancion(tvTitulo.getText().toString(), id, currentChannel, currentThumbnail);
            if (!cola.contains(id)) {
                cola.add(id);
                indiceActual = cola.size() - 1;
            }
        });

        btnAñadirCola.setOnClickListener(v -> {
            if (webBuscador == null) return;
            String videoId = extraerYoutubeId(webBuscador.getUrl());
            if (videoId == null) {
                Toast.makeText(MainActivity.this, "No se pudo obtener el video", Toast.LENGTH_SHORT).show();
                return;
            }
            Cancion c = new Cancion(tvTitulo.getText().toString(), videoId, currentChannel, currentThumbnail);
            c.asegurarCanal();
            agregarACola(c);
        });

        btnGuardarFavorito.setOnClickListener(v -> {
            if (webBuscador == null) return;
            String id = extraerYoutubeId(webBuscador.getUrl());
            if (id == null) return;
            Cancion c = new Cancion(tvTitulo.getText().toString(), id, currentChannel, currentThumbnail);
            c.asegurarCanal();

            dbFavoritos.orderByChild("youtubeId").equalTo(id)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                Toast.makeText(MainActivity.this, "El video ya está en favoritos", Toast.LENGTH_SHORT).show();
                            } else {
                                dbFavoritos.push().setValue(c)
                                        .addOnSuccessListener(a -> Toast.makeText(MainActivity.this, "Añadido a favoritos", Toast.LENGTH_SHORT).show())
                                        .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Error al añadir favorito", Toast.LENGTH_SHORT).show());
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(MainActivity.this, "Error al comprobar favoritos", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        btnAñadirALista.setOnClickListener(v -> mostrarDialogoAñadirALista());

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

        btnPerfil.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, EditUserActivity.class);
            intent.putExtra("firebaseId", uidUsuario);
            intent.putExtra("soloEdicionPerfil", true);
            startActivity(intent);
        });
    }

    private void configurarDrawer() {
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        // <editor-fold desc="Menu lateral">
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_favoritos) {
                Intent i = new Intent(MainActivity.this, FavoritosActivity.class);
                i.putExtra("uid_usuario", uidUsuario);
                startActivity(i);
            } else if (id == R.id.nav_listas) {
                Intent i = new Intent(MainActivity.this, ListasReproduccionActivity.class);
                i.putExtra("uid_usuario", uidUsuario);
                startActivity(i);
            } else if (id == R.id.nav_logout) {
                SharedPreferences prefs = getSharedPreferences("MisPrefs", MODE_PRIVATE);
                prefs.edit().remove("uid_usuario").apply();
                Intent logoutIntent = new Intent(MainActivity.this, LoginActivity.class);
                logoutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(logoutIntent);
                finish();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
        // </editor-fold>
    }

    private void reproducirVideo(String videoId) {
        if (webBuscador != null && videoId != null) {
            webBuscador.loadUrl("https://m.youtube.com/watch?v=" + videoId);
        }
    }

    private void agregarACola(Cancion cancion) {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("usuarios")
                .child(uidUsuario)
                .child("historial");

        String id = ref.push().getKey();

        Map<String, Object> data = new HashMap<>();
        data.put("youtubeId", cancion.youtubeId);
        data.put("titulo", cancion.titulo);
        data.put("canal", cancion.canal);
        data.put("url_miniatura", cancion.url_miniatura);

        if (id != null) {
            ref.child(id).setValue(data)
                    .addOnSuccessListener(a -> Toast.makeText(MainActivity.this, "Añadido a la cola", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Error al añadir", Toast.LENGTH_SHORT).show());
        }
    }

    private void guardarEnHistorial(Cancion c) {
        if (c == null || c.youtubeId == null) return;

        dbCanciones.orderByChild("youtubeId")
                .equalTo(c.youtubeId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            dbCanciones.push().setValue(c);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }


    private void cargarColaDesdeFirebase() {
        dbCanciones.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                cola.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String videoId = ds.child("youtubeId").getValue(String.class);
                    if (videoId != null) cola.add(videoId);
                }
                if (!cola.isEmpty() && indiceActual == -1) {
                    indiceActual = 0;
                    reproducirVideo(cola.get(indiceActual));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void cargarFotoPerfilUsuario() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("usuarios").child(uidUsuario).child("fotoPerfil");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    btnPerfil.setImageResource(R.drawable.circle_background);
                    return;
                }
                String uriString = snapshot.getValue(String.class);
                /* Mediante la libreria glide se genera en la cache una version de la imagen seleccionadad en formato de circulo y centrada*/
                if (uriString != null && !uriString.equals("default")) {
                    Glide.with(MainActivity.this)
                            .load(Uri.parse(uriString))
                            .circleCrop()
                            .into(btnPerfil);
                } else {
                    btnPerfil.setImageResource(R.drawable.circle_background);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Error al cargar foto de perfil", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /* Algoritmo para extraer el id de las canciones/videos de youtube a traves de la URL formatos soportados:
    *  - https://youtu.be/VIDEO_ID
    *  - https://www.youtube.com/watch?v=VIDEO_ID
    *  - https://www.youtube.com/embed/VIDEO_ID*/
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

                    DatabaseReference listaRef = dbListas.child(nombreLista);
                    Cancion c = new Cancion(tvTitulo.getText().toString(), videoId, currentChannel, currentThumbnail);

                    listaRef.child(videoId).setValue(c)
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

    /* Interfaz que sirve como puente entre la web de youtube cargada en el Webview y la app mediante el uso de Javascript*/
    public class WebAppInterface {
        @JavascriptInterface
        public void setTitle(String title) {
            runOnUiThread(() -> {
                tvTitulo.setText(title);
                if (cancionActual != null) cancionActual.titulo = title;
            });
        }

        @JavascriptInterface
        public void setChannel(String canal) {
            runOnUiThread(() -> currentChannel = canal);
            if (cancionActual != null) cancionActual.canal = canal;
        }

        @JavascriptInterface
        public void setThumbnail(String url) {
            runOnUiThread(() -> currentThumbnail = url);
            if (cancionActual != null) cancionActual.url_miniatura = url;
        }
        /* Cuando detecta que se ha reproducido un video nuevo actualiza los datos*/
        @JavascriptInterface
        public void onNewVideoDetected(String videoId) {
            runOnUiThread(() -> {
                if (videoId == null) return;

                Cancion c = new Cancion(
                        tvTitulo.getText().toString(),
                        videoId,
                        currentChannel,
                        currentThumbnail
                );
                c.asegurarCanal();
                cancionActual = c;
                guardarEnHistorial(c);
                if (!cola.contains(videoId)) {
                    cola.add(videoId);
                    indiceActual = cola.size() - 1;
                }
            });
        }
    }
}