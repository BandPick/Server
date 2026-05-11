package com.example.demo.setlist;

import java.util.List;

public record SetlistResponse(long id, String title, String artist, List<String> positions) {
}