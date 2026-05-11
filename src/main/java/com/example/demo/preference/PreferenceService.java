package com.example.demo.preference;

import com.example.demo.member.Member;
import com.example.demo.member.MemberService;
import com.example.demo.setlist.SetlistRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PreferenceService {

    private final List<Preference> preferenceList = new ArrayList<>();
    private int preferenceSequence = 1;
    private final MemberService memberService;
    private final SetlistRepository setlistRepository;

    public PreferenceService(MemberService memberService, SetlistRepository setlistRepository) {
        this.memberService = memberService;
        this.setlistRepository = setlistRepository;
    }

    public Preference createPreference(Preference preference) {
        Member member = memberService.getMemberById(preference.getParticipantNumber());
        boolean hasSetlist = existsSetlist(preference.getSongId());

        if (member == null || !hasSetlist) {
            return null;
        }

        String newPreferenceId = "P" + String.format("%03d", preferenceSequence++);
        preference.setPreferenceId(newPreferenceId);
        preferenceList.add(preference);
        return preference;
    }

    public List<Preference> getPreferencesByMemberId(String memberId) {
        List<Preference> result = new ArrayList<>();

        for (Preference preference : preferenceList) {
            if (preference.getParticipantNumber().equals(memberId)) {
                result.add(preference);
            }
        }

        return result;
    }

    public Preference getPreferenceById(String preferenceId) {
        for (Preference preference : preferenceList) {
            if (preference.getPreferenceId().equals(preferenceId)) {
                return preference;
            }
        }
        return null;
    }

    public Preference updatePreference(String preferenceId, Preference updatedPreference) {
        Member member = memberService.getMemberById(updatedPreference.getParticipantNumber());
        boolean hasSetlist = existsSetlist(updatedPreference.getSongId());

        if (member == null || !hasSetlist) {
            return null;
        }

        for (Preference preference : preferenceList) {
            if (preference.getPreferenceId().equals(preferenceId)) {
                preference.setParticipantNumber(updatedPreference.getParticipantNumber());
                preference.setSongId(updatedPreference.getSongId());
                preference.setPosition(updatedPreference.getPosition());
                preference.setPreferenceRank(updatedPreference.getPreferenceRank());
                return preference;
            }
        }

        return null;
    }

    public List<PreferenceSummary> getPreferenceSummary() {
        List<PreferenceSummary> summaryList = new ArrayList<>();

        for (Preference preference : preferenceList) {
            boolean found = false;

            for (PreferenceSummary summary : summaryList) {
                if (summary.getSongId().equals(preference.getSongId())) {
                    summary.setCount(summary.getCount() + 1);
                    found = true;
                    break;
                }
            }

            if (!found) {
                summaryList.add(new PreferenceSummary(preference.getSongId(), 1));
            }
        }

        return summaryList;
    }

    private boolean existsSetlist(String songId) {
        if (songId == null || songId.isBlank()) {
            return false;
        }
        try {
            long setlistId = Long.parseLong(songId.trim());
            return setlistRepository.existsById(setlistId);
        } catch (NumberFormatException e) {
            return false;
        }
    }
}