package com.example.demo.member;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="code")
    private String participantNumber;

    @Column(name="name")
    private String name;

    public Member() {
    }

    public Long getId() {
        return id;
    }

    public String getParticipantNumber() {
        return participantNumber;
    }

    public void setParticipantNumber(String participantNumber) {
        this.participantNumber = participantNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}