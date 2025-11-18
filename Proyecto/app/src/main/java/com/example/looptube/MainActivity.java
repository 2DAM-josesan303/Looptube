package com.example.looptube;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.looptube.models.Cancion;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import java.util.Deque;
import java.util.LinkedList;

public class MainActivity extends AppCompatActivity {

    private YouTubePlayerView youtubePlayerView;
    private YouTubePlayer player;
    private ImageButton btnPrev, btnNext;
    private TextView tvTitulo;

    private final Deque<Cancion> videoQueue = new LinkedList<>();
    private final Deque<Cancion> historial = new LinkedList<>();
    private Cancion currentVideo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        youtubePlayerView = findViewById(R.id.youtubePlayer);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        tvTitulo = findViewById(R.id.tvTitulo);

        getLifecycle().addObserver(youtubePlayerView);

        // Video inicial
        videoQueue.add(new Cancion("Bonobo - Kerala", "S0Q4gqBUs7c", "", ""));

        youtubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(YouTubePlayer youTubePlayer) {
                player = youTubePlayer;
                reproducirSiguienteVideo();
            }
        });

        btnNext.setOnClickListener(v -> reproducirSiguienteVideo());
        btnPrev.setOnClickListener(v -> reproducirVideoAnterior());
    }

    private void reproducirSiguienteVideo() {
        if (!videoQueue.isEmpty()) {
            if (currentVideo != null) historial.push(currentVideo);
            currentVideo = videoQueue.poll();
            tvTitulo.setText(currentVideo.titulo);

            if (player != null) {
                player.loadVideo(currentVideo.youtubeId, 0f);
            }
        } else {
            Toast.makeText(this, "No hay más videos en la cola", Toast.LENGTH_SHORT).show();
        }
    }

    private void reproducirVideoAnterior() {
        if (!historial.isEmpty()) {
            currentVideo = historial.pop();
            tvTitulo.setText(currentVideo.titulo);
            if (player != null) {
                player.loadVideo(currentVideo.youtubeId, 0f);
            }
        } else {
            Toast.makeText(this, "No hay video anterior", Toast.LENGTH_SHORT).show();
        }
    }
}