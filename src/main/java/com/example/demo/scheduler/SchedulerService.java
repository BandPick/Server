package com.example.demo.scheduler;

import com.example.demo.schedule.Schedule;
import com.example.demo.schedule.ScheduleService;
import com.example.demo.team.Team;
import com.example.demo.team.TeamService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SchedulerService {

    private final TeamService teamService;
    private final ScheduleService scheduleService;

    public SchedulerService(TeamService teamService, ScheduleService scheduleService) {
        this.teamService = teamService;
        this.scheduleService = scheduleService;
    }

    public List<Team> generateTeams() {
        // TODO: 나중에 팀 자동 매칭 알고리즘 연결
        List<Team> generatedTeams = new ArrayList<>();

        teamService.saveTeams(generatedTeams);
        return generatedTeams;
    }

    public List<Schedule> generateSchedules() {
        // TODO: 나중에 합주 스케줄 자동 생성 알고리즘 연결
        List<Schedule> generatedSchedules = new ArrayList<>();

        scheduleService.saveSchedules(generatedSchedules);
        return generatedSchedules;
    }
}