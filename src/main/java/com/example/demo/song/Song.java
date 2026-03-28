package com.example.demo.song;

import com.example.demo.common.type.Position;

import java.util.List;

public class Song {

    private String songId;
    private String title;
    private String artist;
    private List<Position> requiredPositions;

    public Song() {
    }

    public Song(String songId, String title, String artist, List<Position> requiredPositions) {
        this.songId = songId;
        this.title = title;
        this.artist = artist;
        this.requiredPositions = requiredPositions;
    }

    public String getSongId() {
        return songId;
    }

    public void setSongId(String songId) {
        this.songId = songId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public List<Position> getRequiredPositions() {
        return requiredPositions;
    }

    public void setRequiredPositions(List<Position> requiredPositions) {
        this.requiredPositions = requiredPositions;
    }
}