package com.example.demo.preference.dto;

import com.example.demo.preference.Preference;

public record PreferenceResponse(
        Integer id,
        Long userId,
        Integer priority,
        Integer detailId,
        Integer desiredSession,
        Integer setlistId,
        String desiredPosition,
        String desiredExtra
) {
    public static PreferenceResponse from(Preference preference) {
        return new PreferenceResponse(
                preference.getId(),
                preference.getUserId(),
                preference.getPriority(),
                preference.getDetailId(),
                preference.getDesiredSession(),
                preference.getSetlistId(),
                preference.getDesiredPosition(),
                preference.getDesiredExtra()
        );
    }
}
