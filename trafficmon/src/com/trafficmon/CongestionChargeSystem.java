package com.trafficmon;

import java.math.BigDecimal;
import java.rmi.registry.LocateRegistry;
import java.time.LocalDateTime;
import java.util.*;
import static java.time.temporal.ChronoUnit.*;

public class CongestionChargeSystem {

    // list of vehicles and their timestamps for entry/exit events
    private final List<ZoneBoundaryCrossing> eventLog = new ArrayList<ZoneBoundaryCrossing>();

    // adds vehicle object, type and timestamp
    public void vehicleEnteringZone(Vehicle vehicle) {
        eventLog.add(ZoneBoundaryCrossingFactory.getZoneCrossing("Entry", vehicle, new SystemClock()));
    }
    public void vehicleEnteringZone(Vehicle vehicle, Clock clock){
        eventLog.add(ZoneBoundaryCrossingFactory.getZoneCrossing("Entry", vehicle, clock));
    }

    public void vehicleLeavingZone(Vehicle vehicle) {
        if (!previouslyRegistered(vehicle)) {
            // unregistered vehicles are ignored
            return;
        }
        eventLog.add(ZoneBoundaryCrossingFactory.getZoneCrossing("Exit", vehicle, new SystemClock()));
    }
    public void vehicleLeavingZone(Vehicle vehicle, Clock clock) {
        if (!previouslyRegistered(vehicle)) {
            return;
        }
        eventLog.add(ZoneBoundaryCrossingFactory.getZoneCrossing("Exit", vehicle, clock));
    }

    public List getEventlog(){
        return eventLog;
    }

    public void calculateCharges() {

        // maps vehicle to its entry/exit events
        Map<Vehicle, List<ZoneBoundaryCrossing>> crossingsByVehicle = new HashMap<Vehicle, List<ZoneBoundaryCrossing>>();

        for (ZoneBoundaryCrossing crossing : eventLog) {
            Vehicle vehicle = crossing.getVehicle();

            if (!crossingsByVehicle.containsKey(vehicle)) {
                crossingsByVehicle.put(vehicle, new ArrayList<ZoneBoundaryCrossing>());
            }
            crossingsByVehicle.get(vehicle).add(crossing);
        }

        // loops through each map of vehicle to its entry events
        for (Map.Entry<Vehicle, List<ZoneBoundaryCrossing>> vehicleCrossings : crossingsByVehicle.entrySet()) {
            Vehicle vehicle = vehicleCrossings.getKey();

            List<ZoneBoundaryCrossing> crossings = vehicleCrossings.getValue();

            if (!checkOrderingOf(crossings)) {
                OperationsTeam.getInstance().triggerInvestigationInto(vehicle);
            } else {
                BigDecimal charge = calculateChargeForTimeInZone(crossings);
                try {
                    RegisteredCustomerAccountsService.getInstance().accountFor(vehicle).deduct(charge);
                } catch (InsufficientCreditException | AccountNotRegisteredException e){
                    OperationsTeam.getInstance().issuePenaltyNotice(vehicle, charge); }
            }
        }
    }

    private BigDecimal calculateChargeForTimeInZone(List<ZoneBoundaryCrossing> crossings) {
        BigDecimal charge = new BigDecimal(0);
        
        LocalDateTime returnBeforeTime = LocalDateTime.of(0,1,1,0,0);
        LocalDateTime entryTime = null;
        LocalDateTime exitTime;
        boolean canReturnForFree = false;
        
        for (ZoneBoundaryCrossing crossing : crossings) {
    
            if (crossing instanceof EntryEvent) {
                entryTime = crossing.timestamp();
                if (!earlierThan(entryTime, returnBeforeTime)) {
                    canReturnForFree = false;
                }
                if (!canReturnForFree) {
                    returnBeforeTime = entryTime.plusHours(4);
                }
            } else {
                exitTime = crossing.timestamp();
                if (earlierThan(exitTime, returnBeforeTime) && !canReturnForFree) {
                    assert entryTime != null;
                    if (entryTime.getHour() < 14) {
                        charge = charge.add(new BigDecimal(6));
                    } else {
                        charge = charge.add(new BigDecimal(4));
                    }
                    canReturnForFree = true;
                } else if (!earlierThan(exitTime, returnBeforeTime)) {
                    charge = charge.add(new BigDecimal(12));
                }
            }
        }
        return charge;
    }
    
    private boolean earlierThan(LocalDateTime t1, LocalDateTime t2) {
        return t1.compareTo(t2) < 0;
    }

    private boolean previouslyRegistered(Vehicle vehicle) {
        for (ZoneBoundaryCrossing crossing : eventLog)
            if (crossing.getVehicle().equals(vehicle)) {
                return true;
            }
        return false;
    }

    private boolean checkOrderingOf(List<ZoneBoundaryCrossing> crossings) {
        ZoneBoundaryCrossing previousEvent = crossings.get(0);

        for (ZoneBoundaryCrossing crossing : crossings.subList(1, crossings.size())) {
            //the last event should have a greater timestamp || both time stamps shouldn't be entry events || both time stamps shouldn't be exit events
            if (earlierThan(crossing.timestamp(), previousEvent.timestamp()) ||
               (crossing instanceof EntryEvent && previousEvent instanceof EntryEvent) ||
               (crossing instanceof ExitEvent && previousEvent instanceof ExitEvent)) {
                return false;
            }
            previousEvent = crossing;
        }
        return true;
    }
}
