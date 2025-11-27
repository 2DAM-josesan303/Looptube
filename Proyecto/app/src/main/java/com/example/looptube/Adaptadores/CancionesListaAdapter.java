package com.example.looptube.Adaptadores;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.looptube.R;
import com.example.looptube.models.Cancion;

import java.util.List;

public class CancionesListaAdapter extends RecyclerView.Adapter<CancionesListaAdapter.VH> {

    public interface Listener {
        void onPlay(Cancion c);
        void onDelete(Cancion c, int pos);
    }

    private final List<Cancion> canciones;
    private final Listener listener;

    public CancionesListaAdapter(List<Cancion> canciones, Listener listener) {
        this.canciones = canciones;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cancion_lista, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Cancion c = canciones.get(position);

        holder.tvTitulo.setText(c.titulo);

        Glide.with(holder.itemView.getContext())
                .load(c.url_miniatura)
                .placeholder(R.drawable.ic_placeholder)
                .into(holder.ivMiniatura);

        holder.btnPlay.setOnClickListener(v -> listener.onPlay(c));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(c, holder.getAdapterPosition()));

        holder.itemView.setOnClickListener(v -> listener.onPlay(c));
    }

    @Override
    public int getItemCount() {
        return canciones.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitulo;
        ImageView ivMiniatura;
        ImageButton btnDelete, btnPlay;

        VH(View v) {
            super(v);
            tvTitulo = v.findViewById(R.id.tvTituloCancionLista);
            ivMiniatura = v.findViewById(R.id.ivMiniaturaLista);
            btnPlay = v.findViewById(R.id.btnPlayCancionLista);
            btnDelete = v.findViewById(R.id.btnEliminarCancionLista);
        }
    }
}