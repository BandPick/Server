package com.example.demo.member;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
public class MemberService {

    private final List<Member> memberList = new ArrayList<>();

    public Member createMember(Member member) {
        for (Member existingMember : memberList) {
            if (existingMember.getParticipantNumber().equals(member.getParticipantNumber())) {
                return null;
            }
        }

        memberList.add(member);
        return member;
    }

    public List<Member> getMembers() {
        return memberList;
    }

    public Member getMemberById(String memberId) {
        for (Member member : memberList) {
            if (member.getParticipantNumber().equals(memberId)) {
                return member;
            }
        }
        return null;
    }

    public Member updateMember(String memberId, Member updatedMember) {
        for (Member member : memberList) {
            if (member.getParticipantNumber().equals(memberId)) {
                member.setName(updatedMember.getName());
                member.setPositions(updatedMember.getPositions());
                return member;
            }
        }
        return null;
    }

    public String deleteMember(String memberId) {
        Iterator<Member> iterator = memberList.iterator();

        while (iterator.hasNext()) {
            Member member = iterator.next();
            if (member.getParticipantNumber().equals(memberId)) {
                iterator.remove();
                return "삭제 완료";
            }
        }

        return "해당 부원을 찾을 수 없습니다.";
    }
}