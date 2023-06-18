package com.example.adminhauntedfm;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private CollectionReference playlistCollection;
    private EditText playlistNameEditText;
    private EditText playlistDescriptionEditText;
    private ListView playlistListView;
    private ArrayAdapter<String> playlistAdapter;
    private ArrayList<String> playlistNames;
    private ArrayList<String> playlistIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        playlistCollection = db.collection("playlists");

        playlistNameEditText = findViewById(R.id.playlistNameEditText);
        playlistDescriptionEditText = findViewById(R.id.playlistDescriptionEditText);
        playlistListView = findViewById(R.id.playlistListView);

        Button addButton = findViewById(R.id.addButton);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addPlaylist();
            }
        });

        // Initialize playlist data
        playlistNames = new ArrayList<>();
        playlistIds = new ArrayList<>();
        playlistAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, playlistNames);
        playlistListView.setAdapter(playlistAdapter);

        // Load existing playlists
        loadPlaylists();

        // Set click listeners for list items
        playlistListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                String playlistId = playlistIds.get(position);
                String playlistName = playlistNames.get(position);

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Playlist Options");
                builder.setMessage("Playlist: " + playlistName);
                builder.setPositiveButton("Edit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        editPlaylist(playlistId, playlistName);
                        Toast.makeText(MainActivity.this, "Edit clicked for Playlist: " + playlistName, Toast.LENGTH_SHORT).show();
                    }
                });
                builder.setNegativeButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AlertDialog.Builder deleteConfirmationBuilder = new AlertDialog.Builder(MainActivity.this);
                        deleteConfirmationBuilder.setTitle("Delete Playlist");
                        deleteConfirmationBuilder.setMessage("Are you sure you want to delete the playlist '" + playlistName + "'?");
                        deleteConfirmationBuilder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deletePlaylist(playlistId);
                            }
                        });
                        deleteConfirmationBuilder.setNegativeButton("Cancel", null);
                        deleteConfirmationBuilder.show();
                    }
                });
                builder.show();

                return true;
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
        Map<String, Object> playlistData = new HashMap<>();
        playlistData.put("name", name);
        playlistData.put("description", description);

        // Add the playlist to Firestore
        playlistCollection.add(playlistData)
                .addOnSuccessListener(documentReference -> {
                    // Playlist added successfully
                    String playlistId = documentReference.getId();
                    playlistIds.add(playlistId);
                    playlistNames.add(name);
                    playlistAdapter.notifyDataSetChanged();
                    playlistNameEditText.setText("");
                    playlistDescriptionEditText.setText("");
                    Toast.makeText(MainActivity.this, "Playlist added", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Error adding playlist: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void deletePlaylist(String playlistId) {
        // Delete the playlist from Firestore
        playlistCollection.document(playlistId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Playlist deleted successfully
                    int index = playlistIds.indexOf(playlistId);
                    playlistIds.remove(index);
                    playlistNames.remove(index);
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
                    playlistNames.clear();
                    playlistIds.clear();
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        String playlistId = documentSnapshot.getId();
                        String playlistName = documentSnapshot.getString("name");
                        playlistIds.add(playlistId);
                        playlistNames.add(playlistName);
                    }
                    playlistAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Error loading playlists: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void editPlaylist(String playlistId, String playlistName) {
        playlistCollection.document(playlistId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    // Get the playlist data
                    String name = documentSnapshot.getString("name");
                    String description = documentSnapshot.getString("description");

                    // Create the edit dialog
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Edit Playlist");

                    // Initialize the dialog views
                    final EditText editPlaylistNameEditText = new EditText(MainActivity.this);
                    editPlaylistNameEditText.setText(name);
                    final EditText editPlaylistDescriptionEditText = new EditText(MainActivity.this);
                    editPlaylistDescriptionEditText.setText(description);

                    LinearLayout dialogLayout = new LinearLayout(MainActivity.this);
                    dialogLayout.setOrientation(LinearLayout.VERTICAL);
                    dialogLayout.addView(editPlaylistNameEditText);
                    dialogLayout.addView(editPlaylistDescriptionEditText);
                    builder.setView(dialogLayout);

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
                            updatePlaylist(playlistId, updatedName, updatedDescription);
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

    private void updatePlaylist(String playlistId, String updatedName, String updatedDescription) {
        // Update the playlist details in Firestore
        playlistCollection.document(playlistId)
                .update("name", updatedName, "description", updatedDescription)
                .addOnSuccessListener(aVoid -> {
                    // Playlist updated successfully
                    Toast.makeText(MainActivity.this, "Playlist updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    // Error occurred while updating the playlist
                    Toast.makeText(MainActivity.this, "Error updating playlist: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }



}
