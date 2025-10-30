package com.example.looptube.models;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import java.util.List;

public class Historial_Canciones {
    @Embedded
    public Usuario usuario;

    @Relation(
            parentColumn = "id",
            entityColumn = "id_video",
            associateBy = @Junction(
                    value = Historial.class,
                    parentColumn = "id_usuario",
                    entityColumn = "id_video"
            )
    )
    public List<Cancion> cancionesReproducidas;
}
