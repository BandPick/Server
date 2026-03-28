package com.example.demo.song;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/songs")
public class SongController {

    private final SongService songService;

    public SongController(SongService songService) {
        this.songService = songService;
    }

    @PostMapping
    public ResponseEntity<Song> createSong(@RequestBody Song song) {
        Song createdSong = songService.createSong(song);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdSong);
    }

    @GetMapping
    public ResponseEntity<List<Song>> getSongs() {
        return ResponseEntity.ok(songService.getSongs());
    }

    @GetMapping("/{songId}")
    public ResponseEntity<Song> getSongById(@PathVariable String songId) {
        Song song = songService.getSongById(songId);

        if (song == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(song);
    }

    @PutMapping("/{songId}")
    public ResponseEntity<Song> updateSong(@PathVariable String songId,
                                           @RequestBody Song updatedSong) {
        Song song = songService.updateSong(songId, updatedSong);

        if (song == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(song);
    }

    @DeleteMapping("/{songId}")
    public ResponseEntity<String> deleteSong(@PathVariable String songId) {
        String result = songService.deleteSong(songId);

        if (result.equals("해당 곡을 찾을 수 없습니다.")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }

        return ResponseEntity.ok(result);
    }
}