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
import com.example.demo.teamform.solver.domain.MatchTeam;
import com.example.demo.teamform.solver.domain.MatchTimeSlot;
import com.example.demo.teamform.solver.domain.TeamMatchPlan;
import com.example.demo.teamform.solver.domain.TeamSchedule;
import com.example.demo.teamform.solver.domain.TeamSeat;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TeamSystemMatchService {

    private static final List<String> POSITION_ORDER = List.of("V", "D", "B", "EG1", "EG2", "K");
    private static final List<String> DAY_ORDER = List.of("월", "화", "수", "목", "금");
    private static final List<String> TEAM_NAMES = List.of(
            "A팀", "B팀", "C팀", "D팀", "E팀", "F팀", "G팀", "H팀"
    );
    private static final int MAX_TEAM_BOARDS = 8;
    private static final int TARGET_UNIQUE_PER_TEAM = 5;
    private static final int REHEARSALS_PER_TEAM = 2;

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
        List<TeamSystemLockedTeamRequest> lockedTeams = lockedTeamsOf(request);

        List<MatchMember> allMembers = teamFormService.listAll().stream()
                .map(this::toMember)
                .filter(member -> member.getLevels() != null && !member.getLevels().isEmpty())
                .toList();

        Map<Long, MatchMember> membersById = new HashMap<>();
        for (MatchMember member : allMembers) {
            membersById.put(member.getUserId(), member);
        }

        Set<Long> lockedUserIds = lockedUserIds(lockedTeams);
        Map<Long, Integer> lockedTeamCountByUser = lockedTeamCountByUser(lockedTeams);
        int remainingCapacity = remainingCapacity(allMembers, lockedTeamCountByUser);
        int remainingBoards = Math.max(0, MAX_TEAM_BOARDS - lockedTeams.size());

        if (allMembers.isEmpty() || remainingBoards == 0 || remainingCapacity <= 0) {
            return new TeamSystemMatchResponse(List.of(), unmatchedOf(allMembers, lockedUserIds));
        }

        TeamMatchPlan problem = buildProblem(
                allMembers,
                membersById,
                lockedTeams,
                remainingBoards,
                remainingCapacity
        );
        Solver<TeamMatchPlan> solver = solverFactory.buildSolver();
        TeamMatchPlan solution = solver.solve(problem);

        return toResponse(solution, allMembers, lockedUserIds);
    }

    private TeamMatchPlan buildProblem(
            List<MatchMember> allMembers,
            Map<Long, MatchMember> membersById,
            List<TeamSystemLockedTeamRequest> lockedTeams,
            int remainingBoards,
            int remainingCapacity
    ) {
        int openTeamCount = Math.min(
                remainingBoards,
                Math.max(1, (remainingCapacity + TARGET_UNIQUE_PER_TEAM - 1) / TARGET_UNIQUE_PER_TEAM)
        );

        List<MatchTimeSlot> timeSlots = collectTimeSlots(allMembers);
        List<MatchTeam> teams = new ArrayList<>();
        List<TeamSeat> seats = new ArrayList<>();
        List<TeamSchedule> schedules = new ArrayList<>();

        int teamIndex = 0;
        for (TeamSystemLockedTeamRequest locked : lockedTeams) {
            MatchTeam team = new MatchTeam(teamIndex, teamName(teamIndex), true);
            teams.add(team);
            addLockedTeamEntities(team, locked, membersById, timeSlots, seats, schedules);
            teamIndex += 1;
        }

        for (int offset = 0; offset < openTeamCount; offset++) {
            MatchTeam team = new MatchTeam(teamIndex, teamName(teamIndex), false);
            teams.add(team);
            for (String position : POSITION_ORDER) {
                seats.add(new TeamSeat(teamIndex + "-" + position, team, position));
            }
            for (int rehearsalIndex = 1; rehearsalIndex <= REHEARSALS_PER_TEAM; rehearsalIndex++) {
                schedules.add(new TeamSchedule(
                        "schedule-" + teamIndex + "-" + rehearsalIndex,
                        team,
                        rehearsalIndex
                ));
            }
            teamIndex += 1;
        }

        return new TeamMatchPlan(allMembers, timeSlots, teams, seats, schedules);
    }

    private void addLockedTeamEntities(
            MatchTeam team,
            TeamSystemLockedTeamRequest locked,
            Map<Long, MatchMember> membersById,
            List<MatchTimeSlot> timeSlots,
            List<TeamSeat> seats,
            List<TeamSchedule> schedules
    ) {
        Map<String, MatchMember> occupantByPosition = new LinkedHashMap<>();
        List<MatchMember> lockedMembers = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        if (locked != null && locked.members() != null) {
            for (TeamSystemLockedMemberRequest item : locked.members()) {
                if (item == null || item.userId() == null) {
                    continue;
                }
                MatchMember member = membersById.get(item.userId());
                if (member == null) {
                    continue;
                }
                String position = normalizeLockedPosition(item.session());
                if (position != null) {
                    occupantByPosition.put(position, member);
                }
                if (seen.add(member.getUserId())) {
                    lockedMembers.add(member);
                }
            }
        }

        for (String position : POSITION_ORDER) {
            TeamSeat seat = new TeamSeat(team.getTeamIndex() + "-" + position, team, position);
            seat.setMember(occupantByPosition.get(position));
            seat.setPinned(true);
            seats.add(seat);
        }

        List<MatchTimeSlot> picked = pickLockedTimeSlots(lockedMembers, timeSlots);
        for (int rehearsalIndex = 1; rehearsalIndex <= REHEARSALS_PER_TEAM; rehearsalIndex++) {
            TeamSchedule schedule = new TeamSchedule(
                    "schedule-" + team.getTeamIndex() + "-" + rehearsalIndex,
                    team,
                    rehearsalIndex
            );
            MatchTimeSlot slot = picked.get(Math.min(rehearsalIndex - 1, picked.size() - 1));
            schedule.setTimeSlot(slot);
            schedule.setPinned(true);
            schedules.add(schedule);
        }
    }

    private String normalizeLockedPosition(String session) {
        if (session == null || session.isBlank()) {
            return null;
        }
        String normalized = session.trim().toUpperCase();
        if ("K1".equals(normalized) || "K2".equals(normalized)) {
            normalized = "K";
        }
        return POSITION_ORDER.contains(normalized) ? normalized : null;
    }

    private List<MatchTimeSlot> pickLockedTimeSlots(
            List<MatchMember> members,
            List<MatchTimeSlot> fallback
    ) {
        Set<MatchTimeSlot> common = MatchMember.commonTimeSlots(members);
        List<MatchTimeSlot> pool = !common.isEmpty()
                ? sortTimeSlots(common)
                : sortTimeSlots(new LinkedHashSet<>(fallback));

        if (pool.isEmpty()) {
            MatchTimeSlot monday = new MatchTimeSlot("월", "19:00");
            MatchTimeSlot wednesday = new MatchTimeSlot("수", "19:00");
            return List.of(monday, wednesday);
        }

        MatchTimeSlot first = pool.get(0);
        MatchTimeSlot second = null;
        for (MatchTimeSlot slot : pool) {
            if (!slot.getDayOfWeek().equals(first.getDayOfWeek())) {
                second = slot;
                break;
            }
        }
        if (second == null) {
            // Common pool is single-day only; still return two slots (solver/UI note),
            // preferring a different day from the full fallback grid when possible.
            for (MatchTimeSlot slot : sortTimeSlots(new LinkedHashSet<>(fallback))) {
                if (!slot.getDayOfWeek().equals(first.getDayOfWeek())) {
                    second = slot;
                    break;
                }
            }
        }
        if (second == null) {
            second = first;
        }
        return List.of(first, second);
    }

    private TeamSystemMatchResponse toResponse(
            TeamMatchPlan solution,
            List<MatchMember> allMembers,
            Set<Long> fixedUserIds
    ) {
        Map<Integer, List<TeamSeat>> byTeam = solution.getSeats().stream()
                .filter(TeamSeat::isAssigned)
                .filter(TeamSeat::isOpen)
                .collect(Collectors.groupingBy(TeamSeat::getTeamIndex));

        Map<Integer, List<MatchTimeSlot>> timesByTeam = solution.getSchedules().stream()
                .filter(schedule -> schedule.getTimeSlot() != null)
                .filter(TeamSchedule::isOpen)
                .sorted(Comparator
                        .comparingInt(TeamSchedule::getTeamIndex)
                        .thenComparingInt(TeamSchedule::getRehearsalIndex))
                .collect(Collectors.groupingBy(
                        TeamSchedule::getTeamIndex,
                        LinkedHashMap::new,
                        Collectors.mapping(TeamSchedule::getTimeSlot, Collectors.toList())
                ));

        List<Integer> usedTeamIndexes = byTeam.keySet().stream()
                .sorted()
                .filter(index -> !byTeam.get(index).isEmpty())
                .toList();

        Set<Long> assigned = new HashSet<>(fixedUserIds);
        List<TeamSystemTeamResponse> teams = new ArrayList<>();
        for (int outputIndex = 0; outputIndex < usedTeamIndexes.size(); outputIndex++) {
            int teamIndex = usedTeamIndexes.get(outputIndex);
            List<TeamSeat> seats = byTeam.get(teamIndex);
            teams.add(toTeamResponse(outputIndex, seats, timesByTeam.getOrDefault(teamIndex, List.of())));
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

    private TeamSystemTeamResponse toTeamResponse(
            int index,
            List<TeamSeat> seats,
            List<MatchTimeSlot> timeSlots
    ) {
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

        boolean complete = POSITION_ORDER.stream().allMatch(position ->
                seats.stream().anyMatch(seat -> seat.isAssigned() && position.equals(seat.getPosition()))
        );
        String status = complete ? "완료" : "대기";
        String note = buildNote(seats, timeSlots);

        return new TeamSystemTeamResponse(
                teamName(index),
                status,
                note,
                members
        );
    }

    private String buildNote(List<TeamSeat> seats, List<MatchTimeSlot> timeSlots) {
        StringBuilder note = new StringBuilder();
        List<String> displays = timeSlots == null
                ? List.of()
                : timeSlots.stream()
                .filter(slot -> slot != null && !slot.display().isBlank())
                .map(MatchTimeSlot::display)
                .distinct()
                .toList();
        if (!displays.isEmpty()) {
            note.append("배정 합주 ").append(String.join(" · ", displays));
            long distinctDays = timeSlots.stream()
                    .filter(slot -> slot != null && !slot.getDayOfWeek().isBlank())
                    .map(MatchTimeSlot::getDayOfWeek)
                    .distinct()
                    .count();
            if (distinctDays >= REHEARSALS_PER_TEAM) {
                note.append(" (주 ").append(distinctDays).append("일)");
            }
        }
        if (hasDualVocalInstrument(seats)) {
            if (!note.isEmpty()) {
                note.append(" · ");
            }
            note.append("보컬·악기 겸임");
        }
        return note.toString();
    }

    private boolean hasDualVocalInstrument(List<TeamSeat> seats) {
        Map<Long, Set<String>> byMember = new LinkedHashMap<>();
        for (TeamSeat seat : seats) {
            if (!seat.isAssigned()) {
                continue;
            }
            byMember.computeIfAbsent(seat.getMember().getUserId(), id -> new HashSet<>())
                    .add(seat.getPosition());
        }
        for (Set<String> positions : byMember.values()) {
            if (positions.contains("V") && positions.stream().anyMatch(position -> !"V".equals(position))) {
                return true;
            }
        }
        return false;
    }

    private String teamName(int index) {
        if (index >= 0 && index < TEAM_NAMES.size()) {
            return TEAM_NAMES.get(index);
        }
        return (index + 1) + "팀";
    }

    private List<TeamSystemLockedTeamRequest> lockedTeamsOf(TeamSystemMatchRequest request) {
        if (request == null || request.lockedTeams() == null) {
            return List.of();
        }
        return request.lockedTeams().stream()
                .filter(team -> team != null && team.members() != null && !team.members().isEmpty())
                .toList();
    }

    private Map<Long, Integer> lockedTeamCountByUser(List<TeamSystemLockedTeamRequest> lockedTeams) {
        Map<Long, Integer> counts = new HashMap<>();
        for (TeamSystemLockedTeamRequest team : lockedTeams) {
            Set<Long> seen = new HashSet<>();
            for (TeamSystemLockedMemberRequest member : team.members()) {
                if (member == null || member.userId() == null || !seen.add(member.userId())) {
                    continue;
                }
                counts.merge(member.userId(), 1, Integer::sum);
            }
        }
        return counts;
    }

    private int remainingCapacity(List<MatchMember> members, Map<Long, Integer> lockedTeamCountByUser) {
        int remaining = 0;
        for (MatchMember member : members) {
            int used = lockedTeamCountByUser.getOrDefault(member.getUserId(), 0);
            remaining += Math.max(0, member.getMaxTeams() - used);
        }
        return remaining;
    }

    private Set<Long> lockedUserIds(List<TeamSystemLockedTeamRequest> lockedTeams) {
        Set<Long> fixedUserIds = new HashSet<>();
        for (TeamSystemLockedTeamRequest team : lockedTeams) {
            if (team.members() == null) {
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

    private List<MatchTimeSlot> collectTimeSlots(List<MatchMember> members) {
        Set<MatchTimeSlot> unique = new LinkedHashSet<>();
        for (MatchMember member : members) {
            if (member.getAvailableTimeSlots() == null) {
                continue;
            }
            unique.addAll(member.getAvailableTimeSlots());
        }
        if (unique.isEmpty()) {
            return weekdayGrid();
        }
        return sortTimeSlots(unique);
    }

    private List<MatchTimeSlot> sortTimeSlots(Set<MatchTimeSlot> slots) {
        return slots.stream()
                .sorted(Comparator
                        .comparingInt((MatchTimeSlot slot) -> {
                            int index = DAY_ORDER.indexOf(slot.getDayOfWeek());
                            return index < 0 ? Integer.MAX_VALUE : index;
                        })
                        .thenComparing(MatchTimeSlot::getStartTime))
                .toList();
    }

    private List<MatchTimeSlot> weekdayGrid() {
        List<MatchTimeSlot> slots = new ArrayList<>();
        for (String day : DAY_ORDER) {
            for (int hour = 9; hour < 22; hour++) {
                slots.add(new MatchTimeSlot(day, String.format("%02d:00", hour)));
                slots.add(new MatchTimeSlot(day, String.format("%02d:30", hour)));
            }
        }
        return slots;
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
        Set<MatchTimeSlot> slots = new LinkedHashSet<>();
        for (TeamFormScheduleResponse schedule : form.schedules()) {
            if (schedule.dayOfWeek() == null || schedule.startTime() == null) {
                continue;
            }
            slots.add(new MatchTimeSlot(schedule.dayOfWeek(), schedule.startTime()));
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
