package com.example.demo.teamform.dto;

public record MemberTeamSlotResponse(
        String position,
        boolean needed,
        Long userId,
        String name,
        String level,
        boolean me
) {
}
