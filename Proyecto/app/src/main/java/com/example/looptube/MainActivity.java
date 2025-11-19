package com.example.looptube;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private EditText etUrl;
    private Button btnCargar, btnAñadirCola;
    private WebView webBuscador;
    private TextView tvTitulo;
    private ImageButton btnPrev, btnNext;

    private ArrayList<String> cola = new ArrayList<>();
    private int indiceActual = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etUrl = findViewById(R.id.etUrl);
        btnCargar = findViewById(R.id.btnCargar);
        btnAñadirCola = findViewById(R.id.btnAñadirCola);
        webBuscador = findViewById(R.id.webBuscador);
        tvTitulo = findViewById(R.id.tvTitulo);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);

        // Configurar WebView
        webBuscador.getSettings().setJavaScriptEnabled(true);
        webBuscador.addJavascriptInterface(new WebAppInterface(), "Android");
        webBuscador.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // Obtener título automáticamente
                view.loadUrl("javascript:Android.setTitle(document.title);");
            }
        });
        webBuscador.loadUrl("https://m.youtube.com");

        // Botón "Reproducir"
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

        // Botón "Añadir a la cola"
        btnAñadirCola.setOnClickListener(v -> {
            String url = etUrl.getText().toString().trim();
            String id = extraerYoutubeId(url);
            if (id == null) {
                Toast.makeText(this, "URL no válida", Toast.LENGTH_SHORT).show();
                return;
            }
            cola.add(id);
            Toast.makeText(this, "Añadido a la cola", Toast.LENGTH_SHORT).show();
        });

        // Botón "Anterior"
        btnPrev.setOnClickListener(v -> {
            if (indiceActual > 0) {
                indiceActual--;
                reproducirVideo(cola.get(indiceActual));
            } else {
                Toast.makeText(this, "No hay video anterior", Toast.LENGTH_SHORT).show();
            }
        });

        // Botón "Siguiente"
        btnNext.setOnClickListener(v -> {
            if (indiceActual < cola.size() - 1) {
                indiceActual++;
                reproducirVideo(cola.get(indiceActual));
            } else {
                Toast.makeText(this, "No hay más videos en la cola", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void reproducirVideo(String id) {
        String url = "https://m.youtube.com/watch?v=" + id;
        webBuscador.loadUrl(url);
    }

    // Extraer ID de YouTube
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
        if (webBuscador.canGoBack()) {
            webBuscador.goBack();
        } else {
            super.onBackPressed();
        }
    }

    // Interfaz para recibir título desde JavaScript
    public class WebAppInterface {
        @JavascriptInterface
        public void setTitle(String title) {
            runOnUiThread(() -> tvTitulo.setText(title));
        }
    }
}