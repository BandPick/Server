package com.example.demo.preference;

import com.example.demo.member.Member;
import com.example.demo.member.MemberService;
import com.example.demo.song.Song;
import com.example.demo.song.SongService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PreferenceService {

    private final List<Preference> preferenceList = new ArrayList<>();
    private int preferenceSequence = 1;
    private final MemberService memberService;
    private final SongService songService;

    public PreferenceService(MemberService memberService, SongService songService) {
        this.memberService = memberService;
        this.songService = songService;
    }

    public Preference createPreference(Preference preference) {
        Member member = memberService.getMemberById(preference.getParticipantNumber());
        Song song = songService.getSongById(preference.getSongId());

        if (member == null || song == null) {
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
        Song song = songService.getSongById(updatedPreference.getSongId());

        if (member == null || song == null) {
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
}