package com.trafficmon;

public class ExitEvent extends ZoneBoundaryCrossing {
    public ExitEvent(Vehicle vehicle, Clock clock){
        super(vehicle, clock);
    }
}
