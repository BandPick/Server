package com.example.demo.song;

import jakarta.persistence.*;

@Entity
@Table(name = "setlist")
public class Song {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title")
    private String title;

    @Column(name = "artist")
    private String artist;

    @Column(name = "song_title")
    private String songTitle;

    public Song() {
    }

    public Song(Long id, String title, String artist) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.songTitle = title;
    }

    public Long getId() {
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
        this.songTitle = title;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getSongTitle() {
        return songTitle;
    }

    public void setSongTitle(String songTitle) {
        this.songTitle = songTitle;
        this.title = songTitle;
    }
}
