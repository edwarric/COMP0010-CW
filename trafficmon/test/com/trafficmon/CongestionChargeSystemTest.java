package com.trafficmon;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;


public class CongestionChargeSystemTest {

    private CongestionChargeSystem CCSystem = new CongestionChargeSystem();
    private ControllableClock clock = new ControllableClock();
    OutputStream os = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(os);

    @Test
    public void newEventShouldRegisterInLog() {
        assertTrue(CCSystem.getEventlog().isEmpty());
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A123 XYZ"));
        assertThat(CCSystem.getEventlog().size(), is(1));
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A123 XYZ"));
        assertThat(CCSystem.getEventlog().size(), is(2));
    }
    
    @Test
    public void exitingUnregisteredVehiclesShouldBeIgnored() {
        assertTrue(CCSystem.getEventlog().isEmpty());
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A987 XYZ"));
        assertThat(CCSystem.getEventlog().size(), is(0));
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A987 XYZ"));
        assertThat(CCSystem.getEventlog().size(), is(0));
    }
    
    @Test
    public void exitingUnregisteredVehiclesShouldBeIgnoredUsingClock() {
        clock.currentTimeIs(1, 15, 20);
        assertTrue(CCSystem.getEventlog().isEmpty());
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A987 XYZ"), clock);
        assertThat(CCSystem.getEventlog().size(), is(0));
    }
    
    @Test
    public void unregisteredVehiclesShouldReceivePenaltyNotice() {
        System.setOut(ps);
        clock.currentTimeIs(1,11, 00);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A987 XYZ"), clock);
        clock.currentTimeIs(1,12,30);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A987 XYZ"), clock);
        CCSystem.calculateCharges();
        assertThat(os.toString(), containsString("Penalty notice for: Vehicle [A987 XYZ]"));
    }
    
    @Test
    public void registeredVehicleOverstays(){
        System.setOut(ps);
        clock.currentTimeIs(1,9,00);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A123 XYZ"), clock);
        clock.currentTimeIs(1, 18, 30);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A123 XYZ"), clock);
        CCSystem.calculateCharges();
        assertThat(os.toString(), containsString("£12.00 deducted"));
    }
    
    @Test
    public void registeredVehicleRevisitsAndOverstays(){
        System.setOut(ps);
        clock.currentTimeIs(1,9,00);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A123 XYZ"), clock);
        clock.currentTimeIs(1, 9, 30);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A123 XYZ"), clock);
        clock.currentTimeIs(1,11,00);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A123 XYZ"), clock);
        clock.currentTimeIs(1, 14, 00);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A123 XYZ"), clock);
        CCSystem.calculateCharges();
        assertThat(os.toString(), containsString("£18.00 deducted"));
    }
    
    @Test
    public void registeredVehicleVisitsAndLeavesBefore2(){
        System.setOut(ps);
        clock.currentTimeIs(1,10,00);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A123 XYZ"), clock);
        clock.currentTimeIs(1, 13, 00);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A123 XYZ"), clock);
        CCSystem.calculateCharges();
        assertThat(os.toString(), containsString("£6.00 deducted"));
    }
    
    @Test
    public void registeredVehicleVisitsAndLeavesAfter2(){
        System.setOut(ps);
        clock.currentTimeIs(1,15,00);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A123 XYZ"), clock);
        clock.currentTimeIs(1, 18, 00);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A123 XYZ"), clock);
        CCSystem.calculateCharges();
        assertThat(os.toString(), containsString("£4.00 deducted"));
    }

    @Test
    public void notEnoughCreditShouldFacePenalty(){
        System.setOut(ps);
        for (int i=1; i<10; i++) {
            clock.currentTimeIs(i,00,00);
            CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A123 XYZ"), clock);
            clock.currentTimeIs(i,12,00);
            CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A123 XYZ"), clock);
        }
        CCSystem.calculateCharges();
        assertThat(os.toString(), containsString("Penalty notice for: Vehicle [A123 XYZ]"));
    }

    @Test
    public void unorderedVehicleLogShouldTriggerInvestigation(){
        System.setOut(ps);
        clock.currentTimeIs(1,14,00);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A123 XYZ"), clock);
        clock.currentTimeIs(1,12,00);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A123 XYZ"), clock);
        CCSystem.calculateCharges();
        assertThat(os.toString(), containsString("Mismatched entries/exits. Triggering investigation into " +
                "vehicle: Vehicle [A123 XYZ]"));
    }

    @Test
    public void duplicateVehicleEntryShouldTriggerInvestigation(){
        System.setOut(ps);
        clock.currentTimeIs(1,12,00);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A123 XYZ"), clock);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A123 XYZ"), clock);
        CCSystem.calculateCharges();
        assertThat(os.toString(), containsString("Mismatched entries/exits. Triggering investigation into " +
                "vehicle: Vehicle [A123 XYZ]"));
    }

    @Test
    public void duplicateVehicleExitShouldTriggerInvestigation(){
        System.setOut(ps);
        clock.currentTimeIs(1,12,00);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A123 XYZ"), clock);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A123 XYZ"), clock);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A123 XYZ"), clock);
        CCSystem.calculateCharges();
        assertThat(os.toString(), containsString("Mismatched entries/exits. Triggering investigation into " +
                "vehicle: Vehicle [A123 XYZ]"));
    }

    
    private class ControllableClock implements Clock{

        private LocalDateTime now;

        @Override
        public LocalDateTime now() {
            return now;
        }

        public void currentTimeIs(int day, int hour, int min){
            now = LocalDateTime.of(0,1, day, hour, min,0,0);
        }
        
    }
    
    private static void delayMinutes(int mins) throws InterruptedException {
        delaySeconds(mins * 60);
    }
    private static void delaySeconds(int secs) throws InterruptedException {
        Thread.sleep(secs * 1000);
    }

}
