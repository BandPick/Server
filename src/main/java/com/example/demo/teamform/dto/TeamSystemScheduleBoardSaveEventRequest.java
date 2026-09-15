package com.example.demo.teamform.dto;

public record TeamSystemScheduleBoardSaveEventRequest(
        Integer teamId,
        String day,
        String startTime,
        String endTime
) {
}
