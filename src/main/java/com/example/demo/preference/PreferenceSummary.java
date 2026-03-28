package com.example.demo.preference;

public class PreferenceSummary {

    private String songId;
    private int count;

    public PreferenceSummary() {
    }

    public PreferenceSummary(String songId, int count) {
        this.songId = songId;
        this.count = count;
    }

    public String getSongId() {
        return songId;
    }

    public void setSongId(String songId) {
        this.songId = songId;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}