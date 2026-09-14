package com.example.demo.teamform.dto;

import java.util.List;

public record TeamSystemScheduleTeamResponse(
        Integer id,
        String name,
        boolean confirmed,
        List<String> members
) {
}
