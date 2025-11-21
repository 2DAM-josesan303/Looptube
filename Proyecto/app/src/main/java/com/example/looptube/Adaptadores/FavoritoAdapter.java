package com.example.looptube.Adaptadores;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
        holder.tv.setText(c.titulo);

        holder.btnPlay.setOnClickListener(v -> listener.onPlay(c));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(c, position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {

        TextView tv;
        ImageButton btnPlay, btnDelete;

        VH(View v) {
            super(v);
            tv = v.findViewById(R.id.tvTituloItem);
            btnPlay = v.findViewById(R.id.btnPlayItem);
            btnDelete = v.findViewById(R.id.btnDeleteItem);
        }
    }
}