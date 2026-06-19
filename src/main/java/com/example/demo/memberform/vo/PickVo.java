package com.example.demo.memberform.vo;

public record PickVo(
        int priority,
        long setlistId,
        String desiredPosition,
        String desiredExtra
) {
}
