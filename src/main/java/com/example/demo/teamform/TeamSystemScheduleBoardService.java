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
import com.example.demo.teamform.dto.TeamSystemScheduleEventResponse;
import com.example.demo.teamform.dto.TeamSystemScheduleTeamResponse;
import com.example.demo.teammember.TeamMember;
import com.example.demo.teammember.TeamMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
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
