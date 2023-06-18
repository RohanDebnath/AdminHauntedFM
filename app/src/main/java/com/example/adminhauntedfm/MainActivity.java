package com.example.adminhauntedfm;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private CollectionReference playlistCollection;
    private EditText playlistNameEditText;
    private EditText playlistDescriptionEditText;
    private RecyclerView playlistRecyclerView;
    private PlaylistAdapter playlistAdapter;
    private List<Playlist> playlistList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        playlistCollection = db.collection("playlists");


        playlistNameEditText = findViewById(R.id.playlistNameEditText);
        playlistDescriptionEditText = findViewById(R.id.playlistDescriptionEditText);
        playlistRecyclerView = findViewById(R.id.playlistRecyclerView);

        Button addButton = findViewById(R.id.addButton);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addPlaylist();
            }
        });
        Button refreshButton = findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadPlaylists();
            }
        });

        // Initialize playlist data
        playlistList = new ArrayList<>();
        playlistAdapter = new PlaylistAdapter(playlistList);
        playlistRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        playlistRecyclerView.setAdapter(playlistAdapter);

        // Load existing playlists
        loadPlaylists();

        // Set click listeners for list items
        playlistAdapter.setOnItemClickListener(new PlaylistAdapter.OnItemClickListener() {
            @Override
            public void onEditClick(int position) {
                Playlist playlist = playlistList.get(position);
                editPlaylist(playlist);
                Toast.makeText(MainActivity.this, "Edit clicked for Playlist: " + playlist.getName(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeleteClick(int position) {
                Playlist playlist = playlistList.get(position);
                deletePlaylist(playlist);
            }
        });
    }

    private void addPlaylist() {
        String name = playlistNameEditText.getText().toString().trim();
        String description = playlistDescriptionEditText.getText().toString().trim();

        // Validate input
        if (name.isEmpty()) {
            playlistNameEditText.setError("Please enter a name");
            playlistNameEditText.requestFocus();
            return;
        }

        if (description.isEmpty()) {
            playlistDescriptionEditText.setError("Please enter a description");
            playlistDescriptionEditText.requestFocus();
            return;
        }

        // Create a new playlist object
        Playlist playlist = new Playlist(name, description);

        // Add the playlist to Firestore
        playlistCollection.add(playlist)
                .addOnSuccessListener(documentReference -> {
                    // Playlist added successfully
                    playlist.setId(documentReference.getId());
                    playlistList.add(playlist);
                    playlistAdapter.notifyDataSetChanged();
                    playlistNameEditText.setText("");
                    playlistDescriptionEditText.setText("");
                    Toast.makeText(MainActivity.this, "Playlist added", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Error adding playlist: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void deletePlaylist(Playlist playlist) {
        // Delete the playlist from Firestore
        playlistCollection.document(playlist.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Playlist deleted successfully
                    playlistList.remove(playlist);
                    playlistAdapter.notifyDataSetChanged();
                    Toast.makeText(MainActivity.this, "Playlist deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Error deleting playlist: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadPlaylists() {
        playlistCollection.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    playlistList.clear();
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        String playlistId = documentSnapshot.getId();
                        String playlistName = documentSnapshot.getString("name");
                        String playlistDescription = documentSnapshot.getString("description");
                        Playlist playlist = new Playlist(playlistName, playlistDescription);
                        playlist.setId(playlistId);
                        playlistList.add(playlist);
                    }
                    playlistAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Error loading playlists: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void editPlaylist(Playlist playlist) {
        playlistCollection.document(playlist.getId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    // Get the playlist data
                    String name = documentSnapshot.getString("name");
                    String description = documentSnapshot.getString("description");

                    // Create the edit dialog
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Edit Playlist");

                    // Inflate the dialog layout
                    View editDialogView = getLayoutInflater().inflate(R.layout.dialog_edit_playlist, null);
                    EditText editPlaylistNameEditText = editDialogView.findViewById(R.id.editPlaylistNameEditText);
                    EditText editPlaylistDescriptionEditText = editDialogView.findViewById(R.id.editPlaylistDescriptionEditText);
                    editPlaylistNameEditText.setText(name);
                    editPlaylistDescriptionEditText.setText(description);

                    builder.setView(editDialogView);

                    // Set click listener for cancel button
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Close the dialog
                            dialog.dismiss();
                        }
                    });

                    // Set click listener for save button
                    builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Get the updated playlist details
                            String updatedName = editPlaylistNameEditText.getText().toString().trim();
                            String updatedDescription = editPlaylistDescriptionEditText.getText().toString().trim();

                            // Update the playlist details in Firestore
                            playlist.setName(updatedName);
                            playlist.setDescription(updatedDescription);
                            updatePlaylist(playlist);
                        }
                    });

                    // Show the edit dialog
                    AlertDialog editDialog = builder.create();
                    editDialog.show();
                })
                .addOnFailureListener(e -> {
                    // Error occurred while retrieving playlist details
                    Toast.makeText(MainActivity.this, "Error retrieving playlist details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updatePlaylist(Playlist playlist) {
        // Update the playlist details in Firestore
        playlistCollection.document(playlist.getId())
                .update("name", playlist.getName(), "description", playlist.getDescription())
                .addOnSuccessListener(aVoid -> {
                    // Playlist updated successfully
                    Toast.makeText(MainActivity.this, "Playlist updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    // Error occurred while updating the playlist
                    Toast.makeText(MainActivity.this, "Error updating playlist: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private static class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {

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
        }

        @Override
        public int getItemCount() {
            return playlistList.size();
        }

        public static class PlaylistViewHolder extends RecyclerView.ViewHolder {

            public TextView playlistNameTextView;
            public TextView playlistDescriptionTextView;
            public ImageView editImageView;
            public ImageView deleteImageView;

            public PlaylistViewHolder(@NonNull View itemView, final OnItemClickListener listener) {
                super(itemView);

                playlistNameTextView = itemView.findViewById(R.id.playlistNameTextView);
                playlistDescriptionTextView = itemView.findViewById(R.id.playlistDescriptionTextView);
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
}
