package com.example.demo.teamform.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record TeamFormSaveRequest(
        @NotNull @NotEmpty List<@Valid PositionRequest> positions,
        String preferredTeammates,
        @NotNull List<@Valid AvailabilityRequest> availabilities
) {
    public record PositionRequest(
            @NotNull String position,
            @NotNull String proficiency
    ) {
    }

    public record AvailabilityRequest(
            @NotNull String availableFrom,
            @NotNull String availableTo
    ) {
    }
}
