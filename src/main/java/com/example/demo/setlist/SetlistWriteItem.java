package com.example.demo.setlist;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * 클라이언트 {@code SetlistItem}과 맞춤: {@code id} 등 알 수 없는 필드는 무시.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SetlistWriteItem(
        Long id,
        String title,
        String artist,
        List<String> sessions
) {
}
