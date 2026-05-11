package com.example.demo.member.dto;

import com.example.demo.member.Member;

public record MemberResponse(
        Long id,
        String participantNumber,
        String name
) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getParticipantNumber(),
                member.getName()
        );
    }
}
