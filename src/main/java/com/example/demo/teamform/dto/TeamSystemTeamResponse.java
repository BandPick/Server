package com.example.demo.teamform.dto;

import java.util.List;
import java.util.Map;

public record TeamSystemTeamResponse(
        String name,
        String status,
        String note,
        List<TeamSystemTeamMemberResponse> members,
        boolean confirmed,
        Map<String, Boolean> neededByPosition
) {
    public TeamSystemTeamResponse(
            String name,
            String status,
            String note,
            List<TeamSystemTeamMemberResponse> members
    ) {
        this(name, status, note, members, false, Map.of());
    }
}
