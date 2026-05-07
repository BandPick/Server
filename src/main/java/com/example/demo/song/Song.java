package com.example.demo.song;

import jakarta.persistence.*;

@Entity
@Table(name = "setlist")
public class Song {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "song_title")
    private String title;

    @Column(name = "artist")
    private String artist;

    public Song() {
    }

    public Song(Integer id, String title, String artist) {
        this.id = id;
        this.title = title;
        this.artist = artist;
    }

    public Integer getId() {
        return id;
    }

    public String getSongId() {
        return String.valueOf(id);
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }
}