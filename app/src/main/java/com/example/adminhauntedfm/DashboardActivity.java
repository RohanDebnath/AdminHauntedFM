package com.example.adminhauntedfm;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            }

            @Override
            public void onDeleteClick(AudioItem audioItem) {
                // Handle delete click event
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
        // Get the selected playlist document ID
        firebaseFirestore.collection("playlists")
                .whereEqualTo("name", selectedPlaylist)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                            String playlistId = queryDocumentSnapshots.getDocuments().get(0).getId();

                            // Create a new AudioItem object with the given name and description
                            AudioItem audioItem = new AudioItem(audioName, audioDescription, selectedAudioFilePath);

                            // Upload the audio item to the selected playlist
                            firebaseFirestore.collection("playlists")
                                    .document(playlistId)
                                    .collection("audioFiles")
                                    .add(audioItem)
                                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                        @Override
                                        public void onSuccess(DocumentReference documentReference) {
                                            Toast.makeText(DashboardActivity.this, "Audio uploaded successfully", Toast.LENGTH_SHORT).show();
                                            audioNameEditText.setText("");
                                            audioDescriptionEditText.setText("");
                                            selectedAudioFilePath = null;
                                            TextView selectedAudioTextView = findViewById(R.id.selectedAudioTextView);
                                            selectedAudioTextView.setText("");
                                            loadAudioItems();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(DashboardActivity.this, "Failed to upload audio", Toast.LENGTH_SHORT).show();
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
}
