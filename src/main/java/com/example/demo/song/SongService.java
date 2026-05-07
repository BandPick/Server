package com.example.demo.song;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SongService {

    private final SongRepository songRepository;

    public SongService(SongRepository songRepository) {
        this.songRepository = songRepository;
    }

    public Song createSong(Song song) {
        return songRepository.save(song);
    }

    public List<Song> getSongs() {
        return songRepository.findAll();
    }

    public Song getSongById(String songId) {
        try {
            Integer id = Integer.parseInt(songId);
            return songRepository.findById(id).orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Song updateSong(String songId, Song updatedSong) {
        Song song = getSongById(songId);

        if (song == null) {
            return null;
        }

        song.setTitle(updatedSong.getTitle());
        song.setArtist(updatedSong.getArtist());

        return songRepository.save(song);
    }

    public String deleteSong(String songId) {
        Song song = getSongById(songId);

        if (song == null) {
            return "해당 곡을 찾을 수 없습니다.";
        }

        songRepository.delete(song);
        return "삭제 완료";
    }
}