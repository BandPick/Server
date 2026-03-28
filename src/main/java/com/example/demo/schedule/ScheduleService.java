package com.example.demo.schedule;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ScheduleService {

    private final List<Schedule> scheduleList = new ArrayList<>();

    public void saveSchedules(List<Schedule> schedules) {
        scheduleList.clear();
        scheduleList.addAll(schedules);
    }

    public List<Schedule> getSchedules() {
        return scheduleList;
    }

    public Schedule getScheduleById(String scheduleId) {
        for (Schedule schedule : scheduleList) {
            if (schedule.getScheduleId().equals(scheduleId)) {
                return schedule;
            }
        }
        return null;
    }

    public Schedule updateSchedule(String scheduleId, ScheduleUpdateRequest request) {
        for (Schedule schedule : scheduleList) {
            if (schedule.getScheduleId().equals(scheduleId)) {
                schedule.setDayOfWeek(request.getDayOfWeek());
                schedule.setStartTime(request.getStartTime());
                schedule.setEndTime(request.getEndTime());
                return schedule;
            }
        }
        return null;
    }
}