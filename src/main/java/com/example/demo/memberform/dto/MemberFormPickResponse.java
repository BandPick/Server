package com.example.demo.memberform.dto;

public record MemberFormPickResponse(
        int priority,
        String songTitle,
        String session,
        long setlistId
) {
}