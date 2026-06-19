package com.example.demo.setlist;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/setlists")
public class SetlistController {

    private final SetlistService setlistService;

    public SetlistController(SetlistService setlistService) {
        this.setlistService = setlistService;
    }

    @GetMapping
    public List<SetlistResponse> list() {
        return setlistService.listAll();
    }

    @PostMapping
    public List<SetlistResponse> save(@RequestBody SetlistSaveRequest body) {
        return setlistService.replaceAll(body);
    }
}