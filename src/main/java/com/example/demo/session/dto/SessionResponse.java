package com.example.demo.session.dto;

import com.example.demo.session.Session;

public record SessionResponse(
        Integer id,
        Long setlistId,
        String position,
        String extra
) {
    public static SessionResponse from(Session session) {
        return new SessionResponse(
                session.getId(),
                session.getSetlistId(),
                session.getPosition(),
                session.getExtra()
        );
    }
}
