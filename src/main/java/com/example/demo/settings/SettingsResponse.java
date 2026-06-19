package com.example.demo.settings;

import java.time.LocalDateTime;

public record SettingsResponse(
        Long id,
        LocalDateTime deadline,
        Integer minVocalSongs,
        Integer minSessionSongs,
        LocalDateTime updateTime
) {
}
