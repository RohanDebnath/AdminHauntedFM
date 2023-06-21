package com.example.adminhauntedfm;

public class AudioItem {
    private String audioName;
    private String audioDescription;
    private String audioFilePath;

    public AudioItem() {
        // Default constructor required for Firestore deserialization
    }

    public AudioItem(String audioName, String audioDescription, String audioFilePath) {
        this.audioName = audioName;
        this.audioDescription = audioDescription;
        this.audioFilePath = audioFilePath;
    }

    public String getAudioName() {
        return audioName;
    }

    public void setAudioName(String audioName) {
        this.audioName = audioName;
    }

    public String getAudioDescription() {
        return audioDescription;
    }

    public void setAudioDescription(String audioDescription) {
        this.audioDescription = audioDescription;
    }

    public String getAudioFilePath() {
        return audioFilePath;
    }

    public void setAudioFilePath(String audioFilePath) {
        this.audioFilePath = audioFilePath;
    }
}
