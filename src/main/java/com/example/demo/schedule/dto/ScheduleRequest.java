package com.example.demo.schedule.dto;

import com.example.demo.schedule.Schedule;

import java.time.LocalDateTime;

public record ScheduleRequest(
        Integer teamId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        LocalDateTime availableFrom,
        LocalDateTime availableTo
) {
    public Schedule toEntity() {
        Schedule schedule = new Schedule();
        schedule.setTeamId(teamId);
        if (availableFrom != null) {
            schedule.setAvailableFrom(availableFrom);
        } else {
            schedule.setStartTime(startTime);
        }
        if (availableTo != null) {
            schedule.setAvailableTo(availableTo);
        } else {
            schedule.setEndTime(endTime);
        }
        return schedule;
    }
}
