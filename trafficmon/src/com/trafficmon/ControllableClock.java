package com.trafficmon;

import java.time.LocalDateTime;

class ControllableClock implements Clock{

    private LocalDateTime now;

    @Override
    public LocalDateTime now() {
        return now;
    }

    ControllableClock() {
        now = LocalDateTime.of(2018, 1, 1, 0, 0, 0);
    }

    void setYear(int year) {
        now = now.withYear(year);
    }

    void setMonth(int month) {
        now = now.withMonth(month);
    }

    void setDay(int day) {
        now = now.withDayOfMonth(day);
    }

    void setHour(int hour) {
        now = now.withHour(hour);
    }

    void setMin(int min) {
        now = now.withMinute(min);
    }

    void setSec(int sec) {
        now = now.withSecond(sec);
    }
}
