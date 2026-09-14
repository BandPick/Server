package com.example.demo.teamform.solver.domain;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.HardSoftScore;

import java.util.List;

@PlanningSolution
public class TeamMatchPlan {

    @ProblemFactCollectionProperty
    @ValueRangeProvider
    private List<MatchMember> members;

    @ProblemFactCollectionProperty
    @ValueRangeProvider
    private List<MatchTimeSlot> timeSlots;

    @ProblemFactCollectionProperty
    private List<MatchTeam> teams;

    @PlanningEntityCollectionProperty
    private List<TeamSeat> seats;

    @PlanningEntityCollectionProperty
    private List<TeamSchedule> schedules;

    @PlanningScore
    private HardSoftScore score;

    public TeamMatchPlan() {
    }

    public TeamMatchPlan(
            List<MatchMember> members,
            List<MatchTimeSlot> timeSlots,
            List<MatchTeam> teams,
            List<TeamSeat> seats,
            List<TeamSchedule> schedules
    ) {
        this.members = members;
        this.timeSlots = timeSlots;
        this.teams = teams;
        this.seats = seats;
        this.schedules = schedules;
    }

    public List<MatchMember> getMembers() {
        return members;
    }

    public void setMembers(List<MatchMember> members) {
        this.members = members;
    }

    public List<MatchTimeSlot> getTimeSlots() {
        return timeSlots;
    }

    public void setTimeSlots(List<MatchTimeSlot> timeSlots) {
        this.timeSlots = timeSlots;
    }

    public List<MatchTeam> getTeams() {
        return teams;
    }

    public void setTeams(List<MatchTeam> teams) {
        this.teams = teams;
    }

    public List<TeamSeat> getSeats() {
        return seats;
    }

    public void setSeats(List<TeamSeat> seats) {
        this.seats = seats;
    }

    public List<TeamSchedule> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<TeamSchedule> schedules) {
        this.schedules = schedules;
    }

    public HardSoftScore getScore() {
        return score;
    }

    public void setScore(HardSoftScore score) {
        this.score = score;
    }
}
