package com.example.demo.teamform.dto;

public record TeamSystemTeamMemberResponse(
        long userId,
        String session,
        String name,
        String level
) {
}
