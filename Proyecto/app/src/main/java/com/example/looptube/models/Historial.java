package com.example.looptube.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "Historial")
public class Historial {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int id_usuario;
    public int id_video;
    public String fecha_reproduccion;

    public Historial() {}

    public Historial(int id_usuario, int id_video, String fecha_reproduccion) {
        this.id_usuario = id_usuario;
        this.id_video = id_video;
        this.fecha_reproduccion = fecha_reproduccion;
    }
}