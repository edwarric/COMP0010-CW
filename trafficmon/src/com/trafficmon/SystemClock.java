package com.trafficmon;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class SystemClock implements Clock {
    @Override
    public LocalDateTime now(){
        return LocalDateTime.now();
    }
}
