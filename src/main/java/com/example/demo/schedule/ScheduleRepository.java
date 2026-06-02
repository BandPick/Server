package com.example.demo.schedule;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduleRepository
        extends JpaRepository<Schedule, Integer> {

    List<Schedule> findByTeamId(Integer teamId);
}