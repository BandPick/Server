package com.example.demo.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "settings")
public class Settings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime deadline;

    @Column(name = "min_vocal_songs", nullable = false)
    private Integer minVocalSongs;

    @Column(name = "min_session_songs", nullable = false)
    private Integer minSessionSongs;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    public Settings() {
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public Integer getMinVocalSongs() {
        return minVocalSongs;
    }

    public void setMinVocalSongs(Integer minVocalSongs) {
        this.minVocalSongs = minVocalSongs;
    }

    public Integer getMinSessionSongs() {
        return minSessionSongs;
    }

    public void setMinSessionSongs(Integer minSessionSongs) {
        this.minSessionSongs = minSessionSongs;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
