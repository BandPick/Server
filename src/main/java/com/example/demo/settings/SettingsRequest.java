package com.example.demo.settings;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record SettingsRequest(
        @NotNull LocalDateTime deadline,
        @NotNull Integer minVocalSongs,
        @NotNull Integer minSessionSongs
) {
}
