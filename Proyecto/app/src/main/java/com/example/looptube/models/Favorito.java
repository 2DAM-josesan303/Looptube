package com.example.looptube.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "Favoritos")
public class Favorito {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String id_usuario;
    public String id_video;

    public Favorito() {}

    public Favorito(String id_usuario, String id_video) {
        this.id_usuario = id_usuario;
        this.id_video = id_video;
    }
}