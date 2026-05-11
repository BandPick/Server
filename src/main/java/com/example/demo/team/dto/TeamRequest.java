package com.example.demo.team.dto;

import com.example.demo.team.Team;

public record TeamRequest(
        Integer setlistId
) {
    public Team toEntity() {
        Team team = new Team();
        team.setSetlistId(setlistId);
        return team;
    }
}
