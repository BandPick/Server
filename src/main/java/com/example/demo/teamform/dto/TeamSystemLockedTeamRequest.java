package com.example.demo.teamform.dto;

import java.util.List;

public record TeamSystemLockedTeamRequest(
        String name,
        List<TeamSystemLockedMemberRequest> members
) {
}
