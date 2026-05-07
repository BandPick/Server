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

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(songService.createSong(song));
    }

    @GetMapping
    public ResponseEntity<List<Song>> getSongs() {

        return ResponseEntity.ok(
                songService.getSongs()
        );
    }

    @GetMapping("/{songId}")
    public ResponseEntity<?> getSongById(
            @PathVariable String songId) {

        Song song = songService.getSongById(songId);

        if (song == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("해당 곡을 찾을 수 없습니다.");
        }

        return ResponseEntity.ok(song);
    }

    @PutMapping("/{songId}")
    public ResponseEntity<?> updateSong(
            @PathVariable String songId,
            @RequestBody Song updatedSong) {

        Song song =
                songService.updateSong(songId, updatedSong);

        if (song == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("해당 곡을 찾을 수 없습니다.");
        }

        return ResponseEntity.ok(song);
    }

    @DeleteMapping("/{songId}")
    public ResponseEntity<String> deleteSong(
            @PathVariable String songId) {

        return ResponseEntity.ok(
                songService.deleteSong(songId)
        );
    }
}