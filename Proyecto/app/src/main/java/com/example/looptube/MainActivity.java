package com.example.looptube;

import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Deque;
import java.util.LinkedList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private EditText etBuscar;
    private ImageButton btnPrev, btnNext, btnPlayPause;
    private TextView tvTitulo;

    private final Deque<String> videoQueue = new LinkedList<>();
    private final Deque<String> historial = new LinkedList<>();
    private String currentVideoId;

    private boolean isPlaying = true;
    private static final String API_KEY = "TU_API_KEY_AQUI";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.youtubePlayer);
        etBuscar = findViewById(R.id.etBuscar);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        btnPlayPause = findViewById(R.id.btnPlay);
        tvTitulo = findViewById(R.id.tvTitulo);

        // Configuración del WebView
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webView.setWebViewClient(new WebViewClient()); // evita abrir YouTube externo

        // Videos iniciales
        videoQueue.add("S0Q4gqBUs7c");
        videoQueue.add("E7wJTI-1dvQ");
        videoQueue.add("5qap5aO4i9A");

        reproducirSiguienteVideo();

        // Buscador
        etBuscar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = etBuscar.getText().toString().trim();
                if (!query.isEmpty()) buscarVideoEnYouTube(query);
                return true;
            }
            return false;
        });

        // Botones
        btnNext.setOnClickListener(v -> reproducirSiguienteVideo());
        btnPrev.setOnClickListener(v -> reproducirVideoAnterior());
        btnPlayPause.setOnClickListener(v -> {
            if (currentVideoId != null) {
                if (isPlaying) pausarVideo();
                else reproducirVideo(currentVideoId);
                isPlaying = !isPlaying;
            }
        });
    }

    private void reproducirSiguienteVideo() {
        if (!videoQueue.isEmpty()) {
            if (currentVideoId != null) historial.push(currentVideoId);
            currentVideoId = videoQueue.poll();
            isPlaying = true;
            reproducirVideo(currentVideoId);
        } else {
            Toast.makeText(this, "No hay más videos en la cola", Toast.LENGTH_SHORT).show();
        }
    }

    private void reproducirVideoAnterior() {
        if (!historial.isEmpty()) {
            currentVideoId = historial.pop();
            isPlaying = true;
            reproducirVideo(currentVideoId);
        } else {
            Toast.makeText(this, "No hay video anterior", Toast.LENGTH_SHORT).show();
        }
    }

    private void reproducirVideo(String videoId) {
        String html = "<html><body style='margin:0'>" +
                "<div id='player'></div>" +
                "<script>" +
                "var tag = document.createElement('script');" +
                "tag.src = 'https://www.youtube.com/iframe_api';" +
                "var firstScriptTag = document.getElementsByTagName('script')[0];" +
                "firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);" +
                "var player;" +
                "function onYouTubeIframeAPIReady() {" +
                "  player = new YT.Player('player', {" +
                "    height: '100%', width: '100%'," +
                "    videoId: '" + videoId + "'," +
                "    events: { 'onReady': onPlayerReady }" +
                "  });" +
                "}" +
                "function onPlayerReady(event) { event.target.playVideo(); }" +
                "function playVideo() { if(player) player.playVideo(); }" +
                "function pauseVideo() { if(player) player.pauseVideo(); }" +
                "</script></body></html>";
        webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
        tvTitulo.setText("Video ID: " + videoId);
    }

    private void pausarVideo() {
        webView.evaluateJavascript("pauseVideo();", null);
    }

    private void buscarVideoEnYouTube(String query) {
        OkHttpClient client = new OkHttpClient();
        String url = "https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&maxResults=1&q="
                + query.replace(" ", "%20")
                + "&videoEmbeddable=true"
                + "&key=" + API_KEY;

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show());
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonData = response.body().string();
                        JSONObject jsonObject = new JSONObject(jsonData);
                        JSONArray items = jsonObject.getJSONArray("items");

                        if (items.length() > 0) {
                            JSONObject video = items.getJSONObject(0);
                            final String videoId = video.getJSONObject("id").getString("videoId");
                            final String titulo = video.getJSONObject("snippet").getString("title");

                            runOnUiThread(() -> {
                                videoQueue.addFirst(videoId);
                                tvTitulo.setText(titulo);
                                reproducirSiguienteVideo();
                            });
                        } else {
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "No se encontraron resultados", Toast.LENGTH_SHORT).show());
                        }
                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error procesando resultados", Toast.LENGTH_SHORT).show());
                    }
                }
            }
        });
    }
}