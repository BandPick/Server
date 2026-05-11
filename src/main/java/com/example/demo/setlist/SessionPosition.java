package com.example.demo.setlist;

public enum SessionPosition {
    V,
    D,
    B,
    EG1,
    EG2,
    AG,
    K1,
    K2,
    기타;

    public static SessionPosition fromLabel(String label) {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("세션 값이 비어 있습니다.");
        }
        for (SessionPosition value : values()) {
            if (value.name().equals(label.trim())) {
                return value;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 세션 값입니다: " + label);
    }
}
