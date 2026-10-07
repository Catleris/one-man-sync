package lt.oneman.sync;

import net.runelite.client.config.*;

public interface SlayerLabConfig extends Config
{
    String GROUP = "one-man-slayer-lab";
    enum CombatStyle { MELEE, RANGED, MAGIC }
    @ConfigItem(keyName="combatStyle", name="Supplies combat style", description="Controls ammunition or rune quantity warnings; does not select combat actions.", position=10)
    default CombatStyle combatStyle() { return CombatStyle.MELEE; }
    @Range(min=0,max=28)
    @ConfigItem(keyName="foodWarning", name="Food warning below", description="Your chosen food quantity threshold. Zero disables this warning.", position=11)
    default int foodWarning() { return 3; }
    @Range(min=0,max=200)
    @ConfigItem(keyName="doseWarning", name="Potion doses warning below", description="Total carried potion doses; not a required task quantity. Zero disables this warning.", position=12)
    default int doseWarning() { return 0; }
    @Range(min=0,max=10000)
    @ConfigItem(keyName="ammoWarning", name="Ammo warning below", description="Carried + equipped visible ammunition quantity; no weapon charge inspection.", position=13)
    default int ammoWarning() { return 100; }
    @Range(min=0,max=10000)
    @ConfigItem(keyName="runeWarning", name="Rune warning below", description="Visible inventory rune quantity only; rune pouch contents are not counted.", position=14)
    default int runeWarning() { return 100; }
    @ConfigItem(keyName="notifySupplies", name="Desktop supplies alerts", description="Notify once when a chosen threshold is crossed. Local notifications only.", position=15)
    default boolean notifySupplies() { return false; }
    @Range(min=0,max=99)
    @ConfigItem(keyName="prayerPointsWarning", name="Prayer points warning below", description="Visual warning below this number of your Prayer points. Zero disables it.", position=16)
    default int prayerPointsWarning() { return 15; }
    @Range(min=0,max=112)
    @ConfigItem(keyName="prayerDosesWarning", name="Prayer / restore doses below", description="Combined carried Prayer potion and Super restore doses, including mixes. Zero disables it.", position=17)
    default int prayerDosesWarning() { return 2; }
    @ConfigItem(keyName="notifyPrayer", name="Desktop Prayer alerts", description="Optional local alert when your points or carried doses cross their threshold. Rearms after recovery.", position=18)
    default boolean notifyPrayer() { return false; }
}
