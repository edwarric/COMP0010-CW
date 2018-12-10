package com.trafficmon;

class ExitEvent extends ZoneBoundaryCrossing {
    ExitEvent(Vehicle vehicle, Clock clock){
        super(vehicle, clock);
    }

    @Override
    public String type() {
        return "exit";
    }
}
