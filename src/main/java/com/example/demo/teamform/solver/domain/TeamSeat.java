package com.example.demo.teamform.solver.domain;

import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

/**
 * Planning entity: one position seat on a team board.
 */
@PlanningEntity
public class TeamSeat {

    @PlanningId
    private String id;
    private int teamIndex;
    private String position;

    @PlanningVariable(allowsUnassigned = true)
    private MatchMember member;

    public TeamSeat() {
    }

    public TeamSeat(String id, int teamIndex, String position) {
        this.id = id;
        this.teamIndex = teamIndex;
        this.position = position;
    }

    public String getId() {
        return id;
    }

    public int getTeamIndex() {
        return teamIndex;
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

    public boolean isAssigned() {
        return member != null;
    }

    @Override
    public String toString() {
        return "T" + teamIndex + "-" + position + "=" + member;
    }
}
