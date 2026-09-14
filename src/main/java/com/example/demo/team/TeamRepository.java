package com.example.demo.team;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Integer> {

    List<Team> findByTeamTypeOrderByNameAscIdAsc(String teamType);

    void deleteByTeamType(String teamType);
}
