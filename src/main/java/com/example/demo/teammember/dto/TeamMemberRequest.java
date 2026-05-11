package com.example.demo.teammember.dto;

import com.example.demo.teammember.TeamMember;

public record TeamMemberRequest(
        Integer teamId,
        Long userId,
        Integer sessionId,
        String sessionPosition,
        String sessionExtra,
        String participantNumber
) {
    public TeamMember toEntity() {
        TeamMember teamMember = new TeamMember();
        teamMember.setTeamId(teamId);
        teamMember.setUserId(userId);
        teamMember.setSessionId(sessionId);
        teamMember.setSessionPosition(sessionPosition);
        teamMember.setSessionExtra(sessionExtra);
        teamMember.setParticipantNumber(participantNumber);
        return teamMember;
    }
}
