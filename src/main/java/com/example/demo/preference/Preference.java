package com.example.demo.preference;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnTransformer;

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

    @Column(name = "setlist_id")
    private Integer setlistId;

    @Column(name = "desired_position", columnDefinition = "session_position")
    @ColumnTransformer(write = "?::session_position")
    private String desiredPosition;

    @Column(name = "desired_extra")
    private String desiredExtra = "";

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
        this.setlistId = detailId;
    }

    public Integer getSetlistId() {
        return setlistId;
    }

    public void setSetlistId(Integer setlistId) {
        this.setlistId = setlistId;
        this.detailId = setlistId;
    }

    public Integer getDesiredSession() {
        return desiredSession;
    }

    public void setDesiredSession(Integer desiredSession) {
        this.desiredSession = desiredSession;
    }

    public String getDesiredPosition() {
        return desiredPosition;
    }

    public void setDesiredPosition(String desiredPosition) {
        this.desiredPosition = desiredPosition;
    }

    public String getDesiredExtra() {
        return desiredExtra;
    }

    public void setDesiredExtra(String desiredExtra) {
        this.desiredExtra = desiredExtra;
    }

    public String getParticipantNumber() {
        return participantNumber;
    }

    public void setParticipantNumber(String participantNumber) {
        this.participantNumber = participantNumber;
    }
}
