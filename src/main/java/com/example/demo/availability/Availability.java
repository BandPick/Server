package com.example.demo.availability;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "schedule_form")
public class Availability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "available_from")
    private LocalDateTime availableFrom;

    @Column(name = "available_to")
    private LocalDateTime availableTo;

    @Transient
    private String participantNumber;

    public Availability() {
    }

    public Integer getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDateTime getAvailableFrom() {
        return availableFrom;
    }

    public void setAvailableFrom(LocalDateTime availableFrom) {
        this.availableFrom = availableFrom;
    }

    public LocalDateTime getAvailableTo() {
        return availableTo;
    }

    public void setAvailableTo(LocalDateTime availableTo) {
        this.availableTo = availableTo;
    }

    public String getParticipantNumber() {
        return participantNumber;
    }

    public void setParticipantNumber(String participantNumber) {
        this.participantNumber = participantNumber;
    }
}