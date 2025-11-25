package com.example.looptube.Adaptadores;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.looptube.R;
import com.example.looptube.models.Cancion;
import com.google.firebase.database.FirebaseDatabase;

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

        // Reproducir al tocar la fila
        holder.itemView.setOnClickListener(v -> listener.onPlay(c));

        // Eliminar solo de la cola (tabla "canciones")
        holder.btnEliminar.setOnClickListener(v -> {
            new AlertDialog.Builder(holder.itemView.getContext())
                    .setTitle("Eliminar De El Historial")
                    .setMessage("¿Seguro que quieres eliminar " + c.titulo + " de el historial?")
                    .setPositiveButton("Sí", (dialog, which) -> {
                        if (c.youtubeId != null) {
                            FirebaseDatabase.getInstance().getReference("canciones")
                                    .child(c.youtubeId)
                                    .removeValue()
                                    .addOnSuccessListener(a -> {
                                        items.remove(position);
                                        notifyItemRemoved(position);
                                        Toast.makeText(holder.itemView.getContext(),
                                                "Canción eliminada de el historial", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(holder.itemView.getContext(),
                                            "Error al eliminar", Toast.LENGTH_SHORT).show());
                        }
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle;
        ImageButton btnEliminar;

        VH(View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tvHistTitle);
            btnEliminar = v.findViewById(R.id.btnEliminar);
        }
    }
}