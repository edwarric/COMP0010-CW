package com.trafficmon;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public class CongestionChargeSystem {
    
    // list of vehicles and their timestamps (entry/exit)
    private final List<ZoneBoundaryCrossing> eventLog = new ArrayList<ZoneBoundaryCrossing>();
    
    public void vehicleEnteringZone(Vehicle vehicle) {
        eventLog.add(ZoneBoundaryCrossingFactory.getZoneCrossing("Entry", vehicle, new SystemClock()));
    }
    
    public void vehicleEnteringZone(Vehicle vehicle, Clock clock){
        eventLog.add(ZoneBoundaryCrossingFactory.getZoneCrossing("Entry", vehicle, clock));
    }
    
    public void vehicleLeavingZone(Vehicle vehicle) {
        // unregistered vehicles are ignored
        if (!previouslyRegistered(vehicle)) {
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

    public List getEventLog(){
        return eventLog;
    }

    public void calculateCharges(PenaltiesService penaltiesService) {

        Map<Vehicle, List<ZoneBoundaryCrossing>> crossingsByVehicle = new HashMap<Vehicle, List<ZoneBoundaryCrossing>>();

        for (ZoneBoundaryCrossing crossing : eventLog) {
            Vehicle vehicle = crossing.getVehicle();
    
            // stores each vehicle that has an activity once
            if (!crossingsByVehicle.containsKey(vehicle)) {
                crossingsByVehicle.put(vehicle, new ArrayList<ZoneBoundaryCrossing>());
            }
            crossingsByVehicle.get(vehicle).add(crossing);
        }

        for (Map.Entry<Vehicle, List<ZoneBoundaryCrossing>> vehicleCrossings : crossingsByVehicle.entrySet()) {
            Vehicle vehicle = vehicleCrossings.getKey();
            List<ZoneBoundaryCrossing> crossings = vehicleCrossings.getValue();

            if (!checkOrderingOf(crossings)) {
                penaltiesService.triggerInvestigationInto(vehicle);
            } else {
                BigDecimal charge = calculateChargeForTimeInZone(crossings);

                try {
                    RegisteredCustomerAccountsService.getInstance().accountFor(vehicle).deduct(charge);
                } catch (InsufficientCreditException | AccountNotRegisteredException e){
                    penaltiesService.issuePenaltyNotice(vehicle, charge); }
            }
        }
    }

    private BigDecimal calculateChargeForTimeInZone(List<ZoneBoundaryCrossing> crossings) {
        BigDecimal charge = new BigDecimal(0);
        boolean canReturnForFree = false;
        LocalDateTime returnBeforeTime = LocalDateTime.of(0,1,1,0,0);
        LocalDateTime entryTime = null, exitTime;

        for (ZoneBoundaryCrossing crossing : crossings) {
            if (crossing.type().equals("entry") ) {
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
                    charge = decideCharge(entryTime.getHour(), charge);
                    canReturnForFree = true;
                } else if (!earlierThan(exitTime, returnBeforeTime)) {
                    charge = charge.add(new BigDecimal(12));
                }
            }
        }
        return charge;
    }

    private BigDecimal decideCharge(int time, BigDecimal charge) {
        if(time < 14){
            charge = charge.add(new BigDecimal(6));
        }
        else{
            charge = charge.add(new BigDecimal(4));
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
        String crossingType, prevCrossingType;

        for (ZoneBoundaryCrossing crossing : crossings.subList(1, crossings.size())) {
            crossingType = crossing.type();
            prevCrossingType = previousEvent.type();

            if (earlierThan(crossing.timestamp(), previousEvent.timestamp()) ||
                    (crossingType.equals("entry") && prevCrossingType.equals("entry")) ||
                    (crossingType.equals("exit") && prevCrossingType.equals("exit"))) {
                return false;
            }
            previousEvent = crossing;
        }
        return true;
    }
}
