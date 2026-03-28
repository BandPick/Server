package com.example.demo.team;

import java.util.List;

public class Team {

    private String teamId;
    private String songId;
    private List<String> memberIds;

    public Team() {
    }

    public Team(String teamId, String songId, List<String> memberIds) {
        this.teamId = teamId;
        this.songId = songId;
        this.memberIds = memberIds;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getSongId() {
        return songId;
    }

    public void setSongId(String songId) {
        this.songId = songId;
    }

    public List<String> getMemberIds() {
        return memberIds;
    }

    public void setMemberIds(List<String> memberIds) {
        this.memberIds = memberIds;
    }
}