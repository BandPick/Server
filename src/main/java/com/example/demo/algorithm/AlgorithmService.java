package com.example.demo.algorithm;

import com.example.demo.availability.Availability;
import com.example.demo.availability.AvailabilityRepository;
import com.example.demo.common.type.Position;
import com.example.demo.member.MemberRepository;
import com.example.demo.preference.Preference;
import com.example.demo.preference.PreferenceRepository;
import com.example.demo.session.Session;
import com.example.demo.session.SessionRepository;
import com.example.demo.setlist.Setlist;
import com.example.demo.setlist.SetlistRepository;
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
    private final SetlistRepository setlistRepository;
    private final SessionRepository sessionRepository;
    private final PreferenceRepository preferenceRepository;
    private final AvailabilityRepository availabilityRepository;

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ScheduleRepository scheduleRepository;

    public AlgorithmService(MemberRepository memberRepository,
                            SetlistRepository setlistRepository,
                            SessionRepository sessionRepository,
                            PreferenceRepository preferenceRepository,
                            AvailabilityRepository availabilityRepository,
                            TeamRepository teamRepository,
                            TeamMemberRepository teamMemberRepository,
                            ScheduleRepository scheduleRepository) {
        this.memberRepository = memberRepository;
        this.setlistRepository = setlistRepository;
        this.sessionRepository = sessionRepository;
        this.preferenceRepository = preferenceRepository;
        this.availabilityRepository = availabilityRepository;
        this.teamRepository = teamRepository;                   // 추가
        this.teamMemberRepository = teamMemberRepository;       // 추가
        this.scheduleRepository = scheduleRepository;           // 추가
    }

    public RunResult run() {

        // 1. 곡 리스트 (DB Setlist -> songList, songIdToName)
        List<Setlist> songs = setlistRepository.findAll();
        List<String> songList = new ArrayList<>();
        Map<Long, String> songIdToName = new HashMap<>();
        for (Setlist song : songs) {
            String key = song.getTitle() + "_" + song.getArtist();
            songList.add(key);
            songIdToName.put(song.getId(), key);
        }

        // 2. 곡별 필요 세션 (DB Session -> requiredSessions)
        Map<String, List<Position>> requiredSessions = new HashMap<>();
        for (Setlist song : songs) {
            String key = songIdToName.get(song.getId());
            List<Session> sessions = sessionRepository.findBySetlistId(song.getId());
            List<Position> positions = new ArrayList<>();
            for (Session s : sessions) {
                try {
                    positions.add(toPosition(s.getPosition()));
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

            // 선호곡 + 가능 세션 (Preference -> choice, session)
            List<Preference> prefs = preferenceRepository.findByUserId(dbMember.getId());
            for (Preference pref : prefs) {
                String songName = songIdToName.get(resolveSetlistId(pref));
                if (songName != null) {
                    m.choice.put(pref.getPriority(), songName);
                }

                // 지원한 포지션을 session 목록에 추가 (중복 제거)
                try {
                    Position pos = resolvePosition(pref);
                    if (!m.session.contains(pos)) {
                        m.session.add(pos);
                    }
                } catch (Exception ignored) {}
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
        // 임시 추가
        System.out.println("songIdToName: " + songIdToName);
        System.out.println("requiredSessions: " + requiredSessions);
        System.out.println("songMemberList: " + songMemberList);

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
    public void replaceGeneratedResults(Algorithm.AssignmentState state,
                                        List<PracticeSchedule> schedules,
                                        Map<Long, String> songIdToName) {
        scheduleRepository.deleteAllInBatch();
        teamMemberRepository.deleteAllInBatch();
        teamRepository.deleteAllInBatch();

        saveConfirmed(state, songIdToName);
        saveSchedules(state, schedules, songIdToName);
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
                tm.setSessionPosition(toDbPosition(position));
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

        // 알고리즘이 선택한 1시간 합주 시간을 저장
        for (PracticeSchedule ps : schedules) {
            Long songId = songNameToId.get(ps.getSong());
            if (songId == null) continue;

            Integer teamId = songIdToTeamId.get(songId.intValue());
            if (teamId == null) continue;

            Schedule sc = new Schedule();
            sc.setTeamId(teamId);
            sc.setAvailableFrom(LocalDateTime.of(ps.getDate(), ps.getStartTime()));
            sc.setAvailableTo(LocalDateTime.of(ps.getDate(), ps.getEndTime()));
            sc.setStartTime(LocalDateTime.of(ps.getDate(), ps.getStartTime()));
            sc.setEndTime(LocalDateTime.of(ps.getDate(), ps.getEndTime()));
            scheduleRepository.save(sc);
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
    private String toDbPosition(Position position) {
        if (position == null) return null;
        return switch (position) {
            case VOCAL1 -> "V";
            case DRUM -> "D";
            case BASS -> "B";
            case E_GUITAR1 -> "EG1";
            case E_GUITAR2 -> "EG2";
            case A_GUITAR1 -> "AG";
            case KEYBOARD1 -> "K1";
            case KEYBOARD2 -> "K2";
            case CHORUS1 -> "기타";
            default -> position.name(); // 예외적인 경우에만 기본 name() 반환
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
