package com.example.demo.teamform.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record TeamFormSaveRequest(
        String teammates,
        @NotNull Integer maxTeams,
        @NotNull @NotEmpty List<@Valid PositionRequest> positions,
        @NotNull List<@Valid ScheduleRequest> schedules
) {
    public record PositionRequest(
            @NotNull String position,
            @NotNull String level
    ) {
    }

    public record ScheduleRequest(
            @NotNull String dayOfWeek,
            @NotNull String startTime
    ) {
    }
}
