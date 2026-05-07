package com.example.demo.preference;

import jakarta.persistence.*;

@Entity
@Table(name = "form")
public class Preference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "detail_id")
    private Integer detailId;

    @Column(name = "desired_session")
    private Integer desiredSession;

    @Transient
    private String participantNumber;

    public Preference() {
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

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Integer getDetailId() {
        return detailId;
    }

    public void setDetailId(Integer detailId) {
        this.detailId = detailId;
    }

    public Integer getDesiredSession() {
        return desiredSession;
    }

    public void setDesiredSession(Integer desiredSession) {
        this.desiredSession = desiredSession;
    }

    public String getParticipantNumber() {
        return participantNumber;
    }

    public void setParticipantNumber(String participantNumber) {
        this.participantNumber = participantNumber;
    }
}