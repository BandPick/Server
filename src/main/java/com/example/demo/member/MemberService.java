package com.example.demo.member;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public Member createMember(Member member) {
        if (memberRepository.existsByParticipantNumber(member.getParticipantNumber())) {
            return null;
        }

        return memberRepository.save(member);
    }

    public List<Member> getMembers() {
        return memberRepository.findAll();
    }

    public Member getMemberById(String participantNumber) {
        return memberRepository.findByParticipantNumber(participantNumber);
    }

    public Member updateMember(String participantNumber, Member updatedMember) {
        Member member = memberRepository.findByParticipantNumber(participantNumber);

        if (member == null) {
            return null;
        }

        member.setParticipantNumber(updatedMember.getParticipantNumber());
        member.setName(updatedMember.getName());

        return memberRepository.save(member);
    }

    public String deleteMember(String participantNumber) {
        Member member = memberRepository.findByParticipantNumber(participantNumber);

        if (member == null) {
            return "해당 부원을 찾을 수 없습니다.";
        }

        memberRepository.delete(member);
        return "삭제 완료";
    }
}