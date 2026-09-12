package com.example.demo.teamform.dto;

public record TeamFormSaveResponse(
        int savedPositionCount,
        int savedAvailabilityCount,
        String message
) {
}
