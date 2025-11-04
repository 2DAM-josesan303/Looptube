package com.example.looptube.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "usuario")
public class Usuario {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String nombre;
    public String email;
    public String contraseña_hash;
    public String rol;

    public Usuario() {}

    public Usuario(String nombre, String email, String contraseña_hash, String rol) {
        this.nombre = nombre;
        this.email = email;
        this.contraseña_hash = contraseña_hash;
        this.rol = rol;
    }
}