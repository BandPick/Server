package com.example.demo.preference;

import com.example.demo.member.Member;
import com.example.demo.member.MemberService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PreferenceService {

    private final PreferenceRepository preferenceRepository;
    private final MemberService memberService;

    public PreferenceService(PreferenceRepository preferenceRepository,
                             MemberService memberService) {
        this.preferenceRepository = preferenceRepository;
        this.memberService = memberService;
    }

    public Preference createPreference(Preference preference) {
        Member member = memberService.getMemberById(preference.getParticipantNumber());

        if (member == null) {
            return null;
        }

        preference.setUserId(member.getId());

        return preferenceRepository.save(preference);
    }

    public List<Preference> getPreferencesByMemberId(String participantNumber) {
        Member member = memberService.getMemberById(participantNumber);

        if (member == null) {
            return List.of();
        }

        return preferenceRepository.findByUserId(member.getId());
    }

    public Preference updatePreference(String preferenceId, Preference updatedPreference) {
        Integer id;

        try {
            id = Integer.parseInt(preferenceId);
        } catch (NumberFormatException e) {
            return null;
        }

        Preference preference = preferenceRepository.findById(id).orElse(null);

        if (preference == null) {
            return null;
        }

        if (updatedPreference.getParticipantNumber() != null) {
            Member member = memberService.getMemberById(updatedPreference.getParticipantNumber());

            if (member == null) {
                return null;
            }

            preference.setUserId(member.getId());
        }

        preference.setPriority(updatedPreference.getPriority());
        preference.setDetailId(updatedPreference.getDetailId());
        preference.setDesiredSession(updatedPreference.getDesiredSession());

        return preferenceRepository.save(preference);
    }

    public List<PreferenceSummary> getPreferenceSummary() {
        List<PreferenceSummary> summaryList = new ArrayList<>();

        for (Preference preference : preferenceRepository.findAll()) {
            boolean found = false;

            for (PreferenceSummary summary : summaryList) {
                if (summary.getDetailId().equals(preference.getDetailId())) {
                    summary.setCount(summary.getCount() + 1);
                    found = true;
                    break;
                }
            }

            if (!found) {
                summaryList.add(new PreferenceSummary(preference.getDetailId(), 1));
            }
        }

        return summaryList;
    }
}