package com.example.adminhauntedfm;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AudioAdapter extends RecyclerView.Adapter<AudioAdapter.ViewHolder> {
    private Context context;
    private List<AudioItem> audioItems;
    private OnItemClickListener listener;

    public AudioAdapter(Context context, List<AudioItem> audioItems, OnItemClickListener listener) {
        this.context = context;
        this.audioItems = audioItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_audio, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AudioItem audioItem = audioItems.get(position);

        holder.audioNameTextView.setText(audioItem.getAudioName());
        holder.audioDescriptionTextView.setText(audioItem.getAudioDescription());

        if (audioItem.getAudioFileUrl() != null) {
            holder.audioFileUrlTextView.setVisibility(View.VISIBLE);
            holder.audioFileUrlTextView.setText(audioItem.getAudioFileUrl());
        } else {
            holder.audioFileUrlTextView.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onItemClick(audioItem);
            }
        });

        holder.editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onEditClick(audioItem);
            }
        });

        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onDeleteClick(audioItem);
            }
        });
    }

    @Override
    public int getItemCount() {
        return audioItems.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView audioNameTextView;
        TextView audioDescriptionTextView;
        TextView audioFileUrlTextView;
        Button editButton;
        Button deleteButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            audioNameTextView = itemView.findViewById(R.id.audioNameTextView);
            audioDescriptionTextView = itemView.findViewById(R.id.audioDescriptionTextView);
            audioFileUrlTextView = itemView.findViewById(R.id.audioFileUrlTextView);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(AudioItem audioItem);

        void onEditClick(AudioItem audioItem);

        void onDeleteClick(AudioItem audioItem);
    }
}
