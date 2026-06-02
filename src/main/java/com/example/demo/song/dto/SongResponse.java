package com.example.demo.song.dto;

import com.example.demo.song.Song;

public record SongResponse(
        Long id,
        String songId,
        String title,
        String artist,
        String songTitle
) {
    public static SongResponse from(Song song) {
        return new SongResponse(
                song.getId(),
                song.getSongId(),
                song.getTitle(),
                song.getArtist(),
                song.getSongTitle()
        );
    }
}
