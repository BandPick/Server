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

        return scheduleRepository.save(schedule);
    }

    public List<Schedule> getSchedules() {

        return scheduleRepository.findAll();
    }

    public List<Schedule> getSchedulesByTeamId(Integer teamId) {

        return scheduleRepository.findByTeamId(teamId);
    }

    public Schedule updateSchedule(Integer id,
                                   Schedule updatedSchedule) {

        Schedule schedule =
                scheduleRepository.findById(id).orElse(null);

        if (schedule == null) {
            return null;
        }

        schedule.setTeamId(updatedSchedule.getTeamId());
        schedule.setStartTime(updatedSchedule.getStartTime());
        schedule.setEndTime(updatedSchedule.getEndTime());

        return scheduleRepository.save(schedule);
    }

    public String deleteSchedule(Integer id) {

        Schedule schedule =
                scheduleRepository.findById(id).orElse(null);

        if (schedule == null) {
            return "해당 스케줄을 찾을 수 없습니다.";
        }

        scheduleRepository.delete(schedule);

        return "삭제 완료";
    }
}