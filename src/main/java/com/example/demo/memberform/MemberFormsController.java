package com.example.demo.memberform;

import com.example.demo.memberform.dto.MemberFormMemberResponse;
import org.springframework.web.bind.annotation.GetMapping;
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
}
