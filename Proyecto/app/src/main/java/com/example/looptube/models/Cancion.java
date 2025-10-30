package com.example.looptube.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "Canciones")
public class Cancion {
    @PrimaryKey(autoGenerate = true)
    public int id_video;
    public String titulo;
    public String canal;
    public String url_miniatura;

    public Cancion() {}

    public Cancion(String titulo, String canal, String url_miniatura) {
        this.titulo = titulo;
        this.canal = canal;
        this.url_miniatura = url_miniatura;
    }
}