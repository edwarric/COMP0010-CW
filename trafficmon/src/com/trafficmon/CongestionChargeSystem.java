package com.trafficmon;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public class CongestionChargeSystem {

    private final List<ZoneBoundaryCrossing> eventLog = new ArrayList<ZoneBoundaryCrossing>();
    //is a list of vehicles and their timestamps (entry/exit)

    public void vehicleEnteringZone(Vehicle vehicle) {
        //adds the vehicle object and its entry time to list
        eventLog.add(ZoneBoundaryCrossingFactory.getZoneCrossing("Entry", vehicle, new SystemClock()));
    }
    public void vehicleEnteringZone(Vehicle vehicle, Clock clock){
        //adds the vehicle object and its entry time to list
        eventLog.add(ZoneBoundaryCrossingFactory.getZoneCrossing("Entry", vehicle, clock));
    }


    public void vehicleLeavingZone(Vehicle vehicle) {
        if (!previouslyRegistered(vehicle)) {
            //unregistered vehicles are ignored
            return;
        }
        //adds vehicle and its exit time to list
        eventLog.add(ZoneBoundaryCrossingFactory.getZoneCrossing("Exit", vehicle, new SystemClock()));
    }

    public void vehicleLeavingZone(Vehicle vehicle, Clock clock) {
        if (!previouslyRegistered(vehicle)) {
            //unregistered vehicles are ignored
            return;
        }
        //adds vehicle and its exit time to list
        eventLog.add(ZoneBoundaryCrossingFactory.getZoneCrossing("Exit", vehicle, clock));
    }

    public List getEventlog(){
        return eventLog;
    }

    public void calculateCharges(PenaltiesService penaltiesService) {

        Map<Vehicle, List<ZoneBoundaryCrossing>> crossingsByVehicle = new HashMap<Vehicle, List<ZoneBoundaryCrossing>>();

        for (ZoneBoundaryCrossing crossing : eventLog) {
            Vehicle vehicle = crossing.getVehicle();

            //Finds vehicle in list
            if (!crossingsByVehicle.containsKey(vehicle)) {
                //stores each vehicle that has an activity once.
                crossingsByVehicle.put(vehicle, new ArrayList<ZoneBoundaryCrossing>());
            }
            crossingsByVehicle.get(vehicle).add(crossing);
        }

        for (Map.Entry<Vehicle, List<ZoneBoundaryCrossing>> vehicleCrossings : crossingsByVehicle.entrySet()) {
            //iterates through every vehicle stored in crossingsByVehicle
            Vehicle vehicle = vehicleCrossings.getKey();
            //Stores all activities of 'vehicle in crossings'
            List<ZoneBoundaryCrossing> crossings = vehicleCrossings.getValue();

            if (!checkOrderingOf(crossings)) {
                penaltiesService.triggerInvestigationInto(vehicle);
            } else {
                //calls calculation function
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
        
        LocalDateTime returnBeforeTime = LocalDateTime.of(0,1,1,0,0);
        LocalDateTime entryTime = null, exitTime;
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
        if (t1.compareTo(t2) < 0) {
            return true;
        }
        return false;
    }

    private boolean previouslyRegistered(Vehicle vehicle) {
        for (ZoneBoundaryCrossing crossing : eventLog)
            if (crossing.getVehicle().equals(vehicle)) {
                return true;
            }

        return false;
    }

    private boolean checkOrderingOf(List<ZoneBoundaryCrossing> crossings) {
        //method finds the last occurrings event.
        ZoneBoundaryCrossing lastEvent = crossings.get(0);

        for (ZoneBoundaryCrossing crossing : crossings.subList(1, crossings.size())) {
            //the last event should have a greater timestamp || both time stamps shouldn't be entry events || both time stamps shouldn't be exit events
            if ((crossing.timestamp().compareTo(lastEvent.timestamp()) < 0) ||
               (crossing instanceof EntryEvent && lastEvent instanceof EntryEvent) ||
               (crossing instanceof ExitEvent && lastEvent instanceof ExitEvent)) {
                return false;
            }
            lastEvent = crossing;
        }

        return true;
    }

}
