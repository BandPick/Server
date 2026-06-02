package com.example.demo.memberform.vo;

public record FormPickRow(
        long userId,
        String userName,
        int priority,
        String songTitle,
        String desiredPosition,
        String desiredExtra
) {
}
