package com.example.demo.teammember;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamMemberRepository
        extends JpaRepository<TeamMember, Integer> {

    List<TeamMember> findByTeamId(Integer teamId);

    List<TeamMember> findByUserId(Long userId);
}