package com.example.demo.teamform.dto;

import java.util.List;

public record TeamSystemMatchResponse(
        List<TeamSystemTeamResponse> teams,
        List<TeamSystemUnmatchedResponse> unmatched
) {
}
