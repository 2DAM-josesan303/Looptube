package com.example.looptube.Adaptadores;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.looptube.R;

import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.VH> {

    public interface Listener {
        void onClickLista(String nombre);
        void onDelete(String nombre, int pos);
    }

    private final List<String> listas;
    private final Listener listener;

    public PlaylistAdapter(List<String> listas, Listener listener) {
        this.listas = listas;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lista_reproduccion, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        String nombre = listas.get(position);

        holder.tvNombreLista.setText(nombre);

        holder.itemView.setOnClickListener(v -> listener.onClickLista(nombre));

        holder.btnEliminar.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                listener.onDelete(nombre, pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listas.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvNombreLista;
        ImageView ivIcono;
        ImageButton btnEliminar;

        VH(View v) {
            super(v);
            tvNombreLista = v.findViewById(R.id.tvNombreLista);
            ivIcono = v.findViewById(R.id.ivIconoLista);
            btnEliminar = v.findViewById(R.id.btnEliminarLista);
        }
    }
}