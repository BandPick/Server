package com.example.demo.member.dto;

import com.example.demo.member.Member;

public record MemberRequest(
        String participantNumber,
        String name
) {
    public Member toEntity() {
        Member member = new Member();
        member.setParticipantNumber(participantNumber);
        member.setName(name);
        return member;
    }
}
