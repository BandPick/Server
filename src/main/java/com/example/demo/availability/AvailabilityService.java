package com.example.demo.availability;

import com.example.demo.member.Member;
import com.example.demo.member.MemberService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AvailabilityService {

    private final AvailabilityRepository availabilityRepository;
    private final MemberService memberService;

    public AvailabilityService(AvailabilityRepository availabilityRepository,
                               MemberService memberService) {
        this.availabilityRepository = availabilityRepository;
        this.memberService = memberService;
    }

    public Availability createAvailability(Availability availability) {
        Member member = memberService.getMemberById(availability.getParticipantNumber());

        if (member == null) {
            return null;
        }

        availability.setUserId(member.getId());

        return availabilityRepository.save(availability);
    }

    public List<Availability> getAvailabilitiesByMemberId(String participantNumber) {
        Member member = memberService.getMemberById(participantNumber);

        if (member == null) {
            return List.of();
        }

        return availabilityRepository.findByUserId(member.getId());
    }

    public Availability getAvailabilityById(Integer id) {
        return availabilityRepository.findById(id).orElse(null);
    }

    public Availability updateAvailability(String availabilityId, Availability updatedAvailability) {
        Integer id;

        try {
            id = Integer.parseInt(availabilityId);
        } catch (NumberFormatException e) {
            return null;
        }

        Availability availability = availabilityRepository.findById(id).orElse(null);

        if (availability == null) {
            return null;
        }

        if (updatedAvailability.getParticipantNumber() != null) {
            Member member = memberService.getMemberById(updatedAvailability.getParticipantNumber());

            if (member == null) {
                return null;
            }

            availability.setUserId(member.getId());
        }

        availability.setAvailableFrom(updatedAvailability.getAvailableFrom());
        availability.setAvailableTo(updatedAvailability.getAvailableTo());

        return availabilityRepository.save(availability);
    }
}