package com.example.demo.algorithm;

import java.time.LocalTime;

public class TimeUtils {       // 시간 48비트로 표현(30분)
    // HH:mm 형식을 0~47 인덱스로 변환
    public static int timeToIndex(LocalTime time){  // 13:00 같은 시간이 몇번째 bit인지 판별
        return ((time.getHour()*2) + (time.getMinute() >= 30? 1 : 0));
    }
    public static long createBitmask(LocalTime start, LocalTime end){   // 시작시간~종료시간 비트화
        long mask = 0;
        int startIndex = timeToIndex(start);
        int endIndex = timeToIndex(end);

        for (int i= startIndex; i<endIndex; i++){
            mask = mask | (1L << i);
        }
        return mask;
    }
    // 비트 인덱스 -> LocalTime 변환 (역변환)
    public static LocalTime indexToTime(int index) {
        return LocalTime.of(index / 2, (index % 2) * 30);
    }
}


