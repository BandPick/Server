package com.example.demo.teamform.dto;

public record TeamSystemUnmatchedResponse(
        long userId,
        String name,
        String reason
) {
}
