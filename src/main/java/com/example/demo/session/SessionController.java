package com.example.demo.session;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    public ResponseEntity<Session> createSession(
            @RequestBody Session session) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sessionService.createSession(session));
    }

    @GetMapping
    public ResponseEntity<List<Session>> getSessions() {

        return ResponseEntity.ok(
                sessionService.getSessions()
        );
    }

    @GetMapping("/setlist/{setlistId}")
    public ResponseEntity<List<Session>>
    getSessionsBySetlistId(@PathVariable Integer setlistId) {

        return ResponseEntity.ok(
                sessionService.getSessionsBySetlistId(setlistId)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSession(
            @PathVariable Integer id,
            @RequestBody Session updatedSession) {

        Session session =
                sessionService.updateSession(id, updatedSession);

        if (session == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("수정 실패");
        }

        return ResponseEntity.ok(session);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteSession(
            @PathVariable Integer id) {

        return ResponseEntity.ok(
                sessionService.deleteSession(id)
        );
    }
}