package com.trafficmon;

import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.is;

import java.math.BigDecimal;

public class CongestionChargeMocks {
    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();
    private final PenaltiesService penaltiesService = context.mock(PenaltiesService.class);

    private final CongestionChargeSystem CCSystem = new CongestionChargeSystem();
    private final ControllableClock clock = new ControllableClock();
    Account account = new Account("Fehed", Vehicle.withRegistration("A102 ABC"), new BigDecimal(0));

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
        context.checking(new Expectations() {{
            exactly(1).of(penaltiesService).issuePenaltyNotice(Vehicle.withRegistration("ASDFGHJK"),
                                                                      new BigDecimal(6));
        }});
        clock.setHour(9);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("ASDFGHJK"), clock);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("ASDFGHJK"), clock);
        CCSystem.calculateCharges(penaltiesService);


    }

    @Test
    public void registeredVehicleOverstays() {
      context.checking(new Expectations() {{
          exactly(1).of(penaltiesService).issuePenaltyNotice(Vehicle.withRegistration("A102 ABC"),
                                                                            new BigDecimal(12));
      }});
      clock.setHour(9);
      CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A102 ABC"), clock);
      clock.setHour(15);
      CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A102 ABC"), clock);
      CCSystem.calculateCharges(penaltiesService);
    }

    @Test
    public void registeredVehicleRevisitsAndOverstays() {
        context.checking(new Expectations() {{
            exactly(1).of(penaltiesService).issuePenaltyNotice(Vehicle.withRegistration("A102 ABC"),
                                                                              new BigDecimal(18));
        }});
        clock.setHour(9);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A102 ABC"), clock);
        clock.setHour(10);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A102 ABC"), clock);
        clock.setHour(11);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A102 ABC"), clock);
        clock.setHour(14);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A102 ABC"), clock);
        CCSystem.calculateCharges(penaltiesService);
    }

    @Test
    public void registeredVehicleVisitsAndLeavesBefore2() {
        context.checking(new Expectations() {{
            exactly(1).of(penaltiesService).issuePenaltyNotice(Vehicle.withRegistration("A102 ABC"),
                                                                              new BigDecimal(6));
        }});
        clock.setHour(9);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A102 ABC"), clock);
        clock.setHour(10);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A102 ABC"), clock);
        CCSystem.calculateCharges(penaltiesService);
    }

    @Test
    public void registeredVehicleVisitsAndLeavesAfter2() {
        context.checking(new Expectations() {{
            exactly(1).of(penaltiesService).issuePenaltyNotice(Vehicle.withRegistration("A102 ABC"),
                                                                              new BigDecimal(4));
        }});
        clock.setHour(15);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A102 ABC"), clock);
        clock.setHour(18);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A102 ABC"), clock);
        CCSystem.calculateCharges(penaltiesService);
    }

    @Test
    public void registeredVehicleVisitsBeforeAndAfter2() {
        context.checking(new Expectations() {{
            exactly(1).of(penaltiesService).issuePenaltyNotice(Vehicle.withRegistration("A102 ABC"),
                                                                              new BigDecimal(10));
        }});
        clock.setHour(9);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A102 ABC"), clock);
        clock.setHour(10);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A102 ABC"), clock);
        clock.setHour(15);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A102 ABC"), clock);
        clock.setHour(17);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A102 ABC"), clock);
        CCSystem.calculateCharges(penaltiesService);
    }

    @Test
    public void notEnoughCreditShouldFacePenalty() {
        context.checking(new Expectations() {{
            exactly(1).of(penaltiesService).issuePenaltyNotice(Vehicle.withRegistration("A102 ABC"),
                                                                              new BigDecimal(6));
        }});
        clock.setHour(9);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A102 ABC"), clock);
        clock.setHour(10);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A102 ABC"), clock);
        CCSystem.calculateCharges(penaltiesService);
    }

    @Test
    public void vehicleReEnteredWithoutSecondCharge() {
        context.checking(new Expectations() {{
            exactly(1).of(penaltiesService).issuePenaltyNotice(Vehicle.withRegistration("A102 ABC"),
                                                                              new BigDecimal(6));
        }});
        clock.setHour(9);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A102 ABC"), clock);
        clock.setHour(10);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A102 ABC"), clock);
        clock.setHour(11);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A102 ABC"), clock);
        clock.setHour(12);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A102 ABC"), clock);
        CCSystem.calculateCharges(penaltiesService);
    }

    @Test
    public void unorderedVehicleLogShouldTriggerInvestigation() {
        context.checking(new Expectations() {{
            exactly(1).of(penaltiesService).triggerInvestigationInto(Vehicle.withRegistration("A102 ABC"));
        }});
        clock.setHour(13);
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A102 ABC"), clock);
        clock.setHour(10);
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A102 ABC"), clock);
        CCSystem.calculateCharges(penaltiesService);
    }

    @Test
    public void duplicateVehicleEntryShouldTriggerInvestigation() {
        context.checking(new Expectations() {{
            exactly(1).of(penaltiesService).triggerInvestigationInto(Vehicle.withRegistration("A102 ABC"));
        }});
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A102 ABC"));
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A102 ABC"));
        CCSystem.calculateCharges(penaltiesService);

    }

    @Test
    public void duplicateVehicleExitShouldTriggerInvestigation() {
        context.checking(new Expectations() {{
            exactly(1).of(penaltiesService).triggerInvestigationInto(Vehicle.withRegistration("A102 ABC"));
        }});
        CCSystem.vehicleEnteringZone(Vehicle.withRegistration("A102 ABC"));
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A102 ABC"));
        CCSystem.vehicleLeavingZone(Vehicle.withRegistration("A102 ABC"));
        CCSystem.calculateCharges(penaltiesService);
    }
}
