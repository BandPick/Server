package com.example.demo.auth.dto;

import com.example.demo.auth.vo.LoginUserVo;

public record LoginResponse(
        boolean success,
        String message,
        LoginUserVo user
) {
}
