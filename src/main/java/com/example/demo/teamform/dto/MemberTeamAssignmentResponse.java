package com.example.demo.teamform.dto;

import java.util.List;

public record MemberTeamAssignmentResponse(
        List<MemberTeamCardResponse> teams
) {
}
