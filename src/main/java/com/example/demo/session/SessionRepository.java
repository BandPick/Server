package com.example.demo.session;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionRepository
        extends JpaRepository<Session, Integer> {

    List<Session> findBySetlistId(Integer setlistId);
}