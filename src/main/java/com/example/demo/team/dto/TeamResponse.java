package com.example.demo.team.dto;

import com.example.demo.team.Team;

public record TeamResponse(
        Integer id,
        String teamId,
        Integer setlistId
) {
    public static TeamResponse from(Team team) {
        return new TeamResponse(
                team.getId(),
                team.getTeamId(),
                team.getSetlistId()
        );
    }
}
