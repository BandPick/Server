package com.example.demo.availability;

import com.example.demo.common.type.AvailabilityType;
import com.example.demo.common.type.DayOfWeekType;

public class Availability {

    private String availabilityId;
    private String participantNumber;
    private AvailabilityType type;
    private String weekNumber;
    private DayOfWeekType dayOfWeek;
    private String startTime;
    private String endTime;

    public Availability() {
    }

    public Availability(String availabilityId, String participantNumber, AvailabilityType type, String weekNumber,
                        DayOfWeekType dayOfWeek, String startTime, String endTime) {
        this.availabilityId = availabilityId;
        this.participantNumber = participantNumber;
        this.type = type;
        this.weekNumber = weekNumber;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getAvailabilityId() {
        return availabilityId;
    }

    public void setAvailabilityId(String availabilityId) {
        this.availabilityId = availabilityId;
    }

    public String getParticipantNumber() {
        return participantNumber;
    }

    public void setParticipantNumber(String participantNumber) {
        this.participantNumber = participantNumber;
    }

    public AvailabilityType getType() {
        return type;
    }

    public void setType(AvailabilityType type) {
        this.type = type;
    }

    public String getWeekNumber() {
        return weekNumber;
    }

    public void setWeekNumber(String weekNumber) {
        this.weekNumber = weekNumber;
    }

    public DayOfWeekType getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(DayOfWeekType dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
}