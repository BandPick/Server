package com.example.demo.teamform.solver.domain;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.HardMediumSoftScore;

import java.util.List;

@PlanningSolution
public class TeamMatchPlan {

    @ProblemFactCollectionProperty
    @ValueRangeProvider
    private List<MatchMember> members;

    @PlanningEntityCollectionProperty
    private List<TeamSeat> seats;

    @PlanningScore
    private HardMediumSoftScore score;

    public TeamMatchPlan() {
    }

    public TeamMatchPlan(List<MatchMember> members, List<TeamSeat> seats) {
        this.members = members;
        this.seats = seats;
    }

    public List<MatchMember> getMembers() {
        return members;
    }

    public void setMembers(List<MatchMember> members) {
        this.members = members;
    }

    public List<TeamSeat> getSeats() {
        return seats;
    }

    public void setSeats(List<TeamSeat> seats) {
        this.seats = seats;
    }

    public HardMediumSoftScore getScore() {
        return score;
    }

    public void setScore(HardMediumSoftScore score) {
        this.score = score;
    }
}
