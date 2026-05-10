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

import java.time.LocalDate;
import java.util.*;

@Service
public class AlgorithmService {

    private final MemberRepository memberRepository;
    private final SongRepository songRepository;
    private final SessionRepository sessionRepository;
    private final PreferenceRepository preferenceRepository;
    private final AvailabilityRepository availabilityRepository;

    public AlgorithmService(MemberRepository memberRepository,
                            SongRepository songRepository,
                            SessionRepository sessionRepository,
                            PreferenceRepository preferenceRepository,
                            AvailabilityRepository availabilityRepository) {
        this.memberRepository = memberRepository;
        this.songRepository = songRepository;
        this.sessionRepository = sessionRepository;
        this.preferenceRepository = preferenceRepository;
        this.availabilityRepository = availabilityRepository;
    }

    public Algorithm.AssignmentState run() {

        // ① 곡 리스트 (DB Song -> songList, songIdToName)
        List<Song> songs = songRepository.findAll();
        List<String> songList = new ArrayList<>();
        Map<Long, String> songIdToName = new HashMap<>();
        for (Song song : songs) {
            String key = song.getTitle() + "_" + song.getArtist();
            songList.add(key);
            songIdToName.put(song.getId(), key);
        }

        // ② 곡별 필요 세션 (DB Session -> requiredSessions)
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

        // ③ 멤버 리스트 (DB Member -> algorithm Member)
        List<com.example.demo.member.Member> dbMembers = memberRepository.findAll();
        Map<Integer, Member_AL> memberList = new HashMap<>();

        for (com.example.demo.member.Member dbMember : dbMembers) {
            Member_AL m = new Member_AL();
            m.$USER_code = dbMember.getId().intValue();
            m.$USER_name = dbMember.getName();

            // 가능 시간 (Availability -> availableSlots 비트마스크)
            List<Availability> slots = availabilityRepository.findByUserId(dbMember.getId());
            for (Availability av : slots) {
                LocalDate date = av.getAvailableFrom().toLocalDate();
                long bitMask = TimeUtils.createBitmask(
                        av.getAvailableFrom().toLocalTime(),
                        av.getAvailableTo().toLocalTime()
                );
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

        // ④ 곡별 지원 현황 (Preference -> songMemberList)
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

        // ⑤ 알고리즘 실행
        Algorithm algorithm = new Algorithm();
        return algorithm.runStep1(songList, requiredSessions, songMemberList, memberList);
    }

    public List<PracticeSchedule> runStep2(Algorithm.AssignmentState state) {
        Algorithm algorithm = new Algorithm();
        return algorithm.generateSchedules(state);
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
}
