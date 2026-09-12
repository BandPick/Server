package com.example.demo.teamform;

import com.example.demo.teamform.dto.TeamFormMemberResponse;
import com.example.demo.teamform.dto.TeamFormPositionResponse;
import com.example.demo.teamform.dto.TeamFormScheduleResponse;
import com.example.demo.teamform.dto.TeamSystemMatchResponse;
import com.example.demo.teamform.dto.TeamSystemTeamMemberResponse;
import com.example.demo.teamform.dto.TeamSystemTeamResponse;
import com.example.demo.teamform.dto.TeamSystemUnmatchedResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TeamSystemMatchService {

    private static final List<String> POSITION_ORDER = List.of("V", "D", "B", "EG1", "EG2", "AG", "K");
    private static final Set<String> CORE_POSITIONS = Set.of("V", "D", "B");
    private static final Set<String> GUITAR_POSITIONS = Set.of("EG1", "EG2", "AG");
    private static final int MIN_UNIQUE_MEMBERS = 3;

    private final TeamFormService teamFormService;

    public TeamSystemMatchService(TeamFormService teamFormService) {
        this.teamFormService = teamFormService;
    }

    public TeamSystemMatchResponse match() {
        List<Member> members = teamFormService.listAll().stream()
                .map(this::toMember)
                .filter(member -> !member.levels.isEmpty())
                .toList();

        Map<Long, Integer> teamCounts = new HashMap<>();
        Set<String> signatures = new HashSet<>();
        List<TeamSystemTeamResponse> teams = new ArrayList<>();
        int maxTeams = Math.max(1, members.size());

        while (teams.size() < maxTeams) {
            List<Member> pool = members.stream()
                    .filter(member -> teamCounts.getOrDefault(member.userId, 0) < member.maxTeams)
                    .toList();
            Assignment assignment = buildBestTeam(pool, teamCounts, signatures);
            if (assignment == null) {
                break;
            }
            if (!signatures.add(signature(assignment))) {
                break;
            }
            teams.add(toTeamResponse(teams.size(), assignment));
            for (Member member : uniqueMembers(assignment.byPosition.values())) {
                teamCounts.merge(member.userId, 1, Integer::sum);
            }
        }

        Set<Long> assigned = teamCounts.keySet();
        List<TeamSystemUnmatchedResponse> unmatched = members.stream()
                .filter(member -> !assigned.contains(member.userId))
                .map(member -> new TeamSystemUnmatchedResponse(
                        member.userId,
                        member.name,
                        "아직 팀에 배정되지 않았습니다."
                ))
                .toList();

        return new TeamSystemMatchResponse(teams, unmatched);
    }

    private Assignment buildBestTeam(
            List<Member> pool,
            Map<Long, Integer> teamCounts,
            Set<String> signatures
    ) {
        if (uniqueMembers(pool).size() < MIN_UNIQUE_MEMBERS) {
            return null;
        }

        Assignment best = null;
        int bestScore = Integer.MIN_VALUE;
        List<Member> seeds = pool.stream()
                .sorted(Comparator
                        .comparingInt((Member member) -> teamCounts.getOrDefault(member.userId, 0))
                        .thenComparing(Comparator
                                .comparingInt((Member member) -> preferredInPool(member, pool).size())
                                .reversed())
                        .thenComparing(Comparator.comparingInt(this::bestSkill).reversed()))
                .limit(Math.min(12, pool.size()))
                .toList();

        for (Member seed : seeds) {
            Assignment candidate = fillFromSeed(seed, pool, teamCounts);
            if (!isViable(candidate)) {
                continue;
            }
            if (signatures.contains(signature(candidate))) {
                continue;
            }
            int score = scoreAssignment(candidate, teamCounts);
            if (score > bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    private Assignment fillFromSeed(Member seed, List<Member> pool, Map<Long, Integer> teamCounts) {
        Assignment assignment = new Assignment();

        List<Member> group = new ArrayList<>();
        group.add(seed);
        for (Member preferred : preferredInPool(seed, pool)) {
            if (group.size() >= POSITION_ORDER.size()) {
                break;
            }
            group.add(preferred);
        }
        assignPositions(group, assignment);
        tryDualVocalGuitar(assignment);
        fillMissing(assignment, pool, teamCounts, true);
        tryDualVocalGuitar(assignment);
        fillMissing(assignment, pool, teamCounts, false);
        tryDualVocalGuitar(assignment);
        return assignment;
    }

    private void assignPositions(List<Member> group, Assignment assignment) {
        record Option(Member member, String position, int score) {
        }
        List<Option> options = new ArrayList<>();
        for (Member member : group) {
            for (Map.Entry<String, String> entry : member.levels.entrySet()) {
                options.add(new Option(
                        member,
                        entry.getKey(),
                        optionScore(entry.getKey(), entry.getValue())
                ));
            }
        }
        options.sort(Comparator.comparingInt(Option::score).reversed());
        for (Option option : options) {
            if (!canTakePosition(option.member, option.position, assignment)) {
                continue;
            }
            assignment.byPosition.put(option.position, option.member);
        }
    }

    private void fillMissing(
            Assignment assignment,
            List<Member> pool,
            Map<Long, Integer> teamCounts,
            boolean coreOnly
    ) {
        List<String> needed = new ArrayList<>();
        if (coreOnly) {
            needed.addAll(List.of("V", "D", "B"));
            if (!hasGuitar(assignment)) {
                needed.addAll(List.of("EG1", "EG2", "AG"));
            }
        } else {
            needed.addAll(POSITION_ORDER);
        }

        for (String position : needed) {
            if (assignment.byPosition.containsKey(position)) {
                continue;
            }
            if (coreOnly && GUITAR_POSITIONS.contains(position) && hasGuitar(assignment)) {
                continue;
            }

            Member best = null;
            int bestScore = Integer.MIN_VALUE;
            for (Member member : pool) {
                if (!canTakePosition(member, position, assignment)) {
                    continue;
                }
                int score = candidateScore(member, position, assignment, teamCounts);
                boolean optionalFill = !coreOnly
                        && commonSlots(assignment, member).isEmpty()
                        && uniqueMembers(assignment.byPosition.values()).size() >= MIN_UNIQUE_MEMBERS;
                if (optionalFill) {
                    continue;
                }
                if (score > bestScore) {
                    best = member;
                    bestScore = score;
                }
            }
            if (best == null) {
                continue;
            }
            assignment.byPosition.put(position, best);
        }
    }

    private void tryDualVocalGuitar(Assignment assignment) {
        Member vocal = assignment.byPosition.get("V");
        if (vocal != null && !hasGuitar(assignment)) {
            for (String guitar : List.of("EG1", "EG2", "AG")) {
                if (canTakePosition(vocal, guitar, assignment)) {
                    assignment.byPosition.put(guitar, vocal);
                    return;
                }
            }
        }
        if (hasGuitar(assignment) && !assignment.byPosition.containsKey("V")) {
            for (String guitar : List.of("EG1", "EG2", "AG")) {
                Member guitarist = assignment.byPosition.get(guitar);
                if (guitarist != null && canTakePosition(guitarist, "V", assignment)) {
                    assignment.byPosition.put("V", guitarist);
                    return;
                }
            }
        }
    }

    private boolean canTakePosition(Member member, String position, Assignment assignment) {
        if (!member.levels.containsKey(position) || assignment.byPosition.containsKey(position)) {
            return false;
        }
        Set<String> current = positionsOf(member, assignment);
        if (current.isEmpty()) {
            return true;
        }
        if (current.size() >= 2) {
            return false;
        }
        String existing = current.iterator().next();
        return isVocalGuitarPair(existing, position);
    }

    private boolean isVocalGuitarPair(String left, String right) {
        return ("V".equals(left) && GUITAR_POSITIONS.contains(right))
                || ("V".equals(right) && GUITAR_POSITIONS.contains(left));
    }

    private Set<String> positionsOf(Member member, Assignment assignment) {
        Set<String> positions = new HashSet<>();
        for (Map.Entry<String, Member> entry : assignment.byPosition.entrySet()) {
            if (entry.getValue().userId == member.userId) {
                positions.add(entry.getKey());
            }
        }
        return positions;
    }

    private boolean isViable(Assignment assignment) {
        if (!CORE_POSITIONS.stream().allMatch(assignment.byPosition::containsKey) || !hasGuitar(assignment)) {
            return false;
        }
        int uniqueCount = uniqueMembers(assignment.byPosition.values()).size();
        if (hasDualVocalGuitar(assignment)) {
            return uniqueCount >= MIN_UNIQUE_MEMBERS;
        }
        return uniqueCount >= 4;
    }

    private boolean hasDualVocalGuitar(Assignment assignment) {
        Member vocal = assignment.byPosition.get("V");
        if (vocal == null) {
            return false;
        }
        for (String guitar : GUITAR_POSITIONS) {
            Member guitarist = assignment.byPosition.get(guitar);
            if (guitarist != null && guitarist.userId == vocal.userId) {
                return true;
            }
        }
        return false;
    }

    private boolean hasGuitar(Assignment assignment) {
        return assignment.byPosition.keySet().stream().anyMatch(GUITAR_POSITIONS::contains);
    }

    private int scoreAssignment(Assignment assignment, Map<Long, Integer> teamCounts) {
        int score = 0;
        List<Member> members = uniqueMembers(assignment.byPosition.values());
        for (Map.Entry<String, Member> entry : assignment.byPosition.entrySet()) {
            score += levelScore(entry.getValue().levels.get(entry.getKey())) * 8;
        }
        score += commonSlots(members).size() * 4;
        score += preferenceHits(members) * 25;
        score += assignment.byPosition.size();
        if (hasDualVocalGuitar(assignment)) {
            score += 12;
        }
        for (Member member : members) {
            score -= teamCounts.getOrDefault(member.userId, 0) * 35;
        }
        return score;
    }

    private int candidateScore(
            Member member,
            String position,
            Assignment assignment,
            Map<Long, Integer> teamCounts
    ) {
        int score = levelScore(member.levels.get(position)) * 8;
        score += commonSlots(assignment, member).size() * 3;
        List<Member> current = uniqueMembers(assignment.byPosition.values());
        if (current.stream().noneMatch(item -> item.userId == member.userId)) {
            current.add(member);
        }
        score += preferenceHits(current) * 20;
        score -= teamCounts.getOrDefault(member.userId, 0) * 35;
        if (!positionsOf(member, assignment).isEmpty() && isVocalGuitarPair(
                positionsOf(member, assignment).iterator().next(),
                position
        )) {
            score += 18;
        }
        return score;
    }

    private int optionScore(String position, String level) {
        int score = levelScore(level) * 10;
        if (CORE_POSITIONS.contains(position)) {
            score += 40;
        } else if (GUITAR_POSITIONS.contains(position)) {
            score += 25;
        }
        return score;
    }

    private int bestSkill(Member member) {
        return member.levels.values().stream().mapToInt(this::levelScore).max().orElse(0);
    }

    private int levelScore(String level) {
        if (level == null) {
            return 0;
        }
        return switch (level) {
            case "상" -> 4;
            case "중" -> 3;
            case "하" -> 2;
            case "도전해보고싶음" -> 1;
            default -> 0;
        };
    }

    private List<Member> preferredInPool(Member member, List<Member> pool) {
        List<Member> preferred = new ArrayList<>();
        for (Member other : pool) {
            if (other.userId == member.userId) {
                continue;
            }
            if (namesMatch(member.preferredNames, other.name) || namesMatch(other.preferredNames, member.name)) {
                preferred.add(other);
            }
        }
        return preferred;
    }

    private boolean namesMatch(List<String> names, String memberName) {
        String target = memberName == null ? "" : memberName.trim();
        if (target.isEmpty()) {
            return false;
        }
        for (String name : names) {
            if (target.equalsIgnoreCase(name) || target.contains(name) || name.contains(target)) {
                return true;
            }
        }
        return false;
    }

    private int preferenceHits(List<Member> members) {
        int hits = 0;
        for (Member member : members) {
            for (Member other : members) {
                if (member.userId == other.userId) {
                    continue;
                }
                if (namesMatch(member.preferredNames, other.name)) {
                    hits += 1;
                }
            }
        }
        return hits;
    }

    private Set<String> commonSlots(Assignment assignment, Member extra) {
        List<Member> members = uniqueMembers(assignment.byPosition.values());
        if (members.stream().noneMatch(member -> member.userId == extra.userId)) {
            members.add(extra);
        }
        return commonSlots(members);
    }

    private Set<String> commonSlots(List<Member> members) {
        Set<String> common = null;
        for (Member member : uniqueMembers(members)) {
            if (common == null) {
                common = new HashSet<>(member.slots);
            } else {
                common.retainAll(member.slots);
            }
        }
        return common == null ? Set.of() : common;
    }

    private List<Member> uniqueMembers(Collection<Member> members) {
        Map<Long, Member> unique = new LinkedHashMap<>();
        for (Member member : members) {
            unique.putIfAbsent(member.userId, member);
        }
        return new ArrayList<>(unique.values());
    }

    private String signature(Assignment assignment) {
        return assignment.byPosition.entrySet().stream()
                .sorted(Comparator.comparingInt(entry -> POSITION_ORDER.indexOf(entry.getKey())))
                .map(entry -> entry.getKey() + "=" + entry.getValue().userId)
                .reduce((left, right) -> left + "|" + right)
                .orElse("");
    }

    private TeamSystemTeamResponse toTeamResponse(int index, Assignment assignment) {
        List<TeamSystemTeamMemberResponse> members = assignment.byPosition.entrySet().stream()
                .sorted(Comparator.comparingInt(entry -> POSITION_ORDER.indexOf(entry.getKey())))
                .map(entry -> new TeamSystemTeamMemberResponse(
                        entry.getValue().userId,
                        entry.getKey(),
                        entry.getValue().name,
                        entry.getValue().levels.getOrDefault(entry.getKey(), "")
                ))
                .toList();

        List<Member> assigned = uniqueMembers(assignment.byPosition.values());
        int common = commonSlots(assigned).size();
        int preferences = preferenceHits(assigned);
        StringBuilder note = new StringBuilder("공통 가능 시간 " + common + "칸");
        if (preferences > 0) {
            note.append(" · 희망 팀원 반영");
        }
        if (hasDualVocalGuitar(assignment)) {
            note.append(" · 보컬·기타 겸임");
        }

        return new TeamSystemTeamResponse(
                teamName(index),
                "완료",
                note.toString(),
                members
        );
    }

    private String teamName(int index) {
        if (index < 26) {
            return (char) ('A' + index) + "팀";
        }
        return (index + 1) + "팀";
    }

    private Member toMember(TeamFormMemberResponse form) {
        Map<String, String> levels = new LinkedHashMap<>();
        for (TeamFormPositionResponse position : form.positions()) {
            if (position.position() == null || position.position().isBlank()) {
                continue;
            }
            levels.put(position.position(), position.level() == null ? "" : position.level());
        }
        Set<String> slots = new HashSet<>();
        for (TeamFormScheduleResponse schedule : form.schedules()) {
            if (schedule.dayOfWeek() == null || schedule.startTime() == null) {
                continue;
            }
            slots.add(schedule.dayOfWeek() + "-" + schedule.startTime());
        }
        return new Member(
                form.userId(),
                form.name(),
                parsePreferredNames(form.teammates()),
                Math.max(1, Math.min(3, form.maxTeams())),
                levels,
                slots
        );
    }

    private List<String> parsePreferredNames(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (String part : raw.split("[,\\n;/]+")) {
            for (String token : part.trim().split("\\s+")) {
                String name = token.trim();
                if (name.length() >= 2) {
                    names.add(name);
                }
            }
        }
        return names;
    }

    private static final class Assignment {
        private final Map<String, Member> byPosition = new LinkedHashMap<>();
    }

    private static final class Member {
        private final long userId;
        private final String name;
        private final List<String> preferredNames;
        private final int maxTeams;
        private final Map<String, String> levels;
        private final Set<String> slots;

        private Member(
                long userId,
                String name,
                List<String> preferredNames,
                int maxTeams,
                Map<String, String> levels,
                Set<String> slots
        ) {
            this.userId = userId;
            this.name = name;
            this.preferredNames = preferredNames;
            this.maxTeams = maxTeams;
            this.levels = levels;
            this.slots = slots;
        }
    }
}
