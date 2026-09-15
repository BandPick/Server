package com.example.demo.teamform.solver.domain;

import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

/**
 * Planning entity: one of a team's weekly rehearsals (each team has two).
 */
@PlanningEntity
public class TeamSchedule {

    @PlanningId
    private String id;
    private MatchTeam team;
    /** 1 = first weekly rehearsal, 2 = second (display / identity only). */
    private int rehearsalIndex;

    @PlanningVariable
    private MatchTimeSlot timeSlot;

    @PlanningPin
    private boolean pinned;

    public TeamSchedule() {
    }

    public TeamSchedule(String id, MatchTeam team, int rehearsalIndex) {
        this.id = id;
        this.team = team;
        this.rehearsalIndex = rehearsalIndex;
    }

    public String getId() {
        return id;
    }

    public MatchTeam getTeam() {
        return team;
    }

    public int getTeamIndex() {
        return team == null ? -1 : team.getTeamIndex();
    }

    public int getRehearsalIndex() {
        return rehearsalIndex;
    }

    public MatchTimeSlot getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(MatchTimeSlot timeSlot) {
        this.timeSlot = timeSlot;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public boolean isOpen() {
        return team == null || !team.isLocked();
    }

    @Override
    public String toString() {
        return (team == null ? "?" : team) + "#" + rehearsalIndex + "@" + timeSlot;
    }
}
