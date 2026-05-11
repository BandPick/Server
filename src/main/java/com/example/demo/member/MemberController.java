package com.example.demo.member;

import com.example.demo.member.dto.MemberRequest;
import com.example.demo.member.dto.MemberResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping
    public ResponseEntity<?> createMember(@RequestBody MemberRequest request) {
        Member createdMember = memberService.createMember(request.toEntity());

        if (createdMember == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("이미 존재하는 participantNumber입니다.");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(MemberResponse.from(createdMember));
    }

    @GetMapping
    public ResponseEntity<List<MemberResponse>> getMembers() {
        return ResponseEntity.ok(
                memberService.getMembers().stream()
                        .map(MemberResponse::from)
                        .toList()
        );
    }

    @GetMapping("/{participantNumber}")
    public ResponseEntity<MemberResponse> getMemberById(@PathVariable String participantNumber) {
        Member member = memberService.getMemberById(participantNumber);

        if (member == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(MemberResponse.from(member));
    }

    @PutMapping("/{memberId}")
    public ResponseEntity<MemberResponse> updateMember(@PathVariable String memberId,
                                                       @RequestBody MemberRequest request) {
        Member member = memberService.updateMember(memberId, request.toEntity());

        if (member == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(MemberResponse.from(member));
    }
}
