package com.example.looptube.Adaptadores;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.looptube.R;
import com.example.looptube.models.Cancion;

import java.util.List;

public class HistorialAdapter extends RecyclerView.Adapter<HistorialAdapter.VH> {

    public interface Listener {
        void onPlay(Cancion c);
    }

    private final List<Cancion> items;
    private final Listener listener;

    public HistorialAdapter(List<Cancion> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_historial, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Cancion c = items.get(position);
        holder.tvTitle.setText(c.titulo);

        holder.itemView.setOnClickListener(v -> listener.onPlay(c));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle;

        VH(View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tvHistTitle);
        }
    }
}