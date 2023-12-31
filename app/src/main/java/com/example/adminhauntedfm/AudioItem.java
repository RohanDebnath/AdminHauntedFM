package com.example.adminhauntedfm;

public class AudioItem {
    private String id;
    private String audioName;
    private String audioDescription;
    private String audioFilePath;
    private String audioFileUrl;

    public AudioItem() {
        // Default constructor required for Firestore deserialization
    }

    public AudioItem(String audioName, String audioDescription, String audioFilePath) {
        this.audioName = audioName;
        this.audioDescription = audioDescription;
        this.audioFilePath = audioFilePath;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getAudioFileUrl() {
        return audioFileUrl;
    }

    public void setAudioFileUrl(String audioFileUrl) {
        this.audioFileUrl = audioFileUrl;
    }
}
