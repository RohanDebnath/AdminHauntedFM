package com.example.adminhauntedfm;

public class Playlist {
    private String id;
    private String name;
    private String description;

    public Playlist() {
        // Default constructor required for Firestore serialization
    }

    public Playlist(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
