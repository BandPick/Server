package com.example.demo.algorithm;

import com.example.demo.common.type.Position;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Member_AL {  // 참여자
    Integer $USER_code; // 고유코드
    String $USER_name;    //이름
    List<Position> session = new ArrayList<>();

    // Integer에 $MEMBER_WANTED_prioriy 가져올 예정
    Map<Integer, String> choice = new HashMap<>();
    Map<LocalDate, Long> availableSlots = new HashMap<>();

    public long getBitTime(LocalDate date){
        return availableSlots.getOrDefault(date, 0L);
    }

    // 전체 가능 슬롯 수 (bit카운트 합산)
    public int totalAvailableBits(){
        return availableSlots.values().stream()
                .mapToInt(Long::bitCount)
                .sum();
    }
}


