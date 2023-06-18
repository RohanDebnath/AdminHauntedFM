package com.example.adminhauntedfm;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {

    private List<Playlist> playlistList;
    private OnItemClickListener clickListener;

    public interface OnItemClickListener {
        void onEditClick(int position);

        void onDeleteClick(int position);
    }

    public PlaylistAdapter(List<Playlist> playlistList) {
        this.playlistList = playlistList;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }
    public void setPlaylistImage(int position, Drawable image) {
        Playlist playlist = playlistList.get(position);
        playlist.setImageDrawable(image);
        notifyItemChanged(position);
    }
    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist, parent, false);
        return new PlaylistViewHolder(view, clickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        Playlist playlist = playlistList.get(position);
        holder.playlistNameTextView.setText(playlist.getName());
        holder.playlistDescriptionTextView.setText(playlist.getDescription());

        // Load the playlist image using Glide library
        Glide.with(holder.itemView.getContext())
                .load(playlist.getImageUrl())
                .placeholder(R.drawable.ic_launcher_foreground) // Placeholder image while loading
                .error(R.drawable.ic_launcher_foreground) // Error image if loading fails
                .into(holder.playlistImageView);
    }

    @Override
    public int getItemCount() {
        return playlistList.size();
    }


    public static class PlaylistViewHolder extends RecyclerView.ViewHolder {

        public TextView playlistNameTextView;
        public TextView playlistDescriptionTextView;
        public ImageView playlistImageView;
        public ImageView editImageView;
        public ImageView deleteImageView;

        public PlaylistViewHolder(@NonNull View itemView, final OnItemClickListener listener) {
            super(itemView);

            playlistNameTextView = itemView.findViewById(R.id.playlistNameTextView);
            playlistDescriptionTextView = itemView.findViewById(R.id.playlistDescriptionTextView);
            playlistImageView = itemView.findViewById(R.id.playlistImageView);
            editImageView = itemView.findViewById(R.id.editImageView);
            deleteImageView = itemView.findViewById(R.id.deleteImageView);

            editImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onEditClick(position);
                        }
                    }
                }
            });

            deleteImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onDeleteClick(position);
                        }
                    }
                }
            });
        }
    }
}
