package com.example.looptube;

public class Usuario {
    public String id;
    public String email;
    public String contraseña_hash;
    public String rol;

    public Usuario() {}

    public Usuario(String id, String email, String contraseña_hash, String rol) {
        this.id = id;
        this.email = email;
        this.contraseña_hash = contraseña_hash;
        this.rol = rol;
    }
}
