package com.example.demo.session;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SessionService {

    private final SessionRepository sessionRepository;

    public SessionService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    public Session createSession(Session session) {
        normalize(session);
        return sessionRepository.save(session);
    }

    public List<Session> getSessions() {
        return sessionRepository.findAll();
    }

    public List<Session> getSessionsBySetlistId(Long setlistId) {
        return sessionRepository.findBySetlistId(setlistId);
    }

    public Session updateSession(Long setlistId, String position, String extra, Session updatedSession) {
        SessionId sessionId = new SessionId(setlistId, position, normalizeExtra(extra));
        Session session = sessionRepository.findById(sessionId).orElse(null);

        if (session == null) {
            return null;
        }

        normalize(updatedSession);
        session.setSetlistId(updatedSession.getSetlistId());
        session.setPosition(updatedSession.getPosition());
        session.setExtra(updatedSession.getExtra());

        return sessionRepository.save(session);
    }

    public String deleteSession(Long setlistId, String position, String extra) {
        SessionId sessionId = new SessionId(setlistId, position, normalizeExtra(extra));

        if (!sessionRepository.existsById(sessionId)) {
            return "Session not found.";
        }

        sessionRepository.deleteById(sessionId);
        return "Deleted.";
    }

    private void normalize(Session session) {
        session.setExtra(normalizeExtra(session.getExtra()));
    }

    private String normalizeExtra(String extra) {
        return extra == null ? "" : extra;
    }
}
