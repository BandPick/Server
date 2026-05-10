package com.example.demo.session;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    public ResponseEntity<Session> createSession(@RequestBody Session session) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sessionService.createSession(session));
    }

    @GetMapping
    public ResponseEntity<List<Session>> getSessions() {
        return ResponseEntity.ok(sessionService.getSessions());
    }

    @GetMapping("/setlist/{setlistId}")
    public ResponseEntity<List<Session>> getSessionsBySetlistId(@PathVariable Long setlistId) {
        return ResponseEntity.ok(sessionService.getSessionsBySetlistId(setlistId));
    }

    @PutMapping("/{setlistId}/{position}")
    public ResponseEntity<?> updateSession(
            @PathVariable Long setlistId,
            @PathVariable String position,
            @RequestParam(defaultValue = "") String extra,
            @RequestBody Session updatedSession
    ) {
        Session session = sessionService.updateSession(setlistId, position, extra, updatedSession);

        if (session == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Session not found.");
        }

        return ResponseEntity.ok(session);
    }

    @DeleteMapping("/{setlistId}/{position}")
    public ResponseEntity<String> deleteSession(
            @PathVariable Long setlistId,
            @PathVariable String position,
            @RequestParam(defaultValue = "") String extra
    ) {
        return ResponseEntity.ok(sessionService.deleteSession(setlistId, position, extra));
    }
}
