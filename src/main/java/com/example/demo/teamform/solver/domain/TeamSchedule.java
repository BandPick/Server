package com.example.demo.teamform.solver.domain;

import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

/**
 * Planning entity: when this team rehearses.
 */
@PlanningEntity
public class TeamSchedule {

    @PlanningId
    private String id;
    private MatchTeam team;

    @PlanningVariable
    private MatchTimeSlot timeSlot;

    @PlanningPin
    private boolean pinned;

    public TeamSchedule() {
    }

    public TeamSchedule(String id, MatchTeam team) {
        this.id = id;
        this.team = team;
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
        return (team == null ? "?" : team) + "@" + timeSlot;
    }
}
