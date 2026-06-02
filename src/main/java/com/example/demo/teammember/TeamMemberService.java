package com.example.demo.teammember;

import com.example.demo.member.Member;
import com.example.demo.member.MemberService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeamMemberService {

    private final TeamMemberRepository teamMemberRepository;
    private final MemberService memberService;

    public TeamMemberService(TeamMemberRepository teamMemberRepository,
                             MemberService memberService) {

        this.teamMemberRepository = teamMemberRepository;
        this.memberService = memberService;
    }

    public TeamMember createTeamMember(TeamMember teamMember) {

        Member member =
                memberService.getMemberById(
                        teamMember.getParticipantNumber()
                );

        if (member == null) {
            return null;
        }

        teamMember.setUserId(member.getId());

        return teamMemberRepository.save(teamMember);
    }

    public List<TeamMember> getTeamMembersByTeamId(Integer teamId) {

        return teamMemberRepository.findByTeamId(teamId);
    }

    public TeamMember updateTeamMember(Integer id,
                                       TeamMember updatedTeamMember) {

        TeamMember teamMember =
                teamMemberRepository.findById(id).orElse(null);

        if (teamMember == null) {
            return null;
        }

        if (updatedTeamMember.getParticipantNumber() != null) {

            Member member =
                    memberService.getMemberById(
                            updatedTeamMember.getParticipantNumber()
                    );

            if (member == null) {
                return null;
            }

            teamMember.setUserId(member.getId());
        }

        teamMember.setTeamId(updatedTeamMember.getTeamId());
        teamMember.setSessionId(updatedTeamMember.getSessionId());
        teamMember.setSessionPosition(updatedTeamMember.getSessionPosition());
        teamMember.setSessionExtra(updatedTeamMember.getSessionExtra());

        return teamMemberRepository.save(teamMember);
    }

    public String deleteTeamMember(Integer id) {

        TeamMember teamMember =
                teamMemberRepository.findById(id).orElse(null);

        if (teamMember == null) {
            return "해당 팀 멤버를 찾을 수 없습니다.";
        }

        teamMemberRepository.delete(teamMember);

        return "삭제 완료";
    }
}
