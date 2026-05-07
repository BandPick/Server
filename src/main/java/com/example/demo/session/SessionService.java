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

        return sessionRepository.save(session);
    }

    public List<Session> getSessions() {

        return sessionRepository.findAll();
    }

    public List<Session> getSessionsBySetlistId(Integer setlistId) {

        return sessionRepository.findBySetlistId(setlistId);
    }

    public Session updateSession(Integer id,
                                 Session updatedSession) {

        Session session =
                sessionRepository.findById(id).orElse(null);

        if (session == null) {
            return null;
        }

        session.setSetlistId(updatedSession.getSetlistId());
        session.setPosition(updatedSession.getPosition());

        return sessionRepository.save(session);
    }

    public String deleteSession(Integer id) {

        Session session =
                sessionRepository.findById(id).orElse(null);

        if (session == null) {
            return "해당 세션을 찾을 수 없습니다.";
        }

        sessionRepository.delete(session);

        return "삭제 완료";
    }
}