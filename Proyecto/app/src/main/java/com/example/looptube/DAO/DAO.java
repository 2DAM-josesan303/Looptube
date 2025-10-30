package com.example.looptube.DAO;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.looptube.models.Historial_Canciones;
import com.example.looptube.models.Usuario;
import com.example.looptube.models.Lista;
import com.example.looptube.models.Cancion;
import com.example.looptube.models.Favorito;
import com.example.looptube.models.Historial;
import com.example.looptube.models.Historial_Canciones;

import java.util.List;

@Dao
public interface DAO {

    // <editor-fold desc="USUARIOS">
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertarUsuario(Usuario usuario);

    @Query("SELECT * FROM Usuarios")
    List<Usuario> obtenerUsuarios();

    @Query("SELECT * FROM Usuarios WHERE id = :idUsuario LIMIT 1")
    Usuario obtenerUsuarioPorId(int idUsuario);

    @Query("DELETE FROM Usuarios WHERE id = :idUsuario")
    void eliminarUsuario(int idUsuario);
    // </editor-fold>

    // <editor-fold desc="LISTAS">
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertarLista(Lista lista);

    @Query("SELECT * FROM Listas WHERE id_usuario = :idUsuario")
    List<Lista> obtenerListasPorUsuario(int idUsuario);

    @Query("DELETE FROM Listas WHERE id_lista = :idLista")
    void eliminarLista(int idLista);
    // </editor-fold>

    // <editor-fold desc="CANCIONES">
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertarCancion(Cancion cancion);

    @Query("SELECT * FROM Canciones")
    List<Cancion> obtenerCanciones();

    @Query("SELECT * FROM Canciones WHERE id_video = :idVideo LIMIT 1")
    Cancion obtenerCancionPorId(int idVideo);

    @Query("DELETE FROM Canciones WHERE id_video = :idVideo")
    void eliminarCancion(int idVideo);
    // </editor-fold>

    // <editor-fold desc="FAVORITOS">
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertarFavorito(Favorito favorito);

    @Query("SELECT * FROM Favoritos WHERE id_usuario = :idUsuario")
    List<Favorito> obtenerFavoritosPorUsuario(int idUsuario);

    @Query("DELETE FROM Favoritos WHERE id_usuario = :idUsuario AND id_video = :idVideo")
    void eliminarFavorito(int idUsuario, int idVideo);
    // </editor-fold>

    // <editor-fold desc="HISTORIAL">
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertarHistorial(Historial historial);
    //obtiene usuario + canciones reproducidas
    @Transaction
    @Query("SELECT * FROM Usuarios WHERE id = :idUsuario")
    Historial_Canciones obtenerHistorialCompleto(int idUsuario);

    @Query("SELECT * FROM Historial WHERE id_usuario = :idUsuario ORDER BY fecha_reproduccion DESC")
    List<Historial> obtenerHistorialPorUsuario(int idUsuario);

    @Query("DELETE FROM Historial WHERE id_usuario = :idUsuario")
    void eliminarHistorialDeUsuario(int idUsuario);
    // </editor-fold>
}
