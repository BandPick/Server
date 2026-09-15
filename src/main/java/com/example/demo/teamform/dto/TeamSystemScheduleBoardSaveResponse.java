package com.example.demo.teamform.dto;

public record TeamSystemScheduleBoardSaveResponse(
        int teamCount,
        int eventCount,
        String message,
        TeamSystemScheduleBoardResponse board
) {
}
