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
    public String id_firebase;
    public int id_lista;

    public Cancion() {}

    public Cancion(String titulo, String youtubeId, String canal, String url_miniatura) {
        this.titulo = titulo;
        this.youtubeId = youtubeId;
        this.canal = canal;
        this.url_miniatura = url_miniatura;
    }

    public Cancion(String titulo, String youtubeId, String canal, String url_miniatura, int id_lista) {
        this.titulo = titulo;
        this.youtubeId = youtubeId;
        this.canal = canal;
        this.url_miniatura = url_miniatura;
        this.id_lista = id_lista;
    }

    public static String extraerCanal(String titulo) {
        if (titulo == null || !titulo.contains(" - ")) return "Canal desconocido";
        int primerGuion = titulo.indexOf(" - ");
        return titulo.substring(0, primerGuion).trim();
    }

    public void asegurarCanal() {
        if (this.canal == null || this.canal.equals("Canal desconocido")) {
            this.canal = extraerCanal(this.titulo);
        }
    }
}