package com.example.demo.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpsertRequest(
        @NotBlank @Size(max = 20) String code,
        @NotBlank String name
) {
}
