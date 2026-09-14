package com.example.demo.teamform;

import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import com.example.demo.teamform.dto.TeamFormMemberResponse;
import com.example.demo.teamform.dto.TeamFormPositionResponse;
import com.example.demo.teamform.dto.TeamFormScheduleResponse;
import com.example.demo.teamform.dto.TeamSystemLockedMemberRequest;
import com.example.demo.teamform.dto.TeamSystemLockedTeamRequest;
import com.example.demo.teamform.dto.TeamSystemMatchRequest;
import com.example.demo.teamform.dto.TeamSystemMatchResponse;
import com.example.demo.teamform.dto.TeamSystemTeamMemberResponse;
import com.example.demo.teamform.dto.TeamSystemTeamResponse;
import com.example.demo.teamform.dto.TeamSystemUnmatchedResponse;
import com.example.demo.teamform.solver.domain.MatchMember;
import com.example.demo.teamform.solver.domain.TeamMatchPlan;
import com.example.demo.teamform.solver.domain.TeamSeat;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TeamSystemMatchService {

    private static final List<String> POSITION_ORDER = List.of("V", "D", "B", "EG1", "EG2", "K");
    private static final Set<String> CORE_POSITIONS = Set.of("V", "D", "B");
    private static final Set<String> GUITAR_POSITIONS = Set.of("EG1", "EG2");
    private static final int MIN_UNIQUE_MEMBERS = 3;
    private static final int MAX_TEAM_BOARDS = 7;

    private final TeamFormService teamFormService;
    private final SolverFactory<TeamMatchPlan> solverFactory;

    public TeamSystemMatchService(
            TeamFormService teamFormService,
            SolverFactory<TeamMatchPlan> solverFactory
    ) {
        this.teamFormService = teamFormService;
        this.solverFactory = solverFactory;
    }

    public TeamSystemMatchResponse match() {
        return match(null);
    }

    public TeamSystemMatchResponse match(TeamSystemMatchRequest request) {
        Set<Long> fixedUserIds = lockedUserIds(request);

        List<MatchMember> allMembers = teamFormService.listAll().stream()
                .map(this::toMember)
                .filter(member -> member.getLevels() != null && !member.getLevels().isEmpty())
                .toList();

        List<MatchMember> freeMembers = allMembers.stream()
                .filter(member -> !fixedUserIds.contains(member.getUserId()))
                .sorted(Comparator
                        .comparingInt((MatchMember member) -> member.getSlots() == null ? 0 : member.getSlots().size())
                        .thenComparing(Comparator.comparingInt(MatchMember::scheduleScarcity).reversed())
                        .thenComparingLong(MatchMember::getUserId))
                .toList();

        if (freeMembers.isEmpty()) {
            return new TeamSystemMatchResponse(List.of(), unmatchedOf(allMembers, fixedUserIds));
        }

        TeamMatchPlan problem = buildProblem(freeMembers);
        Solver<TeamMatchPlan> solver = solverFactory.buildSolver();
        TeamMatchPlan solution = solver.solve(problem);

        return toResponse(solution, allMembers, fixedUserIds);
    }

    private TeamMatchPlan buildProblem(List<MatchMember> freeMembers) {
        int capacity = freeMembers.stream().mapToInt(MatchMember::getMaxTeams).sum();
        int teamCount = Math.min(
                MAX_TEAM_BOARDS,
                Math.max(1, (capacity + MIN_UNIQUE_MEMBERS - 1) / MIN_UNIQUE_MEMBERS)
        );

        List<TeamSeat> seats = new ArrayList<>();
        for (int teamIndex = 0; teamIndex < teamCount; teamIndex++) {
            for (String position : POSITION_ORDER) {
                seats.add(new TeamSeat(teamIndex + "-" + position, teamIndex, position));
            }
        }
        return new TeamMatchPlan(freeMembers, seats);
    }

    private TeamSystemMatchResponse toResponse(
            TeamMatchPlan solution,
            List<MatchMember> allMembers,
            Set<Long> fixedUserIds
    ) {
        Map<Integer, List<TeamSeat>> byTeam = solution.getSeats().stream()
                .filter(TeamSeat::isAssigned)
                .collect(Collectors.groupingBy(TeamSeat::getTeamIndex));

        List<Integer> viableTeamIndexes = byTeam.keySet().stream()
                .sorted()
                .filter(index -> isViable(byTeam.get(index)))
                .toList();

        Set<Long> assigned = new HashSet<>(fixedUserIds);
        List<TeamSystemTeamResponse> teams = new ArrayList<>();
        for (int outputIndex = 0; outputIndex < viableTeamIndexes.size(); outputIndex++) {
            int teamIndex = viableTeamIndexes.get(outputIndex);
            List<TeamSeat> seats = byTeam.get(teamIndex);
            teams.add(toTeamResponse(outputIndex, seats));
            for (TeamSeat seat : seats) {
                assigned.add(seat.getMember().getUserId());
            }
        }

        return new TeamSystemMatchResponse(teams, unmatchedOf(allMembers, assigned));
    }

    private List<TeamSystemUnmatchedResponse> unmatchedOf(
            List<MatchMember> members,
            Set<Long> assigned
    ) {
        return members.stream()
                .filter(member -> !assigned.contains(member.getUserId()))
                .map(member -> new TeamSystemUnmatchedResponse(
                        member.getUserId(),
                        member.getName(),
                        "아직 팀에 배정되지 않았습니다."
                ))
                .toList();
    }

    private TeamSystemTeamResponse toTeamResponse(int index, List<TeamSeat> seats) {
        List<TeamSystemTeamMemberResponse> members = seats.stream()
                .filter(TeamSeat::isAssigned)
                .sorted(Comparator.comparingInt(seat -> POSITION_ORDER.indexOf(seat.getPosition())))
                .map(seat -> new TeamSystemTeamMemberResponse(
                        seat.getMember().getUserId(),
                        seat.getPosition(),
                        seat.getMember().getName(),
                        seat.getMember().getLevels().getOrDefault(seat.getPosition(), "")
                ))
                .toList();

        List<MatchMember> assigned = seats.stream()
                .filter(TeamSeat::isAssigned)
                .map(TeamSeat::getMember)
                .distinct()
                .toList();
        int common = MatchMember.commonSlots(assigned).size();
        StringBuilder note = new StringBuilder("공통 가능 시간 " + common + "칸");
        if (hasDualVocalGuitar(seats)) {
            note.append(" · 보컬·기타 겸임");
        }

        return new TeamSystemTeamResponse(
                teamName(index),
                "완료",
                note.toString(),
                members
        );
    }

    private boolean isViable(List<TeamSeat> seats) {
        Set<String> filled = seats.stream()
                .filter(TeamSeat::isAssigned)
                .map(TeamSeat::getPosition)
                .collect(Collectors.toSet());
        if (!filled.containsAll(CORE_POSITIONS) || !hasGuitar(seats)) {
            return false;
        }
        List<MatchMember> members = seats.stream()
                .filter(TeamSeat::isAssigned)
                .map(TeamSeat::getMember)
                .distinct()
                .toList();
        if (MatchMember.commonSlots(members).isEmpty()) {
            return false;
        }
        int unique = members.size();
        if (hasDualVocalGuitar(seats)) {
            return unique >= MIN_UNIQUE_MEMBERS;
        }
        return unique >= 4;
    }

    private boolean hasGuitar(List<TeamSeat> seats) {
        return seats.stream()
                .filter(TeamSeat::isAssigned)
                .map(TeamSeat::getPosition)
                .anyMatch(GUITAR_POSITIONS::contains);
    }

    private boolean hasDualVocalGuitar(List<TeamSeat> seats) {
        Map<Long, Set<String>> byMember = new LinkedHashMap<>();
        for (TeamSeat seat : seats) {
            if (!seat.isAssigned()) {
                continue;
            }
            byMember.computeIfAbsent(seat.getMember().getUserId(), id -> new HashSet<>())
                    .add(seat.getPosition());
        }
        for (Set<String> positions : byMember.values()) {
            if (positions.contains("V") && positions.stream().anyMatch(GUITAR_POSITIONS::contains)) {
                return true;
            }
        }
        return false;
    }

    private String teamName(int index) {
        if (index < 26) {
            return (char) ('A' + index) + "팀";
        }
        return (index + 1) + "팀";
    }

    private Set<Long> lockedUserIds(TeamSystemMatchRequest request) {
        Set<Long> fixedUserIds = new HashSet<>();
        if (request == null || request.lockedTeams() == null) {
            return fixedUserIds;
        }
        for (TeamSystemLockedTeamRequest team : request.lockedTeams()) {
            if (team == null || team.members() == null) {
                continue;
            }
            for (TeamSystemLockedMemberRequest member : team.members()) {
                if (member != null && member.userId() != null) {
                    fixedUserIds.add(member.userId());
                }
            }
        }
        return fixedUserIds;
    }

    private MatchMember toMember(TeamFormMemberResponse form) {
        Map<String, String> levels = new LinkedHashMap<>();
        Map<String, Integer> priorities = new LinkedHashMap<>();
        for (TeamFormPositionResponse position : form.positions()) {
            if (position.position() == null || position.position().isBlank()) {
                continue;
            }
            levels.put(position.position(), position.level() == null ? "" : position.level());
            if (position.priority() > 0) {
                priorities.put(position.position(), position.priority());
            }
        }
        Set<String> slots = new HashSet<>();
        for (TeamFormScheduleResponse schedule : form.schedules()) {
            if (schedule.dayOfWeek() == null || schedule.startTime() == null) {
                continue;
            }
            slots.add(schedule.dayOfWeek() + "-" + schedule.startTime());
        }
        return new MatchMember(
                form.userId(),
                form.name(),
                Math.max(1, Math.min(4, form.maxTeams())),
                levels,
                priorities,
                slots
        );
    }
}
