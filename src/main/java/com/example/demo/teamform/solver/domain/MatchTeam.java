package com.example.demo.teamform.solver.domain;

import java.util.Objects;

/**
 * Problem fact: one team board (A팀 … H팀).
 */
public class MatchTeam {

    private int teamIndex;
    private String name;
    private boolean locked;

    public MatchTeam() {
    }

    public MatchTeam(int teamIndex, String name) {
        this(teamIndex, name, false);
    }

    public MatchTeam(int teamIndex, String name, boolean locked) {
        this.teamIndex = teamIndex;
        this.name = name;
        this.locked = locked;
    }

    public int getTeamIndex() {
        return teamIndex;
    }

    public String getName() {
        return name;
    }

    public boolean isLocked() {
        return locked;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MatchTeam team)) {
            return false;
        }
        return teamIndex == team.teamIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(teamIndex);
    }

    @Override
    public String toString() {
        return name == null ? ("T" + teamIndex) : name;
    }
}
