package com.example.demo.teamform.solver.domain;

import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

/**
 * Planning entity: one session seat on a team board (V / D / B / EG1 / EG2 / K).
 */
@PlanningEntity
public class TeamSeat {

    @PlanningId
    private String id;
    private MatchTeam team;
    private String position;

    @PlanningVariable(allowsUnassigned = true)
    private MatchMember member;

    @PlanningPin
    private boolean pinned;

    public TeamSeat() {
    }

    public TeamSeat(String id, MatchTeam team, String position) {
        this.id = id;
        this.team = team;
        this.position = position;
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

    public String getPosition() {
        return position;
    }

    public MatchMember getMember() {
        return member;
    }

    public void setMember(MatchMember member) {
        this.member = member;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public boolean isAssigned() {
        return member != null;
    }

    public boolean isOpen() {
        return team == null || !team.isLocked();
    }

    public boolean isVocal() {
        return MatchMember.isVocalSeat(position);
    }

    public boolean isInstrument() {
        return position != null && !isVocal();
    }

    /** Application skill code (V1/V2 → V). */
    public String skillPosition() {
        return MatchMember.skillKey(position);
    }

    @Override
    public String toString() {
        return (team == null ? "?" : team) + "-" + position + "=" + member;
    }
}
