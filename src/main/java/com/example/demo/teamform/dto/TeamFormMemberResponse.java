package com.example.demo.teamform.dto;

import java.util.List;

public record TeamFormMemberResponse(
        long userId,
        String name,
        String code,
        String teammates,
        int maxTeams,
        String createdAt,
        List<TeamFormPositionResponse> positions,
        List<TeamFormScheduleResponse> schedules
) {
}
