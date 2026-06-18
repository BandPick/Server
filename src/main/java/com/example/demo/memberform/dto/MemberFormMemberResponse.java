package com.example.demo.memberform.dto;

import java.util.List;

public record MemberFormMemberResponse(
        long userId,
        String name,
        List<String> picks
) {
}
