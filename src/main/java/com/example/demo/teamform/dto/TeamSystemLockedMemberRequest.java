package com.example.demo.teamform.dto;

public record TeamSystemLockedMemberRequest(
        Long userId,
        String session
) {
}
