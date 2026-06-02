package com.example.demo.session.dto;

import com.example.demo.session.Session;

public record SessionRequest(
        Long setlistId,
        String position,
        String extra
) {
    public Session toEntity() {
        Session session = new Session();
        session.setSetlistId(setlistId);
        session.setPosition(position);
        session.setExtra(extra);
        return session;
    }
}
