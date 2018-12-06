package com.trafficmon;

import java.time.LocalDateTime;

public abstract class ZoneBoundaryCrossing {

    private final Vehicle vehicle;
    private final LocalDateTime time;
    private final Clock clock;

    public ZoneBoundaryCrossing(Vehicle vehicle, Clock clock){
        this.vehicle = vehicle;
        this.clock = clock;
        this.time = clock.now();
    }
    

    public Vehicle getVehicle() {
        return vehicle;
    }

    public LocalDateTime timestamp() {
        return time;
    }
}
