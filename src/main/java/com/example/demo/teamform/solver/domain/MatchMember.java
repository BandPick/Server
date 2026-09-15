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
    private Set<MatchTimeSlot> availableTimeSlots;

    public MatchMember() {
    }

    public MatchMember(
            long userId,
            String name,
            int maxTeams,
            Map<String, String> levels,
            Map<String, Integer> priorities,
            Set<MatchTimeSlot> availableTimeSlots
    ) {
        this.userId = userId;
        this.name = name;
        this.maxTeams = maxTeams;
        this.levels = levels;
        this.priorities = priorities;
        this.availableTimeSlots = availableTimeSlots;
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

    public Set<MatchTimeSlot> getAvailableTimeSlots() {
        return availableTimeSlots;
    }

    public boolean canPlay(String position) {
        return levels != null && levels.containsKey(position);
    }

    public int rankOf(String position) {
        Integer priority = priorities == null ? null : priorities.get(position);
        if (priority == null || priority <= 0) {
            return Integer.MAX_VALUE;
        }
        return priority;
    }

    public int priorityRank(String position) {
        int rank = rankOf(position);
        return rank == Integer.MAX_VALUE ? 100 : rank;
    }

    /**
     * Vocal + instrument double-up is allowed only when vocal outranks the instrument.
     */
    public boolean canDoubleUpVocalWith(String instrument) {
        if (instrument == null || "V".equals(instrument)) {
            return false;
        }
        return rankOf("V") < rankOf(instrument);
    }

    public boolean isAvailableAt(MatchTimeSlot timeSlot) {
        return timeSlot != null
                && availableTimeSlots != null
                && availableTimeSlots.contains(timeSlot);
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

    public static Set<MatchTimeSlot> commonTimeSlots(Iterable<MatchMember> members) {
        Set<MatchTimeSlot> common = null;
        Set<Long> seen = new HashSet<>();
        for (MatchMember member : members) {
            if (member == null || !seen.add(member.userId)) {
                continue;
            }
            Set<MatchTimeSlot> slots = member.availableTimeSlots == null
                    ? Set.of()
                    : member.availableTimeSlots;
            if (common == null) {
                common = new HashSet<>(slots);
            } else {
                common.retainAll(slots);
            }
        }
        return common == null ? Set.of() : common;
    }

    /** Distinct weekdays covered by {@link #commonTimeSlots(Iterable)}. */
    public static int commonDistinctDayCount(Iterable<MatchMember> members) {
        Set<String> days = new HashSet<>();
        for (MatchTimeSlot slot : commonTimeSlots(members)) {
            if (slot != null && slot.getDayOfWeek() != null && !slot.getDayOfWeek().isBlank()) {
                days.add(slot.getDayOfWeek());
            }
        }
        return days.size();
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
