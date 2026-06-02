package com.example.demo.algorithm;

import java.time.LocalDate;
import java.time.LocalTime;

public class PracticeSchedule {
    String song;
    LocalDate date;
    LocalTime startTime;
    LocalTime endTime;

    public PracticeSchedule(String song, LocalDate date, LocalTime startTime, LocalTime endTime) {
        this.song = song;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getSong() { return song; }
    public LocalDate getDate() { return date; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
}