package com.example.demo.teamform.dto;

public record TeamFormSaveResponse(
        int savedPositionCount,
        int savedScheduleCount,
        String message
) {
}
