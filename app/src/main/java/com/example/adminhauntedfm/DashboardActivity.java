package com.example.adminhauntedfm;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DashboardActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private static final int REQUEST_CODE_AUDIO = 1;
    private Spinner playlistSpinner;
    private EditText audioNameEditText, audioDescriptionEditText;
    private Button selectAudioButton, uploadButton;
    private RecyclerView recyclerView;

    private FirebaseFirestore firebaseFirestore;
    private List<AudioItem> audioItems;
    private AudioAdapter audioAdapter;
    private List<String> playlistNames;
    private ArrayAdapter<String> spinnerAdapter;

    private String selectedPlaylist;
    private String selectedAudioFilePath;

    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Initialize views
        playlistSpinner = findViewById(R.id.playlistSpinner);
        audioNameEditText = findViewById(R.id.audioNameEditText);
        audioDescriptionEditText = findViewById(R.id.audioDescriptionEditText);
        selectAudioButton = findViewById(R.id.selectAudioButton);
        uploadButton = findViewById(R.id.uploadAudioButton);
        recyclerView = findViewById(R.id.audioRecyclerView);
        progressBar=findViewById(R.id.progressbar2);

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        audioItems = new ArrayList<>();
        audioAdapter = new AudioAdapter(this, audioItems, new AudioAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(AudioItem audioItem) {
                // Handle item click event
            }

            @Override
            public void onEditClick(AudioItem audioItem) {
                // Handle edit click event
                editAudioItem(audioItem);
            }

            @Override
            public void onDeleteClick(AudioItem audioItem) {
                // Handle delete click event
                deleteAudioItem(audioItem);
            }
        });
        recyclerView.setAdapter(audioAdapter);

        // Set up Firebase Firestore reference
        firebaseFirestore = FirebaseFirestore.getInstance();

        // Initialize playlist names list and spinner adapter
        playlistNames = new ArrayList<>();
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, playlistNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        playlistSpinner.setAdapter(spinnerAdapter);
        playlistSpinner.setOnItemSelectedListener(this);

        // Set up click listener for select audio button
        selectAudioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open file picker to select an audio file
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("audio/*");
                startActivityForResult(intent, REQUEST_CODE_AUDIO);
            }
        });

        // Set up click listener for upload button
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle upload button click
                String audioName = audioNameEditText.getText().toString().trim();
                String audioDescription = audioDescriptionEditText.getText().toString().trim();

                if (selectedPlaylist.isEmpty()) {
                    Toast.makeText(DashboardActivity.this, "Please select a playlist", Toast.LENGTH_SHORT).show();
                } else if (audioName.isEmpty()) {
                    Toast.makeText(DashboardActivity.this, "Please enter audio name", Toast.LENGTH_SHORT).show();
                } else if (audioDescription.isEmpty()) {
                    Toast.makeText(DashboardActivity.this, "Please enter audio description", Toast.LENGTH_SHORT).show();
                } else if (selectedAudioFilePath == null) {
                    Toast.makeText(DashboardActivity.this, "Please select an audio file", Toast.LENGTH_SHORT).show();
                } else {
                    uploadAudio(audioName, audioDescription);
                }
            }
        });

        // Load playlist names from Firestore
        loadPlaylistNames();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_AUDIO && resultCode == RESULT_OK && data != null) {
            // Get the selected audio file path
            Uri audioUri = data.getData();
            selectedAudioFilePath = getFilePathFromUri(audioUri);
            // For example, display the file path in a TextView
            TextView selectedAudioTextView = findViewById(R.id.selectedAudioTextView);
            selectedAudioTextView.setText(selectedAudioFilePath);
        }
    }

    private void loadPlaylistNames() {
        firebaseFirestore.collection("playlists")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (queryDocumentSnapshots != null) {
                            playlistNames.clear();
                            for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                                String playlistName = documentSnapshot.getString("name");
                                playlistNames.add(playlistName);
                            }
                            spinnerAdapter.notifyDataSetChanged();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(DashboardActivity.this, "Failed to load playlist names", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void uploadAudio(String audioName, String audioDescription) {
        progressBar.setVisibility(View.VISIBLE);

        // Create a reference to the Firebase Storage
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();

        // Create a reference to the audio file in Firebase Storage
        StorageReference audioFileRef = storageRef.child("audioFiles/" + UUID.randomUUID().toString() + ".mp3");

        // Get the URI of the selected audio file
        Uri audioFileUri = Uri.fromFile(new File(selectedAudioFilePath));

        // Upload the audio file to Firebase Storage
        audioFileRef.putFile(audioFileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get the download URL of the uploaded audio file
                    audioFileRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                // Create a new AudioItem object with the given name, description, and file URL
                                AudioItem audioItem = new AudioItem(audioName, audioDescription, selectedAudioFilePath);
                                audioItem.setAudioFileUrl(uri.toString());

                                // Get the selected playlist document ID
                                firebaseFirestore.collection("playlists")
                                        .whereEqualTo("name", selectedPlaylist)
                                        .get()
                                        .addOnSuccessListener(queryDocumentSnapshots -> {
                                            if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                                                String playlistId = queryDocumentSnapshots.getDocuments().get(0).getId();

                                                // Generate a unique ID for the audio item
                                                String audioItemId = firebaseFirestore.collection("playlists")
                                                        .document(playlistId)
                                                        .collection("audioFiles")
                                                        .document().getId();

                                                // Set the generated ID for the audio item
                                                audioItem.setId(audioItemId);

                                                // Upload the audio item to the selected playlist
                                                firebaseFirestore.collection("playlists")
                                                        .document(playlistId)
                                                        .collection("audioFiles")
                                                        .document(audioItemId)
                                                        .set(audioItem)
                                                        .addOnSuccessListener(aVoid -> {
                                                            Toast.makeText(DashboardActivity.this, "Audio uploaded successfully", Toast.LENGTH_SHORT).show();
                                                            audioNameEditText.setText("");
                                                            audioDescriptionEditText.setText("");
                                                            selectedAudioFilePath = null;
                                                            TextView selectedAudioTextView = findViewById(R.id.selectedAudioTextView);
                                                            selectedAudioTextView.setText("");
                                                            loadAudioItems();
                                                            progressBar.setVisibility(View.GONE);
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            Toast.makeText(DashboardActivity.this, "Failed to upload audio", Toast.LENGTH_SHORT).show();
                                                            progressBar.setVisibility(View.GONE);
                                                        });
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(DashboardActivity.this, "Failed to get playlist ID", Toast.LENGTH_SHORT).show();
                                            progressBar.setVisibility(View.GONE);
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(DashboardActivity.this, "Failed to get audio file URL", Toast.LENGTH_SHORT).show();
                                progressBar.setVisibility(View.GONE);
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(DashboardActivity.this, "Failed to upload audio file", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void loadAudioItems() {
        // Clear the existing audio items list
        audioItems.clear();

        // Get the selected playlist document ID
        firebaseFirestore.collection("playlists")
                .whereEqualTo("name", selectedPlaylist)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                            String playlistId = queryDocumentSnapshots.getDocuments().get(0).getId();

                            // Load audio items from the selected playlist
                            firebaseFirestore.collection("playlists")
                                    .document(playlistId)
                                    .collection("audioFiles")
                                    .get()
                                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                        @Override
                                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                            if (queryDocumentSnapshots != null) {
                                                for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                                                    AudioItem audioItem = documentSnapshot.toObject(AudioItem.class);
                                                    audioItems.add(audioItem);
                                                }
                                                audioAdapter.notifyDataSetChanged();
                                            }
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(DashboardActivity.this, "Failed to load audio items", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(DashboardActivity.this, "Failed to get playlist ID", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        selectedPlaylist = playlistNames.get(position);
        loadAudioItems();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Do nothing
    }

    private String getFilePathFromUri(Uri uri) {
        String filePath;
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor == null) {
            filePath = uri.getPath();
        } else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            filePath = cursor.getString(index);
            cursor.close();
        }
        return filePath;
    }
    private void deleteAudioItem(AudioItem audioItem) {
        // Delete the selected audio item from Firestore
        firebaseFirestore.collection("playlists")
                .whereEqualTo("name", selectedPlaylist)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                            String playlistId = queryDocumentSnapshots.getDocuments().get(0).getId();

                            firebaseFirestore.collection("playlists")
                                    .document(playlistId)
                                    .collection("audioFiles")
                                    .document(audioItem.getId())
                                    .delete()
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Toast.makeText(DashboardActivity.this, "Audio deleted successfully", Toast.LENGTH_SHORT).show();
                                            loadAudioItems();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(DashboardActivity.this, "Failed to delete audio", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(DashboardActivity.this, "Failed to get playlist ID", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void editAudioItem(AudioItem audioItem) {
        AlertDialog.Builder builder = new AlertDialog.Builder(DashboardActivity.this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_audio, null);
        EditText editTitleEditText = dialogView.findViewById(R.id.editTitleEditText);
        EditText editDescriptionEditText = dialogView.findViewById(R.id.editDescriptionEditText);
        Button updateButton = dialogView.findViewById(R.id.updateButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        // Set the current title and description in the edit text fields
        editTitleEditText.setText(audioItem.getAudioName());
        editDescriptionEditText.setText(audioItem.getAudioDescription());

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        // Update button click listener
        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newTitle = editTitleEditText.getText().toString().trim();
                String newDescription = editDescriptionEditText.getText().toString().trim();

                if (TextUtils.isEmpty(newTitle)) {
                    editTitleEditText.setError("Please enter a title");
                    return;
                }

                if (TextUtils.isEmpty(newDescription)) {
                    editDescriptionEditText.setError("Please enter a description");
                    return;
                }

                // Update the title and description of the audio item
                audioItem.setAudioName(newTitle);
                audioItem.setAudioDescription(newDescription);

                // Save the updated audio item to Firestore
                saveAudioItem(audioItem);

                dialog.dismiss();
            }
        });

        // Cancel button click listener
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }
    private void saveAudioItem(AudioItem audioItem) {
        // Get the selected playlist document ID
        firebaseFirestore.collection("playlists")
                .whereEqualTo("name", selectedPlaylist)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                            String playlistId = queryDocumentSnapshots.getDocuments().get(0).getId();

                            // Update the audio item in Firestore
                            firebaseFirestore.collection("playlists")
                                    .document(playlistId)
                                    .collection("audioFiles")
                                    .document(audioItem.getId())
                                    .set(audioItem)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Toast.makeText(DashboardActivity.this, "Audio updated successfully", Toast.LENGTH_SHORT).show();
                                            loadAudioItems();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(DashboardActivity.this, "Failed to update audio", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(DashboardActivity.this, "Failed to get playlist ID", Toast.LENGTH_SHORT).show();
                    }
                });
    }

}
