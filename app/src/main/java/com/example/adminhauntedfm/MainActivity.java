package com.example.adminhauntedfm;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

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
    private ImageView imageViewPlaylist;
    private Uri selectedImageUri;
    private Button btnSelectImage,editButton;

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
        imageViewPlaylist = findViewById(R.id.imageViewPlaylist);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImageSelector();
            }
        });

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

    private void openImageSelector() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            imageViewPlaylist.setImageURI(selectedImageUri);
        }
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

        // Generate a custom document ID
        String playlistId = playlistCollection.document().getId();
        playlist.setId(playlistId); // Set the ID of the playlist

        // Upload the image to Firebase Storage
        if (selectedImageUri != null) {
            String imageName = "playlist_" + playlistId;
            StorageReference imageRef = FirebaseStorage.getInstance().getReference().child("images/" + imageName);

            imageRef.putFile(selectedImageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        // Image upload successful, get the image URL
                        imageRef.getDownloadUrl()
                                .addOnSuccessListener(uri -> {
                                    String imageUrl = uri.toString();
                                    playlist.setImageUrl(imageUrl); // Set the image URL in the playlist object

                                    // Add the playlist to Firestore
                                    playlistCollection.document(playlistId)
                                            .set(playlist)
                                            .addOnSuccessListener(aVoid -> {
                                                // Playlist added successfully
                                                playlistList.add(playlist);
                                                playlistAdapter.notifyDataSetChanged();
                                                playlistNameEditText.setText("");
                                                playlistDescriptionEditText.setText("");
                                                imageViewPlaylist.setImageResource(R.drawable.ic_launcher_foreground); // Reset the image view
                                                Toast.makeText(MainActivity.this, "Playlist added", Toast.LENGTH_SHORT).show();
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(MainActivity.this, "Error adding playlist: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    // Error occurred while getting the image URL
                                    Toast.makeText(MainActivity.this, "Error getting image URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        // Error occurred while uploading the image
                        Toast.makeText(MainActivity.this, "Error uploading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Add the playlist to Firestore without an image
            playlistCollection.document(playlistId)
                    .set(playlist)
                    .addOnSuccessListener(aVoid -> {
                        // Playlist added successfully
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
                    String imageUrl = documentSnapshot.getString("imageUrl"); // Assuming the image URL is stored in the "imageUrl" field
                    Playlist playlist = new Playlist(playlistName, playlistDescription);
                    playlist.setId(playlistId);
                    playlist.setImageUrl(imageUrl);
                    playlistList.add(playlist);
                }
                playlistAdapter.notifyDataSetChanged();

                // Load the playlist images
                for (int i = 0; i < playlistList.size(); i++) {
                    final int position = i;
                    Playlist playlist = playlistList.get(position);

                    // Load the playlist image using Glide library
                    if (playlist.getImageUrl() != null) {
                        Glide.with(MainActivity.this)
                                .load(playlist.getImageUrl())
                                .placeholder(R.drawable.ic_launcher_foreground) // Placeholder image while loading
                                .error(R.drawable.ic_launcher_foreground) // Error image if loading fails
                                .into(new CustomTarget<Drawable>() {
                                    @Override
                                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                        // Set the loaded image to the playlist item
                                        playlistAdapter.setPlaylistImage(position, resource);
                                    }

                                    @Override
                                    public void onLoadCleared(@Nullable Drawable placeholder) {
                                        // Called when the image load is cleared
                                        // You can perform any necessary actions here
                                    }
                                });
                    }
                }
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

                    // Set the current playlist details in the EditText fields
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
                            final AlertDialog editDialog = builder.create();
                            String updatedName = editPlaylistNameEditText.getText().toString().trim();
                            String updatedDescription = editPlaylistDescriptionEditText.getText().toString().trim();


                            // Update the playlist details in Firestore
                            playlist.setName(updatedName);
                            playlist.setDescription(updatedDescription);
                            playlistCollection.document(playlist.getId())
                                    .update("name", updatedName, "description", updatedDescription)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            // Playlist updated successfully
                                            Toast.makeText(MainActivity.this, "Playlist updated", Toast.LENGTH_SHORT).show();
                                            editDialog.dismiss(); // Dismiss the edit dialog after successful update
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            // Error occurred while updating the playlist
                                            Toast.makeText(MainActivity.this, "Error updating playlist: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });

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


}
