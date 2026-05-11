package com.example.demo.preference.dto;

import com.example.demo.preference.Preference;

public record PreferenceRequest(
        Long userId,
        Integer priority,
        Integer detailId,
        Integer desiredSession,
        Integer setlistId,
        String desiredPosition,
        String desiredExtra,
        String participantNumber
) {
    public Preference toEntity() {
        Preference preference = new Preference();
        preference.setUserId(userId);
        preference.setPriority(priority);
        if (detailId != null) {
            preference.setDetailId(detailId);
        }
        preference.setDesiredSession(desiredSession);
        if (setlistId != null) {
            preference.setSetlistId(setlistId);
        }
        preference.setDesiredPosition(desiredPosition);
        preference.setDesiredExtra(desiredExtra);
        preference.setParticipantNumber(participantNumber);
        return preference;
    }
}
