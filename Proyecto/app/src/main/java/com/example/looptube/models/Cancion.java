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

    // -----------------------------
    // Método estático para extraer canal desde el título
    // -----------------------------
    public static String extraerCanal(String titulo) {
        if (titulo == null || !titulo.contains(" - ")) return "Canal desconocido";
        int primerGuion = titulo.indexOf(" - ");
        return titulo.substring(0, primerGuion).trim();
    }

    // Método de utilidad para asegurarse de que el canal está correcto
    public void asegurarCanal() {
        if (this.canal == null || this.canal.equals("Canal desconocido")) {
            this.canal = extraerCanal(this.titulo);
        }
    }
}