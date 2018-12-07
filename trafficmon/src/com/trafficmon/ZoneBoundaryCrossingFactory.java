package com.trafficmon;

public class ZoneBoundaryCrossingFactory {

    public static ZoneBoundaryCrossing getZoneCrossing(String type, Vehicle vehicle, Clock clock){
        if("Entry".equalsIgnoreCase(type)){
            return new EntryEvent(vehicle, clock);
        }
        else if("Exit".equalsIgnoreCase(type)){
            return new ExitEvent(vehicle, clock);
        }

        return null;
    }
}
