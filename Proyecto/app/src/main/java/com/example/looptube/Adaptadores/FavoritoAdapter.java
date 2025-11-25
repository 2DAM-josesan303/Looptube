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

public class FavoritoAdapter extends RecyclerView.Adapter<FavoritoAdapter.VH> {

    public interface Listener {
        void onPlay(Cancion c);
        void onDelete(Cancion c, int pos);
    }

    private final List<Cancion> items;
    private final Listener listener;

    public FavoritoAdapter(List<Cancion> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cancion, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Cancion c = items.get(position);

        holder.tvTitulo.setText(c.titulo);
        holder.tvCanal.setText(c.canal);

        // Cargar miniatura con Glide
        Glide.with(holder.itemView.getContext())
                .load(c.url_miniatura)
                .placeholder(R.drawable.ic_placeholder) // opcional
                .into(holder.ivMiniatura);

        // Botón reproducir
        holder.btnPlay.setOnClickListener(v -> listener.onPlay(c));

        // Botón eliminar
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(c, holder.getAdapterPosition()));

        // Click sobre el item completo reproduce también
        holder.itemView.setOnClickListener(v -> listener.onPlay(items.get(holder.getAdapterPosition())));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitulo, tvCanal;
        ImageView ivMiniatura;
        ImageButton btnPlay, btnDelete;

        VH(View v) {
            super(v);
            tvTitulo = v.findViewById(R.id.tvTituloItem);
            tvCanal = v.findViewById(R.id.tvCanalItem); // nuevo TextView para canal
            ivMiniatura = v.findViewById(R.id.ivMiniaturaItem); // ImageView miniatura
            btnPlay = v.findViewById(R.id.btnPlayItem);
            btnDelete = v.findViewById(R.id.btnDeleteItem);
        }
    }
}