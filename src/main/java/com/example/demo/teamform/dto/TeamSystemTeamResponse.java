package com.example.demo.teamform.dto;

import java.util.List;

public record TeamSystemTeamResponse(
        String name,
        String status,
        String note,
        List<TeamSystemTeamMemberResponse> members
) {
}
