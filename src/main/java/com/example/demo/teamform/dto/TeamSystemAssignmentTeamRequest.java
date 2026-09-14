package com.example.demo.teamform.dto;

import java.util.List;

public record TeamSystemAssignmentTeamRequest(
        String name,
        Boolean confirmed,
        List<TeamSystemAssignmentSlotRequest> slots
) {
}
