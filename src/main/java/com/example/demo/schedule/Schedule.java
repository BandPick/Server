package com.example.demo.schedule;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "schedule")
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "team_id")
    private Integer teamId;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "available_from")
    private LocalDateTime availableFrom;

    @Column(name = "available_to")
    private LocalDateTime availableTo;

    public Schedule() {
    }

    public Integer getId() {
        return id;
    }

    public Integer getTeamId() {
        return teamId;
    }

    public void setTeamId(Integer teamId) {
        this.teamId = teamId;
    }

    public LocalDateTime getStartTime() {
        return startTime != null ? startTime : availableFrom;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
        this.availableFrom = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime != null ? endTime : availableTo;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
        this.availableTo = endTime;
    }

    public LocalDateTime getAvailableFrom() {
        return availableFrom;
    }

    public void setAvailableFrom(LocalDateTime availableFrom) {
        this.availableFrom = availableFrom;
        this.startTime = availableFrom;
    }

    public LocalDateTime getAvailableTo() {
        return availableTo;
    }

    public void setAvailableTo(LocalDateTime availableTo) {
        this.availableTo = availableTo;
        this.endTime = availableTo;
    }
}
