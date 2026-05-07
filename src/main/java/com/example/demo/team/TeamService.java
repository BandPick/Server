package com.example.demo.team;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeamService {

    private final TeamRepository teamRepository;

    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    public Team createTeam(Team team) {
        return teamRepository.save(team);
    }

    public List<Team> getTeams() {
        return teamRepository.findAll();
    }

    public Team getTeamById(String teamId) {

        try {
            Integer id = Integer.parseInt(teamId);

            return teamRepository.findById(id).orElse(null);

        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Team updateTeam(String teamId, Team updatedTeam) {

        Team team = getTeamById(teamId);

        if (team == null) {
            return null;
        }

        team.setSetlistId(updatedTeam.getSetlistId());

        return teamRepository.save(team);
    }

    public String deleteTeam(String teamId) {

        Team team = getTeamById(teamId);

        if (team == null) {
            return "해당 팀을 찾을 수 없습니다.";
        }

        teamRepository.delete(team);

        return "삭제 완료";
    }
}