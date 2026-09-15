package com.example.demo.teamform;

import com.example.demo.auth.dao.UserDao;
import com.example.demo.auth.entity.UserEntity;
import com.example.demo.schedule.Schedule;
import com.example.demo.schedule.ScheduleRepository;
import com.example.demo.team.Team;
import com.example.demo.team.TeamRepository;
import com.example.demo.teamform.dto.TeamFormMemberResponse;
import com.example.demo.teamform.dto.TeamFormScheduleResponse;
import com.example.demo.teamform.dto.TeamSystemScheduleBoardResponse;
import com.example.demo.teamform.dto.TeamSystemScheduleBoardSaveEventRequest;
import com.example.demo.teamform.dto.TeamSystemScheduleBoardSaveRequest;
import com.example.demo.teamform.dto.TeamSystemScheduleBoardSaveResponse;
import com.example.demo.teamform.dto.TeamSystemScheduleEventResponse;
import com.example.demo.teamform.dto.TeamSystemScheduleTeamResponse;
import com.example.demo.teammember.TeamMember;
import com.example.demo.teammember.TeamMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class TeamSystemScheduleBoardService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final Map<String, String> DAY_TO_KEY = Map.of(
            "월", "mon",
            "화", "tue",
            "수", "wed",
            "목", "thu",
            "금", "fri"
    );
    private static final Map<DayOfWeek, String> DOW_TO_KEY = Map.of(
            DayOfWeek.MONDAY, "mon",
            DayOfWeek.TUESDAY, "tue",
            DayOfWeek.WEDNESDAY, "wed",
            DayOfWeek.THURSDAY, "thu",
            DayOfWeek.FRIDAY, "fri"
    );
    private static final Map<String, Integer> KEY_TO_OFFSET = Map.of(
            "mon", 0,
            "tue", 1,
            "wed", 2,
            "thu", 3,
            "fri", 4
    );

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ScheduleRepository scheduleRepository;
    private final TeamFormService teamFormService;
    private final UserDao userDao;

    public TeamSystemScheduleBoardService(
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository,
            ScheduleRepository scheduleRepository,
            TeamFormService teamFormService,
            UserDao userDao
    ) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.scheduleRepository = scheduleRepository;
        this.teamFormService = teamFormService;
        this.userDao = userDao;
    }

    @Transactional(readOnly = true)
    public TeamSystemScheduleBoardResponse loadBoard() {
        List<Team> teams = teamRepository.findByTeamTypeOrderByNameAscIdAsc(Team.TYPE_TEAM_SYSTEM);
        Map<Long, TeamFormMemberResponse> formsByUserId = new HashMap<>();
        for (TeamFormMemberResponse form : teamFormService.listAll()) {
            formsByUserId.put(form.userId(), form);
        }
        Map<Long, String> userNames = new HashMap<>();
        for (UserEntity user : userDao.findAll()) {
            userNames.put(user.getId(), user.getName());
        }

        List<TeamSystemScheduleTeamResponse> teamResponses = new ArrayList<>();
        List<TeamSystemScheduleEventResponse> events = new ArrayList<>();
        int syntheticEventId = 1;

        for (Team team : teams) {
            List<TeamMember> members = teamMemberRepository.findByTeamId(team.getId());
            LinkedHashSet<Long> uniqueUserIds = new LinkedHashSet<>();
            List<String> memberNames = new ArrayList<>();

            for (TeamMember member : members) {
                Long userId = member.getUserId();
                if (userId == null || !uniqueUserIds.add(userId)) {
                    continue;
                }
                TeamFormMemberResponse form = formsByUserId.get(userId);
                String name = form != null && form.name() != null && !form.name().isBlank()
                        ? form.name()
                        : userNames.getOrDefault(userId, "알 수 없음");
                memberNames.add(name);
            }

            if (memberNames.isEmpty()) {
                continue;
            }

            teamResponses.add(new TeamSystemScheduleTeamResponse(
                    team.getId(),
                    team.getName() == null ? "" : team.getName(),
                    team.isConfirmed(),
                    memberNames
            ));

            List<Schedule> savedSchedules = scheduleRepository.findByTeamId(team.getId());
            if (!savedSchedules.isEmpty()) {
                for (Schedule schedule : savedSchedules) {
                    TeamSystemScheduleEventResponse event = toEventFromSchedule(
                            syntheticEventId++,
                            team,
                            memberNames,
                            schedule
                    );
                    if (event != null) {
                        events.add(event);
                    }
                }
                continue;
            }

            List<String> commonKeys = commonScheduleKeys(uniqueUserIds, formsByUserId);
            for (Range range : groupRanges(commonKeys)) {
                events.add(new TeamSystemScheduleEventResponse(
                        syntheticEventId++,
                        null,
                        team.getId(),
                        team.getName() == null ? "" : team.getName(),
                        range.dayKey(),
                        range.startTime(),
                        range.endTime(),
                        memberNames,
                        "공통 가능 시간"
                ));
            }
        }

        return new TeamSystemScheduleBoardResponse(teamResponses, events);
    }

    @Transactional
    public TeamSystemScheduleBoardSaveResponse saveBoard(TeamSystemScheduleBoardSaveRequest request) {
        if (request == null || request.weekStartDate() == null) {
            throw new IllegalArgumentException("주 시작일(weekStartDate)이 필요합니다.");
        }
        if (request.weekStartDate().getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new IllegalArgumentException("weekStartDate는 월요일이어야 합니다.");
        }

        List<TeamSystemScheduleBoardSaveEventRequest> items =
                request.events() == null ? List.of() : request.events();

        Map<Integer, List<TeamSystemScheduleBoardSaveEventRequest>> byTeam = new LinkedHashMap<>();
        for (TeamSystemScheduleBoardSaveEventRequest item : items) {
            if (item == null || item.teamId() == null) {
                throw new IllegalArgumentException("teamId가 필요합니다.");
            }
            String day = normalizeDayKey(item.day());
            LocalTime start = parseTime(item.startTime());
            LocalTime end = parseTime(item.endTime());
            if (day == null || start == null || end == null) {
                throw new IllegalArgumentException("요일/시작/종료 시간이 올바르지 않습니다.");
            }
            if (!end.isAfter(start)) {
                throw new IllegalArgumentException("종료 시간은 시작 시간보다 뒤여야 합니다.");
            }
            byTeam.computeIfAbsent(item.teamId(), ignored -> new ArrayList<>()).add(
                    new TeamSystemScheduleBoardSaveEventRequest(
                            item.teamId(),
                            day,
                            TIME_FORMAT.format(start),
                            TIME_FORMAT.format(end)
                    )
            );
        }

        LocalDate weekStart = request.weekStartDate();
        LocalDate weekEnd = weekStart.plusDays(4);
        Set<Integer> validTeamIds = new HashSet<>();
        for (Team team : teamRepository.findByTeamTypeOrderByNameAscIdAsc(Team.TYPE_TEAM_SYSTEM)) {
            if (team.getId() != null) {
                validTeamIds.add(team.getId());
            }
        }

        int savedEventCount = 0;
        for (Map.Entry<Integer, List<TeamSystemScheduleBoardSaveEventRequest>> entry : byTeam.entrySet()) {
            Integer teamId = entry.getKey();
            if (!validTeamIds.contains(teamId)) {
                throw new IllegalArgumentException("팀제 팀이 아닙니다: " + teamId);
            }

            List<Schedule> existing = scheduleRepository.findByTeamId(teamId);
            for (Schedule schedule : existing) {
                LocalDateTime start = schedule.getStartTime();
                if (start == null) {
                    continue;
                }
                LocalDate date = start.toLocalDate();
                if (!date.isBefore(weekStart) && !date.isAfter(weekEnd)) {
                    scheduleRepository.delete(schedule);
                }
            }

            for (TeamSystemScheduleBoardSaveEventRequest item : entry.getValue()) {
                Integer offset = KEY_TO_OFFSET.get(item.day());
                if (offset == null) {
                    continue;
                }
                LocalDate date = weekStart.plusDays(offset);
                LocalTime start = LocalTime.parse(item.startTime(), TIME_FORMAT);
                LocalTime end = LocalTime.parse(item.endTime(), TIME_FORMAT);

                Schedule schedule = new Schedule();
                schedule.setTeamId(teamId);
                schedule.setStartTime(LocalDateTime.of(date, start));
                schedule.setEndTime(LocalDateTime.of(date, end));
                scheduleRepository.save(schedule);
                savedEventCount += 1;
            }
        }

        TeamSystemScheduleBoardResponse board = loadBoard();
        return new TeamSystemScheduleBoardSaveResponse(
                byTeam.size(),
                savedEventCount,
                "합주 스케줄을 저장했습니다.",
                board
        );
    }

    private TeamSystemScheduleEventResponse toEventFromSchedule(
            int eventId,
            Team team,
            List<String> memberNames,
            Schedule schedule
    ) {
        LocalDateTime start = schedule.getStartTime();
        LocalDateTime end = schedule.getEndTime();
        if (start == null || end == null) {
            return null;
        }
        String dayKey = DOW_TO_KEY.get(start.getDayOfWeek());
        if (dayKey == null) {
            return null;
        }
        return new TeamSystemScheduleEventResponse(
                eventId,
                schedule.getId(),
                team.getId(),
                team.getName() == null ? "" : team.getName(),
                dayKey,
                TIME_FORMAT.format(start.toLocalTime()),
                TIME_FORMAT.format(end.toLocalTime()),
                memberNames,
                "확정 합주"
        );
    }

    private List<String> commonScheduleKeys(
            Set<Long> userIds,
            Map<Long, TeamFormMemberResponse> formsByUserId
    ) {
        Set<String> common = null;
        for (Long userId : userIds) {
            TeamFormMemberResponse form = formsByUserId.get(userId);
            Set<String> keys = new HashSet<>();
            if (form != null && form.schedules() != null) {
                for (TeamFormScheduleResponse schedule : form.schedules()) {
                    String dayKey = DAY_TO_KEY.get(String.valueOf(schedule.dayOfWeek()).trim());
                    String time = normalizeTime(schedule.startTime());
                    if (dayKey == null || time == null) {
                        continue;
                    }
                    keys.add(dayKey + "|" + time);
                }
            }
            if (common == null) {
                common = keys;
            } else {
                common.retainAll(keys);
            }
        }
        if (common == null || common.isEmpty()) {
            return List.of();
        }
        return common.stream().sorted().toList();
    }

    private List<Range> groupRanges(List<String> keys) {
        Map<String, List<LocalTime>> timesByDay = new LinkedHashMap<>();
        for (String key : keys) {
            String[] parts = key.split("\\|", 2);
            if (parts.length != 2) {
                continue;
            }
            LocalTime time = LocalTime.parse(parts[1], TIME_FORMAT);
            timesByDay.computeIfAbsent(parts[0], ignored -> new ArrayList<>()).add(time);
        }

        List<Range> ranges = new ArrayList<>();
        for (Map.Entry<String, List<LocalTime>> entry : timesByDay.entrySet()) {
            List<LocalTime> times = entry.getValue().stream()
                    .distinct()
                    .sorted()
                    .toList();
            if (times.isEmpty()) {
                continue;
            }
            LocalTime start = times.get(0);
            LocalTime previous = times.get(0);
            for (int i = 1; i < times.size(); i += 1) {
                LocalTime current = times.get(i);
                if (Objects.equals(previous.plusMinutes(30), current)) {
                    previous = current;
                    continue;
                }
                ranges.add(new Range(
                        entry.getKey(),
                        TIME_FORMAT.format(start),
                        TIME_FORMAT.format(previous.plusMinutes(30))
                ));
                start = current;
                previous = current;
            }
            ranges.add(new Range(
                    entry.getKey(),
                    TIME_FORMAT.format(start),
                    TIME_FORMAT.format(previous.plusMinutes(30))
            ));
        }

        ranges.sort(Comparator
                .comparing(Range::dayKey)
                .thenComparing(Range::startTime));
        return ranges;
    }

    private String normalizeDayKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String key = raw.trim().toLowerCase();
        return KEY_TO_OFFSET.containsKey(key) ? key : null;
    }

    private LocalTime parseTime(String raw) {
        String time = normalizeTime(raw);
        if (time == null) {
            return null;
        }
        try {
            return LocalTime.parse(time, TIME_FORMAT);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String normalizeTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String time = raw.trim();
        if (time.length() >= 5) {
            return time.substring(0, 5);
        }
        return null;
    }

    private record Range(String dayKey, String startTime, String endTime) {
    }
}
