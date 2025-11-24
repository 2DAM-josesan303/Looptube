package com.example.looptube.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cancion")
public class Cancion {
    @PrimaryKey(autoGenerate = true)
    public int id_video;
    public String titulo;
    public String youtubeId;
    public String key;
    public String canal;
    public String url_miniatura;

    public Cancion() {}

    public Cancion(String titulo, String youtubeId,String canal, String url_miniatura) {
        this.titulo = titulo;
        this.youtubeId = youtubeId;
        this.canal = canal;
        this.url_miniatura = url_miniatura;
    }
}