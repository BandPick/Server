package com.example.demo.memberform;

import com.example.demo.memberform.dto.MemberFormMatrixSaveRequest;
import com.example.demo.memberform.dto.MemberFormMatrixSaveResponse;
import com.example.demo.memberform.dto.MemberFormMemberResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/forms")
public class MemberFormsController {

    private final MemberFormService memberFormService;

    public MemberFormsController(MemberFormService memberFormService) {
        this.memberFormService = memberFormService;
    }

    @GetMapping
    public List<MemberFormMemberResponse> listAll() {
        return memberFormService.listAll();
    }

    @PutMapping
    public ResponseEntity<?> replaceAllPicks(@Valid @RequestBody MemberFormMatrixSaveRequest request) {
        try {
            MemberFormMatrixSaveResponse response = memberFormService.replaceAllPicks(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }
}
