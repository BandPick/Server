package com.example.demo.song;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
public class SongService {

    private final List<Song> songList = new ArrayList<>();
    private int songSequence = 1;

    public Song createSong(Song song) {
        String newSongId = "S" + String.format("%03d", songSequence++);
        song.setSongId(newSongId);
        songList.add(song);
        return song;
    }

    public List<Song> getSongs() {
        return songList;
    }

    public Song getSongById(String songId) {
        for (Song song : songList) {
            if (song.getSongId().equals(songId)) {
                return song;
            }
        }
        return null;
    }

    public Song updateSong(String songId, Song updatedSong) {
        for (Song song : songList) {
            if (song.getSongId().equals(songId)) {
                song.setTitle(updatedSong.getTitle());
                song.setArtist(updatedSong.getArtist());
                song.setRequiredPositions(updatedSong.getRequiredPositions());
                return song;
            }
        }
        return null;
    }

    public String deleteSong(String songId) {
        Iterator<Song> iterator = songList.iterator();

        while (iterator.hasNext()) {
            Song song = iterator.next();
            if (song.getSongId().equals(songId)) {
                iterator.remove();
                return "삭제 완료";
            }
        }

        return "해당 곡을 찾을 수 없습니다.";
    }
}