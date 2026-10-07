package lt.oneman.sync;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class SlayerLabPrayerTest
{
    private final SlayerLabConfig config=new SlayerLabConfig(){};
    @Test public void dosesSeparatePotionTypesAndIgnoreOtherSources()
    {
        Map<String,Integer> items=new HashMap<>();
        items.put("Prayer potion(4)",2); items.put("Prayer mix(2)",1);
        items.put("Super restore(3)",2);items.put("Super restore mix(1)",1);
        items.put("Sanfew serum(4)",2);items.put("Blighted super restore(4)",2);
        items.put("Prayer potion(0)",1);items.put("Prayer potion(4) (noted)",10);
        SlayerLabPrayer.Doses d=SlayerLabPrayer.doses(items);
        assertEquals(10,d.prayer);assertEquals(7,d.restore);assertEquals(17,d.total());
    }
    @Test public void timeNeedsStableSampleAndTracksActualObservedLoss()
    {
        SlayerLabPrayer m=new SlayerLabPrayer();m.observe(50,"PIETY");
        assertTrue(Double.isNaN(m.remainingSeconds(50)));
        for(int tick=1;tick<=20;tick++) m.observe(50-tick/10,"PIETY");
        assertEquals(288,m.remainingSeconds(48),0.001);
        assertTrue(m.describe(48,70,Collections.singletonList("PIETY"),new SlayerLabPrayer.Doses(),config).contains("~4m 48s"));
    }
    @Test public void restorationSwitchingAndEquipmentResetOldEstimate()
    {
        SlayerLabPrayer m=new SlayerLabPrayer();m.observe(50,"PIETY");
        for(int tick=1;tick<=20;tick++)m.observe(50-tick/10,"PIETY");
        assertFalse(Double.isNaN(m.remainingSeconds(48)));
        m.observe(70,"PIETY");assertTrue(Double.isNaN(m.remainingSeconds(70)));
        for(int tick=1;tick<=20;tick++)m.observe(70-tick/10,"PIETY");
        m.observe(68,"PROTECT FROM MAGIC");assertTrue(Double.isNaN(m.remainingSeconds(68)));
        m.resetEstimate();assertTrue(Double.isNaN(m.remainingSeconds(68)));
    }
    @Test public void noActivePrayerOrNoObservedDrainDoesNotInventTime()
    {
        SlayerLabPrayer m=new SlayerLabPrayer();
        for(int i=0;i<100;i++)m.observe(50,"PROTECT ITEM");
        assertTrue(Double.isNaN(m.remainingSeconds(50)));
        m.observe(50,"");assertTrue(Double.isNaN(m.remainingSeconds(50)));
        assertTrue(m.describe(50,70,Collections.emptyList(),new SlayerLabPrayer.Doses(),config).contains("no active prayers"));
        assertEquals(0,m.remainingSeconds(0),0);
    }
    @Test public void alertsFireOnceAndRearmSeparatelyAfterRecovery()
    {
        SlayerLabPrayer m=new SlayerLabPrayer();SlayerLabPrayer.Doses d=new SlayerLabPrayer.Doses();d.prayer=3;
        assertTrue(m.crossed(20,d,config).isEmpty());
        assertEquals(1,m.crossed(14,d,config).size());assertTrue(m.crossed(13,d,config).isEmpty());
        d.prayer=1;assertEquals(1,m.crossed(13,d,config).size());
        m.crossed(20,d,config);assertEquals(1,m.crossed(14,d,config).size());
        d.prayer=3;m.crossed(14,d,config);d.prayer=0;assertEquals(1,m.crossed(14,d,config).size());
    }
    @Test public void thresholdsAreStrictAndZeroDisablesAlerts()
    {
        SlayerLabPrayer m=new SlayerLabPrayer();SlayerLabPrayer.Doses d=new SlayerLabPrayer.Doses();d.restore=2;
        assertTrue(m.warnings(15,d,config).isEmpty());
        assertEquals(1,m.warnings(14,d,config).size());
        SlayerLabConfig off=new SlayerLabConfig(){@Override public int prayerPointsWarning(){return 0;} @Override public int prayerDosesWarning(){return 0;}};
        d.restore=0;assertTrue(m.warnings(0,d,off).isEmpty());assertTrue(m.crossed(0,d,off).isEmpty());
    }
    @Test public void rollingWindowDropsOldDrainAndResetClearsAlerts()
    {
        SlayerLabPrayer m=new SlayerLabPrayer();m.observe(50,"PIETY");m.observe(40,"PIETY");
        for(int i=0;i<50;i++)m.observe(40,"PIETY");
        assertTrue(Double.isNaN(m.remainingSeconds(40)));
        SlayerLabPrayer.Doses d=new SlayerLabPrayer.Doses();assertEquals(2,m.crossed(1,d,config).size());
        m.reset();assertEquals(2,m.crossed(1,d,config).size());
    }
}
