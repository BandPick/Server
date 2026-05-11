package com.example.demo.schedule.dto;

import com.example.demo.schedule.Schedule;

import java.time.LocalDateTime;

public record ScheduleResponse(
        Integer id,
        Integer teamId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        LocalDateTime availableFrom,
        LocalDateTime availableTo
) {
    public static ScheduleResponse from(Schedule schedule) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getTeamId(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getAvailableFrom(),
                schedule.getAvailableTo()
        );
    }
}
