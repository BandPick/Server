package com.example.demo.teamform.solver.domain;

import java.util.Objects;

/**
 * Discrete rehearsal block (weekday × start time), e.g. 월 19:00.
 */
public final class MatchTimeSlot {

    private final String dayOfWeek;
    private final String startTime;

    public MatchTimeSlot() {
        this.dayOfWeek = "";
        this.startTime = "";
    }

    public MatchTimeSlot(String dayOfWeek, String startTime) {
        this.dayOfWeek = dayOfWeek == null ? "" : dayOfWeek;
        this.startTime = startTime == null ? "" : startTime;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public String getStartTime() {
        return startTime;
    }

    public String key() {
        return dayOfWeek + "-" + startTime;
    }

    public String display() {
        if (dayOfWeek.isBlank() || startTime.isBlank()) {
            return "";
        }
        return dayOfWeek + " " + startTime;
    }

    public boolean overlaps(MatchTimeSlot other) {
        if (other == null) {
            return false;
        }
        return dayOfWeek.equals(other.dayOfWeek) && startTime.equals(other.startTime);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MatchTimeSlot slot)) {
            return false;
        }
        return dayOfWeek.equals(slot.dayOfWeek) && startTime.equals(slot.startTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dayOfWeek, startTime);
    }

    @Override
    public String toString() {
        return display();
    }
}
