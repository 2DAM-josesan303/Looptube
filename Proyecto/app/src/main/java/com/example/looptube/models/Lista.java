package com.example.looptube.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "Listas")
public class Lista {
    @PrimaryKey(autoGenerate = true)
    public int id_lista;

    public String id_usuario;
    public String nombre;

    public Lista() {}

    public Lista(String id_usuario, String nombre) {
        this.id_usuario = id_usuario;
        this.nombre = nombre;
    }
}
