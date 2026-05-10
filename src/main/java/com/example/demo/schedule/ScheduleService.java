package com.example.demo.schedule;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;

    public ScheduleService(ScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    public Schedule createSchedule(Schedule schedule) {
        normalize(schedule);
        return scheduleRepository.save(schedule);
    }

    public List<Schedule> getSchedules() {
        return scheduleRepository.findAll();
    }

    public List<Schedule> getSchedulesByTeamId(Integer teamId) {
        return scheduleRepository.findByTeamId(teamId);
    }

    public Schedule updateSchedule(Integer id, Schedule updatedSchedule) {
        Schedule schedule = scheduleRepository.findById(id).orElse(null);

        if (schedule == null) {
            return null;
        }

        normalize(updatedSchedule);
        schedule.setTeamId(updatedSchedule.getTeamId());
        schedule.setStartTime(updatedSchedule.getStartTime());
        schedule.setEndTime(updatedSchedule.getEndTime());
        schedule.setAvailableFrom(updatedSchedule.getAvailableFrom());
        schedule.setAvailableTo(updatedSchedule.getAvailableTo());
        normalize(schedule);

        return scheduleRepository.save(schedule);
    }

    public String deleteSchedule(Integer id) {
        Schedule schedule = scheduleRepository.findById(id).orElse(null);

        if (schedule == null) {
            return "Schedule not found.";
        }

        scheduleRepository.delete(schedule);
        return "Deleted.";
    }

    private void normalize(Schedule schedule) {
        if (schedule.getAvailableFrom() == null) {
            schedule.setAvailableFrom(schedule.getStartTime());
        }
        if (schedule.getAvailableTo() == null) {
            schedule.setAvailableTo(schedule.getEndTime());
        }
    }
}
