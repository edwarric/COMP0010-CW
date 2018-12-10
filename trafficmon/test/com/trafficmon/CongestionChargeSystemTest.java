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
    private OutputStream os = new ByteArrayOutputStream();
    private PrintStream ps = new PrintStream(os);

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
    }
    
    @Test
    public void exitingUnregisteredVehiclesShouldBeIgnoredUsingClock() {
        assertTrue(CCSystem.getEventlog().isEmpty());
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A987 XYZ"), clock);
        assertThat(CCSystem.getEventlog().size(), is(0));
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
    public void registeredVehicleOverstays(){
        System.setOut(ps);
        clock.setHour(9);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A123 XYZ"), clock);
        clock.setHour(18);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A123 XYZ"), clock);
        CCSystem.calculateCharges(OperationsTeam.getInstance());
        assertThat(os.toString(), containsString("£12.00 deducted"));
    }
    
    @Test
    public void registeredVehicleRevisitsAndOverstays(){
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
    public void registeredVehicleVisitsAndLeavesBefore2(){
        System.setOut(ps);
        clock.setHour(10);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A123 XYZ"), clock);
        clock.setHour(13);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A123 XYZ"), clock);
        CCSystem.calculateCharges(OperationsTeam.getInstance());
        assertThat(os.toString(), containsString("£6.00 deducted"));
    }
    
    @Test
    public void registeredVehicleVisitsAndLeavesAfter2(){
        System.setOut(ps);
        clock.setHour(15);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A123 XYZ"), clock);
        clock.setHour(18);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A123 XYZ"), clock);
        CCSystem.calculateCharges(OperationsTeam.getInstance());
        assertThat(os.toString(), containsString("£4.00 deducted"));
    }

    @Test
    public void notEnoughCreditShouldFacePenalty(){
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
    public void unorderedVehicleLogShouldTriggerInvestigation(){
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
    public void duplicateVehicleEntryShouldTriggerInvestigation(){
        System.setOut(ps);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A123 XYZ"), clock);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A123 XYZ"), clock);
        CCSystem.calculateCharges(OperationsTeam.getInstance());
        assertThat(os.toString(), containsString("Mismatched entries/exits. Triggering investigation into " +
                "vehicle: Vehicle [A123 XYZ]"));
    }

    @Test
    public void duplicateVehicleExitShouldTriggerInvestigation(){
        System.setOut(ps);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A123 XYZ"), clock);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A123 XYZ"), clock);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A123 XYZ"), clock);
        CCSystem.calculateCharges(OperationsTeam.getInstance());
        assertThat(os.toString(), containsString("Mismatched entries/exits. Triggering investigation into " +
                "vehicle: Vehicle [A123 XYZ]"));
    }

    
    static class ControllableClock implements Clock{

        private LocalDateTime now;

        @Override
        public LocalDateTime now() {
            return now;
        }

        ControllableClock() {
            now = LocalDateTime.of(2018, 1, 1, 0, 0, 0);
        }

        void setYear(int year) {
            now = now.withYear(year);
        }

        void setMonth(int month) {
            now = now.withMonth(month);
        }

        void setDay(int day) {
            now = now.withDayOfMonth(day);
        }

        void setHour(int hour) {
            now = now.withHour(hour);
        }

        void setMin(int min) {
            now = now.withMinute(min);
        }

        void setSec(int sec) {
            now = now.withSecond(sec);
        }
    }
    
    private static void delayMinutes(int mins) throws InterruptedException {
        delaySeconds(mins * 60);
    }
    private static void delaySeconds(int secs) throws InterruptedException {
        Thread.sleep(secs * 1000);
    }
}
