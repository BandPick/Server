package com.example.demo.teamform.vo;

import java.time.LocalDateTime;

public record TeamFormHeaderRow(
        int id,
        long userId,
        String userName,
        String userCode,
        String teammates,
        int maxTeams,
        LocalDateTime createdAt
) {
}
