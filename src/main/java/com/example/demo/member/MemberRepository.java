package com.example.demo.member;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Member findByParticipantNumber(String participantNumber);

    boolean existsByParticipantNumber(String participantNumber);
}
