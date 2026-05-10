package com.example.demo.session;

import java.io.Serializable;
import java.util.Objects;

public class SessionId implements Serializable {

    private Long setlistId;
    private String position;
    private String extra;

    public SessionId() {
    }

    public SessionId(Long setlistId, String position, String extra) {
        this.setlistId = setlistId;
        this.position = position;
        this.extra = extra;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SessionId sessionId)) return false;
        return Objects.equals(setlistId, sessionId.setlistId)
                && Objects.equals(position, sessionId.position)
                && Objects.equals(extra, sessionId.extra);
    }

    @Override
    public int hashCode() {
        return Objects.hash(setlistId, position, extra);
    }
}
