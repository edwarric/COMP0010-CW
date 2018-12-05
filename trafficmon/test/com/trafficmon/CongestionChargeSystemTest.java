package com.trafficmon;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.time.LocalTime;

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
    }
    
    @Test
    public void exitingUnregisteredVehiclesShouldBeIgnored() {
        assertTrue(CCSystem.getEventlog().isEmpty());
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A987 XYZ"));
        assertThat(CCSystem.getEventlog().size(), is(0));
    }
    
    @Test
    public void unregisteredVehiclesShouldReceivePenaltyNotice() {
        System.setOut(ps);
        
        clock.currentTimeIs(11,00);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A987 XYZ"), clock);
        clock.currentTimeIs(12,30);
        
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A987 XYZ"), clock);
        CCSystem.calculateCharges();
        assertThat(os.toString(), containsString("Penalty notice for: Vehicle [A987 XYZ]"));
    }
    
    @Test
    public void registeredVehiclesChargedCorrectly() throws InterruptedException {
        System.setOut(ps);
        
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A123 XYZ"));
        delayMinutes(1);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A123 XYZ"));
        CCSystem.calculateCharges();
        
        assertThat(os.toString(), containsString("£0.05 deducted"));
    }
    
    @Test
    public void registeredVehiclesChargedCorrectlyUsingSystemClock(){
        System.setOut(ps);
        
        clock.currentTimeIs(15,00);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A123 XYZ"), clock);
        clock.currentTimeIs(15,30);

        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A123 XYZ"), clock);
        CCSystem.calculateCharges();
        
        //Legacy system should charge £1.50 deducted for 30 minutes (£0.05 per min)
        assertThat(os.toString(), containsString("£1.50 deducted"));
    }
    
    private class ControllableClock implements Clock{

        private LocalTime now;

        @Override
        public LocalTime now() {
            return now;
        }

        public void currentTimeIs(int hour, int min){
            now = LocalTime.of(hour, min);
        }
        
    }
    
    private static void delayMinutes(int mins) throws InterruptedException {
        delaySeconds(mins * 60);
    }
    private static void delaySeconds(int secs) throws InterruptedException {
        Thread.sleep(secs * 1000);
    }

}
