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

    /** Minutes from midnight for {@link #startTime}, or -1 if unparsable. */
    public int startMinutes() {
        if (startTime == null || startTime.length() < 4) {
            return -1;
        }
        String[] parts = startTime.split(":");
        if (parts.length < 2) {
            return -1;
        }
        try {
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    /** Morning 09:00–11:30 or lunch 12:00–13:30. */
    public boolean isMorningOrLunch() {
        int minutes = startMinutes();
        if (minutes < 0) {
            return false;
        }
        return (minutes >= 9 * 60 && minutes <= 11 * 60 + 30)
                || (minutes >= 12 * 60 && minutes <= 13 * 60 + 30);
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
