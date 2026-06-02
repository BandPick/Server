package com.example.demo.memberform;

import com.example.demo.memberform.dto.MemberFormSaveRequest;
import com.example.demo.memberform.dto.MemberFormSaveResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class MemberFormController {

    private final MemberFormService memberFormService;

    public MemberFormController(MemberFormService memberFormService) {
        this.memberFormService = memberFormService;
    }

    @PostMapping("/{userId}/forms")
    public ResponseEntity<?> saveMemberForm(
            @PathVariable long userId,
            @Valid @RequestBody MemberFormSaveRequest request
    ) {
        try {
            MemberFormSaveResponse response = memberFormService.save(userId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }
}
