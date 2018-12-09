package com.trafficmon;

class EntryEvent extends ZoneBoundaryCrossing {
    EntryEvent(Vehicle vehicleRegistration, Clock clock){
        super(vehicleRegistration, clock);
    }
}
