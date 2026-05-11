package com.example.demo.teammember.dto;

import com.example.demo.teammember.TeamMember;

public record TeamMemberResponse(
        Integer id,
        Integer teamId,
        Long userId,
        Integer sessionId,
        String sessionPosition,
        String sessionExtra
) {
    public static TeamMemberResponse from(TeamMember teamMember) {
        return new TeamMemberResponse(
                teamMember.getId(),
                teamMember.getTeamId(),
                teamMember.getUserId(),
                teamMember.getSessionId(),
                teamMember.getSessionPosition(),
                teamMember.getSessionExtra()
        );
    }
}
