package com.example.demo.memberform.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record MemberFormMatrixSaveRequest(
        @NotNull List<@Valid MatrixPickRequest> picks
) {
    public record MatrixPickRequest(
            @NotNull Long userId,
            @NotNull Integer priority,
            @NotNull Long setlistId,
            @NotNull String desiredPosition,
            String desiredExtra
    ) {
    }
}
