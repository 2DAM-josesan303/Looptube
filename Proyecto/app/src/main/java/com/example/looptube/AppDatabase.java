package com.example.looptube;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.example.looptube.models.Usuario;
import com.example.looptube.models.Lista;
import com.example.looptube.models.Cancion;
import com.example.looptube.models.Favorito;
import com.example.looptube.models.Historial;
import com.example.looptube.DAO.DAO;

@Database(
        entities = {Usuario.class, Lista.class, Cancion.class, Favorito.class, Historial.class},
        version = 1,
        exportSchema = false
)
/* Clase abstracta para implementacion con Room y el DAO*/
public abstract class AppDatabase extends RoomDatabase {
    public abstract DAO dao();
}
