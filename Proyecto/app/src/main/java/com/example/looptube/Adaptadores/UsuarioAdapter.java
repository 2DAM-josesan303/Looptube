package com.example.looptube.Adaptadores;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.example.looptube.R;
import com.example.looptube.models.Usuario;

import java.util.List;

public class UsuarioAdapter extends ArrayAdapter<Usuario> {

    private Context context;
    private List<Usuario> usuarios;
    private OnUsuarioActionListener listener;

    public interface OnUsuarioActionListener {
        void onEditar(Usuario usuario);
        void onEliminar(Usuario usuario);
    }

    public UsuarioAdapter(Context context, List<Usuario> usuarios, OnUsuarioActionListener listener) {
        super(context, 0, usuarios);
        this.context = context;
        this.usuarios = usuarios;
        this.listener = listener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Usuario usuario = usuarios.get(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.usuario_item, parent, false);
        }

        TextView tvNombre = convertView.findViewById(R.id.tvNombreUsuario);
        Button btnEditar = convertView.findViewById(R.id.btnEditar);
        Button btnEliminar = convertView.findViewById(R.id.btnEliminar);

        tvNombre.setText(usuario.nombre + " (" + usuario.rol + ")");

        btnEditar.setOnClickListener(v -> listener.onEditar(usuario));
        btnEliminar.setOnClickListener(v -> listener.onEliminar(usuario));

        return convertView;
    }
}