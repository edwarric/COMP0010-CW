package com.trafficmon;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;


public class CongestionChargeSystemTests {

    private final CongestionChargeSystem CCSystem = new CongestionChargeSystem();
    private final ControllableClock clock = new ControllableClock();
    private final OutputStream os = new ByteArrayOutputStream();
    private final PrintStream ps = new PrintStream(os);

    @Test
    public void newEventShouldRegisterInLog() {
        assertTrue(CCSystem.getEventLog().isEmpty());
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A123 XYZ"));
        assertThat(CCSystem.getEventLog().size(), is(1));
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A123 XYZ"));
        assertThat(CCSystem.getEventLog().size(), is(2));
    }
    
    @Test
    public void exitingUnregisteredVehiclesShouldBeIgnored() {
        assertTrue(CCSystem.getEventLog().isEmpty());
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A987 XYZ"));
        assertTrue(CCSystem.getEventLog().isEmpty());
    }
    
    @Test
    public void exitingUnregisteredVehiclesShouldBeIgnoredUsingClock() {
        assertTrue(CCSystem.getEventLog().isEmpty());
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A987 XYZ"), clock);
        assertTrue(CCSystem.getEventLog().isEmpty());
    }
    
    @Test
    public void unregisteredVehiclesShouldReceivePenaltyNotice() {
        System.setOut(ps);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A987 XYZ"), clock);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A987 XYZ"), clock);
        CCSystem.calculateCharges(OperationsTeam.getInstance());
        assertThat(os.toString(), containsString("Penalty notice for: Vehicle [A987 XYZ]"));
    }
    
    @Test
    public void registeredVehicleOverstays() {
        System.setOut(ps);
        clock.setHour(9);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A123 XYZ"), clock);
        clock.setHour(18);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A123 XYZ"), clock);
        CCSystem.calculateCharges(OperationsTeam.getInstance());
        assertThat(os.toString(), containsString("£12.00 deducted"));
    }
    
    @Test
    public void registeredVehicleRevisitsAndOverstays() {
        System.setOut(ps);
        clock.setHour(9);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A123 XYZ"), clock);
        clock.setHour(10);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A123 XYZ"), clock);
        clock.setHour(11);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A123 XYZ"), clock);
        clock.setHour(14);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A123 XYZ"), clock);
        CCSystem.calculateCharges(OperationsTeam.getInstance());
        assertThat(os.toString(), containsString("£18.00 deducted"));
    }
    
    @Test
    public void registeredVehicleVisitsAndLeavesBefore2() {
        System.setOut(ps);
        clock.setHour(10);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A123 XYZ"), clock);
        clock.setHour(13);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A123 XYZ"), clock);
        CCSystem.calculateCharges(OperationsTeam.getInstance());
        assertThat(os.toString(), containsString("£6.00 deducted"));
    }
    
    @Test
    public void registeredVehicleVisitsAndLeavesAfter2() {
        System.setOut(ps);
        clock.setHour(15);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A123 XYZ"), clock);
        clock.setHour(18);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A123 XYZ"), clock);
        CCSystem.calculateCharges(OperationsTeam.getInstance());
        assertThat(os.toString(), containsString("£4.00 deducted"));
    }

    @Test
    public void notEnoughCreditShouldFacePenalty() {
        System.setOut(ps);
        for (int i=1; i<20; i+=2) {
            clock.setDay(i);
            CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A123 XYZ"), clock);
            clock.setDay(i+1);
            CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A123 XYZ"), clock);
        }
        CCSystem.calculateCharges(OperationsTeam.getInstance());
        assertThat(os.toString(), containsString("Penalty notice for: Vehicle [A123 XYZ]"));
    }

    @Test
    public void unorderedVehicleLogShouldTriggerInvestigation() {
        System.setOut(ps);
        clock.setHour(14);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A123 XYZ"), clock);
        clock.setHour(12);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A123 XYZ"), clock);
        CCSystem.calculateCharges(OperationsTeam.getInstance());
        assertThat(os.toString(), containsString("Mismatched entries/exits. Triggering investigation into " +
                "vehicle: Vehicle [A123 XYZ]"));
    }

    @Test
    public void duplicateVehicleEntryShouldTriggerInvestigation() {
        System.setOut(ps);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A123 XYZ"), clock);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A123 XYZ"), clock);
        CCSystem.calculateCharges(OperationsTeam.getInstance());
        assertThat(os.toString(), containsString("Mismatched entries/exits. Triggering investigation into " +
                "vehicle: Vehicle [A123 XYZ]"));
    }

    @Test
    public void duplicateVehicleExitShouldTriggerInvestigation() {
        System.setOut(ps);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A123 XYZ"), clock);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A123 XYZ"), clock);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A123 XYZ"), clock);
        CCSystem.calculateCharges(OperationsTeam.getInstance());
        assertThat(os.toString(), containsString("Mismatched entries/exits. Triggering investigation into " +
                "vehicle: Vehicle [A123 XYZ]"));
    }
}
