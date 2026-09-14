package com.example.demo.teamform.dto;

import java.util.List;

public record TeamSystemMatchRequest(
        List<TeamSystemLockedTeamRequest> lockedTeams
) {
}
