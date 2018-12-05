package com.trafficmon;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.time.LocalTime;
import static org.hamcrest.CoreMatchers.containsString;

import static org.junit.Assert.*;

public class CongestionChargeSystemTest {

    private CongestionChargeSystem CCSystem = new CongestionChargeSystem();

    @Test
    public void newEventIsRegisteredInTheLog() {
        assert CCSystem.getEventlog().isEmpty();
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A123 XYZ"));
        assert CCSystem.getEventlog().size() == 1;
    }

    @Test
    public void chargeCalculation(){
        OutputStream os = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(os);
        System.setOut(ps);
        ControllableClock clock = new ControllableClock();

        clock.currentTimeIs(15,00);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A123 XYZ"), clock);
        clock.currentTimeIs(15,30);

        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A123 XYZ"), clock);
        CCSystem.calculateCharges();
        //Legacy system should charge £1.50 deducted for 30 minutes
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

}