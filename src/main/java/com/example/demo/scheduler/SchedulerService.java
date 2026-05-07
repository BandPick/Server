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

    public SchedulerService(TeamService teamService,
                            ScheduleService scheduleService) {
        this.teamService = teamService;
        this.scheduleService = scheduleService;
    }

    public List<Team> generateTeams() {
        // TODO: 나중에 팀 자동 매칭 알고리즘 연결
        List<Team> generatedTeams = new ArrayList<>();

        // 예: 알고리즘 결과로 Team 객체를 만들면 여기서 DB 저장
        // Team savedTeam = teamService.createTeam(team);
        // generatedTeams.add(savedTeam);

        return generatedTeams;
    }

    public List<Schedule> generateSchedules() {
        // TODO: 나중에 합주 스케줄 자동 생성 알고리즘 연결
        List<Schedule> generatedSchedules = new ArrayList<>();

        // 예: 알고리즘 결과로 Schedule 객체를 만들면 여기서 DB 저장
        // Schedule savedSchedule = scheduleService.createSchedule(schedule);
        // generatedSchedules.add(savedSchedule);

        return generatedSchedules;
    }
}