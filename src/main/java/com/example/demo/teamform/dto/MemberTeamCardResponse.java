package com.example.demo.teamform.dto;

import java.util.List;

public record MemberTeamCardResponse(
        String name,
        boolean confirmed,
        List<String> myPositions,
        List<MemberTeamSlotResponse> slots
) {
}
