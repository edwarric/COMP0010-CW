package com.trafficmon;

public class Example {
    public static void main(String[] args) throws Exception {
        CongestionChargeSystem congestionChargeSystem = new CongestionChargeSystem();

        congestionChargeSystem.vehicleEnteringZone(Vehicle.withRegistration("dkjf XYZ"));
        delaySeconds(1);
//        congestionChargeSystem.vehicleEnteringZone(Vehicle.withRegistration("J091 4PY"));
//        delaySeconds(1);
        //congestionChargeSystem.vehicleLeavingZone(Vehicle.withRegistration("dkjf XYZ"));
//        delaySeconds(1);
//        congestionChargeSystem.vehicleLeavingZone(Vehicle.withRegistration("J041 4PY"));
        congestionChargeSystem.calculateCharges();
    }
    private static void delayMinutes(int mins) throws InterruptedException {
        delaySeconds(mins * 60);
    }
    private static void delaySeconds(int secs) throws InterruptedException {
        Thread.sleep(secs * 1000);
    }
}
