package com.example.demo.teamform.dto;

import java.util.List;

public record TeamSystemScheduleBoardResponse(
        List<TeamSystemScheduleTeamResponse> teams,
        List<TeamSystemScheduleEventResponse> events
) {
}
