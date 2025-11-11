package com.example.looptube;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private YouTubePlayerView youTubePlayerView;
    private YouTubePlayer youTubePlayer;
    private EditText etBuscar;
    private ImageButton btnPlay, btnPrev, btnNext;
    private TextView tvTitulo;

    private final String[] videoIds = {"dQw4w9WgXcQ", "9bZkp7q19f0", "3JZ_D3ELwOQ"};
    private int currentVideoIndex = 0;

    // 🔑 Sustituye con tu clave de API
    private static final String API_KEY = "AIzaSyCmqhyZBfbSmfG-4ZqwQT40ffdapRYICzw";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        youTubePlayerView = findViewById(R.id.youtubePlayer);
        etBuscar = findViewById(R.id.etBuscar);
        btnPlay = findViewById(R.id.btnPlay);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        tvTitulo = findViewById(R.id.tvTitulo);

        getLifecycle().addObserver(youTubePlayerView);

        youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(YouTubePlayer player) {
                youTubePlayer = player;
                reproducirVideoActual();
            }
        });

        etBuscar.setOnEditorActionListener((v, actionId, event) -> {
            String query = etBuscar.getText().toString().trim();
            if (!query.isEmpty()) {
                buscarVideoEnYouTube(query);
            } else {
                Toast.makeText(this, "Escribe algo para buscar", Toast.LENGTH_SHORT).show();
            }
            return true;
        });

        btnPlay.setOnClickListener(v -> {
            if (youTubePlayer != null) {
                youTubePlayer.pause();
                Toast.makeText(this, "Play/Pausa", Toast.LENGTH_SHORT).show();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (youTubePlayer != null) {
                currentVideoIndex = (currentVideoIndex + 1) % videoIds.length;
                reproducirVideoActual();
            }
        });

        btnPrev.setOnClickListener(v -> {
            if (youTubePlayer != null) {
                currentVideoIndex = (currentVideoIndex - 1 + videoIds.length) % videoIds.length;
                reproducirVideoActual();
            }
        });
    }

    private void reproducirVideoActual() {
        if (youTubePlayer != null) {
            String videoId = videoIds[currentVideoIndex];
            youTubePlayer.loadVideo(videoId, 0);
            tvTitulo.setText("Video actual: " + videoId);
        }
    }

    private void buscarVideoEnYouTube(String query) {
        OkHttpClient client = new OkHttpClient();
        String url = "https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&maxResults=1&q="
                + query.replace(" ", "%20")
                + "&key=" + API_KEY;

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonData = response.body().string();
                        JSONObject jsonObject = new JSONObject(jsonData);
                        JSONArray items = jsonObject.getJSONArray("items");

                        if (items.length() > 0) {
                            JSONObject video = items.getJSONObject(0);
                            String videoId = video.getJSONObject("id").getString("videoId");
                            String titulo = video.getJSONObject("snippet").getString("title");

                            runOnUiThread(() -> {
                                tvTitulo.setText(titulo);
                                if (youTubePlayer != null) {
                                    youTubePlayer.loadVideo(videoId, 0);
                                }
                            });
                        } else {
                            runOnUiThread(() ->
                                    Toast.makeText(MainActivity.this, "No se encontraron resultados", Toast.LENGTH_SHORT).show()
                            );
                        }
                    } catch (Exception e) {
                        runOnUiThread(() ->
                                Toast.makeText(MainActivity.this, "Error al procesar resultados", Toast.LENGTH_SHORT).show()
                        );
                    }
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        youTubePlayerView.release();
    }
}