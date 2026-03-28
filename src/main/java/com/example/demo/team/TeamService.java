package com.example.demo.team;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TeamService {

    private final List<Team> teamList = new ArrayList<>();

    public void saveTeams(List<Team> teams) {
        teamList.clear();
        teamList.addAll(teams);
    }

    public List<Team> getTeams() {
        return teamList;
    }

    public Team getTeamById(String teamId) {
        for (Team team : teamList) {
            if (team.getTeamId().equals(teamId)) {
                return team;
            }
        }
        return null;
    }

    public Team updateTeamMembers(String teamId, List<String> memberIds) {
        for (Team team : teamList) {
            if (team.getTeamId().equals(teamId)) {
                team.setMemberIds(memberIds);
                return team;
            }
        }
        return null;
    }
}