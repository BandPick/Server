package com.example.demo.teamform.solver.domain;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Problem fact: a person available for team-system matching.
 */
public class MatchMember {

    private long userId;
    private String name;
    private int maxTeams;
    private Map<String, String> levels;
    private Map<String, Integer> priorities;
    private Set<String> slots;

    public MatchMember() {
    }

    public MatchMember(
            long userId,
            String name,
            int maxTeams,
            Map<String, String> levels,
            Map<String, Integer> priorities,
            Set<String> slots
    ) {
        this.userId = userId;
        this.name = name;
        this.maxTeams = maxTeams;
        this.levels = levels;
        this.priorities = priorities;
        this.slots = slots;
    }

    public long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public int getMaxTeams() {
        return maxTeams;
    }

    public Map<String, String> getLevels() {
        return levels;
    }

    public Map<String, Integer> getPriorities() {
        return priorities;
    }

    public Set<String> getSlots() {
        return slots;
    }

    public boolean canPlay(String position) {
        return levels != null && levels.containsKey(position);
    }

    public int priorityRank(String position) {
        Integer priority = priorities == null ? null : priorities.get(position);
        if (priority == null || priority <= 0) {
            return 100;
        }
        return priority;
    }

    public String primaryPosition() {
        String best = null;
        int bestRank = Integer.MAX_VALUE;
        if (levels == null) {
            return null;
        }
        for (String position : levels.keySet()) {
            int rank = priorityRank(position);
            if (rank < bestRank) {
                bestRank = rank;
                best = position;
            }
        }
        return best;
    }

    public int levelScore(String position) {
        if (levels == null) {
            return 0;
        }
        String level = levels.get(position);
        if (level == null) {
            return 0;
        }
        return switch (level) {
            case "상" -> 5;
            case "중" -> 3;
            case "하" -> 1;
            default -> 0;
        };
    }

    public int scheduleScarcity() {
        int size = slots == null ? 0 : slots.size();
        return Math.max(0, 40 - size);
    }

    public boolean overlaps(MatchMember other) {
        if (slots == null || other == null || other.slots == null) {
            return false;
        }
        for (String slot : slots) {
            if (other.slots.contains(slot)) {
                return true;
            }
        }
        return false;
    }

    public boolean vocalPreferredOverGuitar(String guitarPosition) {
        return priorityRank("V") < priorityRank(guitarPosition);
    }

    public static Set<String> commonSlots(Iterable<MatchMember> members) {
        Set<String> common = null;
        Set<Long> seen = new HashSet<>();
        for (MatchMember member : members) {
            if (member == null || !seen.add(member.userId)) {
                continue;
            }
            if (common == null) {
                common = new HashSet<>(member.slots == null ? Set.of() : member.slots);
            } else {
                common.retainAll(member.slots == null ? Set.of() : member.slots);
            }
        }
        return common == null ? Set.of() : common;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MatchMember member)) {
            return false;
        }
        return userId == member.userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return name + "(" + userId + ")";
    }
}
