package com.trafficmon;

public class EntryEvent extends ZoneBoundaryCrossing {
    public EntryEvent(Vehicle vehicleRegistration) {
        super(vehicleRegistration);
    }
    public EntryEvent(Vehicle vehicleRegistration, Clock clock){
        super(vehicleRegistration, clock);
    }
}
