package com.example.demo.teamform.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

public record TeamFormSaveRequest(
        @JsonAlias("teammates")
        String message,
        @NotNull Integer maxTeams,
        @NotNull @NotEmpty List<@Valid PositionRequest> positions,
        @NotNull List<@Valid ScheduleRequest> schedules
) {
    public record PositionRequest(
            @NotNull String position,
            @NotNull String level,
            @NotNull Integer priority
    ) {
    }

    public record ScheduleRequest(
            @NotNull String dayOfWeek,
            @NotNull String startTime
    ) {
    }
}
