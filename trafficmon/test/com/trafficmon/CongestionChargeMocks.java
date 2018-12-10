package com.trafficmon;


import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CongestionChargeMocks {
    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();
    AccountsService accountsService = context.mock(AccountsService.class);
    PenaltiesService penaltiesService = context.mock(PenaltiesService.class);

    CongestionChargeSystem CCSystem = new CongestionChargeSystem();
    ControllableClock clock = new ControllableClock();
    Account account = new Account("Fehed", Vehicle.withRegistration("A102 ABC"), new BigDecimal(0));



    @Test
    public void vehicleEntryAndExitBefore2Within4Hours(){
        context.checking(new Expectations() {{
            exactly(1).of(penaltiesService).issuePenaltyNotice(Vehicle.withRegistration("A102 ABC"), new BigDecimal(6));
        }});
        clock.currentTimeIs(1,9,00);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A102 ABC"), clock);
        clock.currentTimeIs(1,10,00);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A102 ABC"), clock);
        CCSystem.calculateCharges(penaltiesService);
    }

    @Test
    public void vehicleEntryAndExitAfter2Within4Hours(){
        context.checking(new Expectations() {{
            exactly(1).of(penaltiesService).issuePenaltyNotice(Vehicle.withRegistration("A102 ABC"), new BigDecimal(4));
        }});
        clock.currentTimeIs(1,15,00);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A102 ABC"), clock);
        clock.currentTimeIs(1,18,00);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A102 ABC"), clock);
        CCSystem.calculateCharges(penaltiesService);
    }

    @Test
    public void vehicleExitAfter4Hours(){
        context.checking(new Expectations() {{
            exactly(1).of(penaltiesService).issuePenaltyNotice(Vehicle.withRegistration("A102 ABC"), new BigDecimal(12));
        }});
        clock.currentTimeIs(1,9,00);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A102 ABC"), clock);
        clock.currentTimeIs(1,15,00);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A102 ABC"), clock);
        CCSystem.calculateCharges(penaltiesService);
    }

    @Test
    public void vehicleReEnteredWithoutSecondCharge(){
        context.checking(new Expectations() {{
            exactly(1).of(penaltiesService).issuePenaltyNotice(Vehicle.withRegistration("A102 ABC"), new BigDecimal(6));
        }});
        clock.currentTimeIs(1,9,00);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A102 ABC"), clock);
        clock.currentTimeIs(1,10,00);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A102 ABC"), clock);
        clock.currentTimeIs(1,11,00);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A102 ABC"), clock);
        clock.currentTimeIs(1,12,00);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A102 ABC"), clock);
        CCSystem.calculateCharges(penaltiesService);
    }

    @Test
    public void vehicleReEnteredAndOverstayed(){
        context.checking(new Expectations() {{
            exactly(1).of(penaltiesService).issuePenaltyNotice(Vehicle.withRegistration("A102 ABC"), new BigDecimal(18));
        }});
        clock.currentTimeIs(1,9,00);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A102 ABC"), clock);
        clock.currentTimeIs(1,10,00);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A102 ABC"), clock);
        clock.currentTimeIs(1,11,00);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A102 ABC"), clock);
        clock.currentTimeIs(1,14,00);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A102 ABC"), clock);
        CCSystem.calculateCharges(penaltiesService);
    }

    @Test
    public void vehicleStaysBefore2AndAfter2(){
        context.checking(new Expectations() {{
            exactly(1).of(penaltiesService).issuePenaltyNotice(Vehicle.withRegistration("A102 ABC"), new BigDecimal(10));
        }});
        clock.currentTimeIs(1,9,00);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A102 ABC"), clock);
        clock.currentTimeIs(1,10,00);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A102 ABC"), clock);
        clock.currentTimeIs(1,15,00);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A102 ABC"), clock);
        clock.currentTimeIs(1,17,00);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A102 ABC"), clock);
        CCSystem.calculateCharges(penaltiesService);
    }

    @Test
    public void duplicateVehicleEntryShouldTriggerInvestigation(){
        context.checking(new Expectations() {{
            exactly(1).of(penaltiesService).triggerInvestigationInto(Vehicle.withRegistration("A102 ABC"));
        }});
        clock.currentTimeIs(1,13,00);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A102 ABC"), clock);
        clock.currentTimeIs(1,10,00);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A102 ABC"), clock);
        CCSystem.calculateCharges(penaltiesService);
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





}
