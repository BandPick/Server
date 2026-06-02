package com.example.demo.song;

import com.example.demo.song.dto.SongRequest;
import com.example.demo.song.dto.SongResponse;
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
    public ResponseEntity<SongResponse> createSong(@RequestBody SongRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SongResponse.from(songService.createSong(request.toEntity())));
    }

    @GetMapping
    public ResponseEntity<List<SongResponse>> getSongs() {

        return ResponseEntity.ok(
                songService.getSongs().stream()
                        .map(SongResponse::from)
                        .toList()
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

        return ResponseEntity.ok(SongResponse.from(song));
    }

    @PutMapping("/{songId}")
    public ResponseEntity<?> updateSong(
            @PathVariable String songId,
            @RequestBody SongRequest request) {

        Song song =
                songService.updateSong(songId, request.toEntity());

        if (song == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("해당 곡을 찾을 수 없습니다.");
        }

        return ResponseEntity.ok(SongResponse.from(song));
    }

    @DeleteMapping("/{songId}")
    public ResponseEntity<String> deleteSong(
            @PathVariable String songId) {

        return ResponseEntity.ok(
                songService.deleteSong(songId)
        );
    }
}
