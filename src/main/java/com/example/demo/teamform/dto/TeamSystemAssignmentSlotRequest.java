package com.example.demo.teamform.dto;

public record TeamSystemAssignmentSlotRequest(
        String position,
        Long userId,
        Boolean needed
) {
}