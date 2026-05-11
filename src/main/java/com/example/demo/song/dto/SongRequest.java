package com.example.demo.song.dto;

import com.example.demo.song.Song;

public record SongRequest(
        String title,
        String artist,
        String songTitle
) {
    public Song toEntity() {
        Song song = new Song();
        if (songTitle != null) {
            song.setSongTitle(songTitle);
        } else {
            song.setTitle(title);
        }
        song.setArtist(artist);
        return song;
    }
}
