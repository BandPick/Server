package com.example.demo.algorithm;

import com.example.demo.availability.Availability;
import com.example.demo.availability.AvailabilityRepository;
import com.example.demo.common.type.Position;
import com.example.demo.member.MemberRepository;
import com.example.demo.preference.Preference;
import com.example.demo.preference.PreferenceRepository;
import com.example.demo.session.Session;
import com.example.demo.session.SessionRepository;
import com.example.demo.song.Song;
import com.example.demo.song.SongRepository;
import org.springframework.stereotype.Service;

import com.example.demo.schedule.Schedule;
import com.example.demo.schedule.ScheduleRepository;
import java.time.LocalDateTime;
import com.example.demo.team.Team;
import com.example.demo.team.TeamRepository;
import com.example.demo.teammember.TeamMember;
import com.example.demo.teammember.TeamMemberRepository;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
public class AlgorithmService {

    private final MemberRepository memberRepository;
    private final SongRepository songRepository;
    private final SessionRepository sessionRepository;
    private final PreferenceRepository preferenceRepository;
    private final AvailabilityRepository availabilityRepository;

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ScheduleRepository scheduleRepository;

    public AlgorithmService(MemberRepository memberRepository,
                            SongRepository songRepository,
                            SessionRepository sessionRepository,
                            PreferenceRepository preferenceRepository,
                            AvailabilityRepository availabilityRepository,
                            TeamRepository teamRepository,
                            TeamMemberRepository teamMemberRepository,
                            ScheduleRepository scheduleRepository) {
        this.memberRepository = memberRepository;
        this.songRepository = songRepository;
        this.sessionRepository = sessionRepository;
        this.preferenceRepository = preferenceRepository;
        this.availabilityRepository = availabilityRepository;
        this.teamRepository = teamRepository;                   // 추가
        this.teamMemberRepository = teamMemberRepository;       // 추가
        this.scheduleRepository = scheduleRepository;           // 추가
    }

    public RunResult run() {

        // 1. 곡 리스트 (DB Song -> songList, songIdToName)
        List<Song> songs = songRepository.findAll();
        List<String> songList = new ArrayList<>();
        Map<Long, String> songIdToName = new HashMap<>();
        for (Song song : songs) {
            String key = song.getTitle() + "_" + song.getArtist();
            songList.add(key);
            songIdToName.put(song.getId(), key);
        }

        // 2. 곡별 필요 세션 (DB Session -> requiredSessions)
        Map<String, List<Position>> requiredSessions = new HashMap<>();
        for (Song song : songs) {
            String key = songIdToName.get(song.getId());
            List<Session> sessions = sessionRepository.findBySetlistId(song.getId());
            List<Position> positions = new ArrayList<>();
            for (Session s : sessions) {
                try {
                    positions.add(Position.valueOf(s.getPosition()));
                } catch (IllegalArgumentException ignored) {}
            }
            requiredSessions.put(key, positions);
        }

        // 3. 멤버 리스트 (DB Member -> algorithm Member)
        List<com.example.demo.member.Member> dbMembers = memberRepository.findAll();
        Map<Integer, Member_AL> memberList = new HashMap<>();

        for (com.example.demo.member.Member dbMember : dbMembers) {
            Member_AL m = new Member_AL();
            m.$USER_code = dbMember.getId().intValue();
            m.$USER_name = dbMember.getName();

            // 가능 시간 (Availability -> availableSlots 비트마스크)
            List<Availability> slots = availabilityRepository.findByUserId(dbMember.getId());
            for (Availability av : slots) {
                LocalDate date = av.getAvailableFrom().toLocalDate(); // 수정 필요할듯
                long bitMask = TimeUtils.createBitmask(
                        av.getAvailableFrom().toLocalTime(),
                        av.getAvailableTo().toLocalTime()
                );
                // 수정 가능하게 merge로 구현
                m.availableSlots.merge(date, bitMask, (a, b) -> a | b);
            }

            // 선호곡 (Preference -> choice)
            List<Preference> prefs = preferenceRepository.findByUserId(dbMember.getId());
            for (Preference pref : prefs) {
                String songName = songIdToName.get(resolveSetlistId(pref));
                if (songName != null) {
                    m.choice.put(pref.getPriority(), songName);
                }
            }

            memberList.put(m.$USER_code, m);
        }

        // 4. 곡별 지원 현황 (Preference -> songMemberList)
        Map<String, List<AssignedSession>> songMemberList = new HashMap<>();
        List<Preference> allPrefs = preferenceRepository.findAll();
        for (Preference pref : allPrefs) {
            String songName = songIdToName.get(resolveSetlistId(pref));
            if (songName == null) continue;

            Member_AL m = memberList.get(pref.getUserId().intValue());
            if (m == null) continue;

            try {
                Position pos = resolvePosition(pref);
                AssignedSession as = new AssignedSession(m, pos);
                songMemberList.computeIfAbsent(songName, k -> new ArrayList<>()).add(as);
            } catch (Exception ignored) {}
        }

        // 5. 알고리즘 실행
        Algorithm algorithm = new Algorithm();
        Algorithm.AssignmentState state = algorithm.runStep1(songList, requiredSessions, songMemberList, memberList);
        //return algorithm.runStep1(songList, requiredSessions, songMemberList, memberList);
        return new RunResult(state, songIdToName);
    }

    public List<PracticeSchedule> runStep2(Algorithm.AssignmentState state) {
        Algorithm algorithm = new Algorithm();
        return algorithm.generateSchedules(state);
    }

    @Transactional
    public void saveConfirmed(Algorithm.AssignmentState state, Map<Long, String> songIdToName) {

        Map<String, Long> songNameToId = new HashMap<>();
        songIdToName.forEach((id, name) -> songNameToId.put(name, id));

        for (Map.Entry<String, Map<Position, Member_AL>> entry : state.confirmed.entrySet()) {
            String songName = entry.getKey();
            if (state.excluded.contains(songName)) continue;

            Long songId = songNameToId.get(songName);
            if (songId == null) continue;

            // Team 저장
            Team team = new Team();
            team.setSetlistId(songId.intValue());
            teamRepository.save(team);

            // TeamMember 저장
            for (Map.Entry<Position, Member_AL> memberEntry : entry.getValue().entrySet()) {
                Position position = memberEntry.getKey();
                Member_AL member = memberEntry.getValue();

                TeamMember tm = new TeamMember();
                tm.setTeamId(team.getId());
                tm.setUserId(member.$USER_code.longValue());
                tm.setSessionPosition(position.name());  // Position -> String
                teamMemberRepository.save(tm);
            }
        }
    }

    @Transactional
    public void saveSchedules(Algorithm.AssignmentState state,
                              List<PracticeSchedule> schedules,
                              Map<Long, String> songIdToName) {

        // 곡명 -> songId 역방향
        Map<String, Long> songNameToId = new HashMap<>();
        songIdToName.forEach((id, name) -> songNameToId.put(name, id));

        // 방금 저장된 team 목록 (setlist_id -> team_id)
        Map<Integer, Integer> songIdToTeamId = new HashMap<>();
        teamRepository.findAll().forEach(t ->
                songIdToTeamId.put(t.getSetlistId(), t.getId())
        );

        // 곡별로 처리
        for (String songName : state.confirmed.keySet()) {
            if (state.excluded.contains(songName)) continue;

            Long songId = songNameToId.get(songName);
            if (songId == null) continue;

            Integer teamId = songIdToTeamId.get(songId.intValue());
            if (teamId == null) continue;

            // ① 팀 전체 공통 가능 시간 저장 (available_from/to)
            List<Member_AL> teamMembers = new ArrayList<>(state.confirmed.get(songName).values());
            Map<LocalDate, Long> commonTimes = new Algorithm().computeCommonTime(teamMembers);

            for (Map.Entry<LocalDate, Long> entry : commonTimes.entrySet()) {
                LocalDate date = entry.getKey();
                long mask = entry.getValue();

                // 연속 구간 전부 저장
                int start = -1;
                for (int i = 0; i < 48; i++) {
                    boolean bitOn = (mask & (1L << i)) != 0;
                    if (bitOn && start == -1) {
                        start = i;  // 구간 시작
                    } else if (!bitOn && start != -1) {
                        // 구간 종료 -> 저장
                        Schedule sc = new Schedule();
                        sc.setTeamId(teamId);
                        sc.setAvailableFrom(LocalDateTime.of(date, TimeUtils.indexToTime(start)));
                        sc.setAvailableTo(LocalDateTime.of(date, TimeUtils.indexToTime(i)));
                        scheduleRepository.save(sc);
                        start = -1;
                    }
                }
                // 마지막 구간 처리
                if (start != -1) {
                    Schedule sc = new Schedule();
                    sc.setTeamId(teamId);
                    sc.setAvailableFrom(LocalDateTime.of(date, TimeUtils.indexToTime(start)));
                    sc.setAvailableTo(LocalDateTime.of(date, TimeUtils.indexToTime(48)));
                    scheduleRepository.save(sc);
                }
            }
        }

        // ② 알고리즘이 선택한 합주 시간 저장 (start_time/end_time)
        // 이미 저장된 schedule 중 해당 팀+날짜 찾아서 start/end 업데이트
        for (PracticeSchedule ps : schedules) {
            Long songId = songNameToId.get(ps.getSong());
            if (songId == null) continue;

            Integer teamId = songIdToTeamId.get(songId.intValue());
            if (teamId == null) continue;

            // 해당 팀의 같은 날짜 schedule 찾아서 start/end 업데이트
            scheduleRepository.findByTeamId(teamId).stream()
                    .filter(sc -> sc.getAvailableFrom() != null &&
                            sc.getAvailableFrom().toLocalDate().equals(ps.getDate()))
                    .findFirst()
                    .ifPresent(sc -> {
                        sc.setStartTime(LocalDateTime.of(ps.getDate(), ps.getStartTime()));
                        sc.setEndTime(LocalDateTime.of(ps.getDate(), ps.getEndTime()));
                        scheduleRepository.save(sc);
                    });
        }
    }

    private Long resolveSetlistId(Preference preference) {
        Integer setlistId = preference.getSetlistId() != null
                ? preference.getSetlistId()
                : preference.getDetailId();
        return setlistId == null ? null : setlistId.longValue();
    }

    private Position resolvePosition(Preference preference) {
        if (preference.getDesiredPosition() != null) {
            return toPosition(preference.getDesiredPosition());
        }
        return Position.values()[preference.getDesiredSession()];
    }

    private Position toPosition(String dbPosition) {
        return switch (dbPosition) {
            case "V" -> Position.VOCAL1;
            case "D" -> Position.DRUM;
            case "B" -> Position.BASS;
            case "EG1" -> Position.E_GUITAR1;
            case "EG2" -> Position.E_GUITAR2;
            case "AG" -> Position.A_GUITAR1;
            case "K1" -> Position.KEYBOARD1;
            case "K2" -> Position.KEYBOARD2;
            case "\uAE30\uD0C0" -> Position.CHORUS1;
            default -> Position.valueOf(dbPosition);
        };
    }

    public static class RunResult {
        public final Algorithm.AssignmentState state;
        public final Map<Long, String> songIdToName;

        public RunResult(Algorithm.AssignmentState state, Map<Long, String> songIdToName) {
            this.state = state;
            this.songIdToName = songIdToName;
        }
    }
}
