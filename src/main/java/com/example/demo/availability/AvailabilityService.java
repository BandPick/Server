package com.example.demo.availability;

import com.example.demo.member.Member;
import com.example.demo.member.MemberService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AvailabilityService {

    private final List<Availability> availabilityList = new ArrayList<>();
    private int availabilitySequence = 1;
    private final MemberService memberService;

    public AvailabilityService(MemberService memberService) {
        this.memberService = memberService;
    }

    public Availability createAvailability(Availability availability) {
        Member member = memberService.getMemberById(availability.getParticipantNumber());

        if (member == null) {
            return null;
        }

        String newAvailabilityId = "AV" + String.format("%03d", availabilitySequence++);
        availability.setAvailabilityId(newAvailabilityId);
        availabilityList.add(availability);
        return availability;
    }

    public List<Availability> getAvailabilitiesByMemberId(String memberId) {
        List<Availability> result = new ArrayList<>();

        for (Availability availability : availabilityList) {
            if (availability.getParticipantNumber().equals(memberId)) {
                result.add(availability);
            }
        }

        return result;
    }

    public Availability getAvailabilityById(String availabilityId) {
        for (Availability availability : availabilityList) {
            if (availability.getAvailabilityId().equals(availabilityId)) {
                return availability;
            }
        }
        return null;
    }

    public Availability updateAvailability(String availabilityId, Availability updatedAvailability) {
        Member member = memberService.getMemberById(updatedAvailability.getParticipantNumber());

        if (member == null) {
            return null;
        }

        for (Availability availability : availabilityList) {
            if (availability.getAvailabilityId().equals(availabilityId)) {
                availability.setParticipantNumber(updatedAvailability.getParticipantNumber());
                availability.setType(updatedAvailability.getType());
                availability.setWeekNumber(updatedAvailability.getWeekNumber());
                availability.setDayOfWeek(updatedAvailability.getDayOfWeek());
                availability.setStartTime(updatedAvailability.getStartTime());
                availability.setEndTime(updatedAvailability.getEndTime());
                return availability;
            }
        }

        return null;
    }
}