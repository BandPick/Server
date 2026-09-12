package com.example.demo.teamform.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

public record TeamFormMemberResponse(
        long userId,
        String name,
        String code,
        @JsonAlias("teammates")
        String message,
        int maxTeams,
        String createdAt,
        List<TeamFormPositionResponse> positions,
        List<TeamFormScheduleResponse> schedules
) {
}
