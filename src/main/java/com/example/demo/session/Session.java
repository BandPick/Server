package com.example.demo.session;

import jakarta.persistence.*;

@Entity
@Table(name = "session")
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "setlist_id")
    private Integer setlistId;

    @Column(name = "position")
    private String position;

    public Session() {
    }

    public Integer getId() {
        return id;
    }

    public Integer getSetlistId() {
        return setlistId;
    }

    public void setSetlistId(Integer setlistId) {
        this.setlistId = setlistId;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }
}