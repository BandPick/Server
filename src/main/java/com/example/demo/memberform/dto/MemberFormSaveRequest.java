package com.example.demo.memberform.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record MemberFormSaveRequest(
        @NotNull Long userId,
        @NotEmpty List<@Valid PickRequest> picks,
        @NotEmpty List<@Valid AvailabilityRequest> availabilities
) {
    public record PickRequest(
            @NotNull Integer priority,
            @NotNull Long setlistId,
            @NotNull String desiredPosition,
            String desiredExtra
    ) {
    }

    public record AvailabilityRequest(
            @NotNull String availableFrom,
            @NotNull String availableTo
    ) {
    }
}
