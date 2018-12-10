package com.trafficmon;

class EntryEvent extends ZoneBoundaryCrossing {
    EntryEvent(Vehicle vehicleRegistration, Clock clock){
        super(vehicleRegistration, clock);
    }

    @Override
    public String type(){
      return "entry";
    }
}
