package com.example.demo.algorithm;

import java.util.List;

public record TeamMatchResultResponse(
        String song,
        String artist,
        String status,
        List<TeamMatchMemberResponse> members
) {
}