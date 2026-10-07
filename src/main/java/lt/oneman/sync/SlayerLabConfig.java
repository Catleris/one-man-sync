package lt.oneman.sync;

import net.runelite.client.config.*;

@ConfigGroup(SlayerLabConfig.GROUP)
public interface SlayerLabConfig extends Config
{
    String GROUP = "one-man-slayer-lab";
    enum CombatStyle { MELEE, RANGED, MAGIC }
    @ConfigItem(keyName="combatStyle", name="Supplies combat style", description="Controls ammunition or rune quantity warnings; does not select combat actions.", position=0)
    default CombatStyle combatStyle() { return CombatStyle.MELEE; }
    @Range(min=0,max=28)
    @ConfigItem(keyName="foodWarning", name="Food warning below", description="Your chosen food quantity threshold. Zero disables this warning.", position=1)
    default int foodWarning() { return 3; }
    @Range(min=0,max=200)
    @ConfigItem(keyName="doseWarning", name="Potion doses warning below", description="Total carried potion doses; not a required task quantity. Zero disables this warning.", position=2)
    default int doseWarning() { return 0; }
    @Range(min=0,max=10000)
    @ConfigItem(keyName="ammoWarning", name="Ammo warning below", description="Carried + equipped visible ammunition quantity; no weapon charge inspection.", position=3)
    default int ammoWarning() { return 100; }
    @Range(min=0,max=10000)
    @ConfigItem(keyName="runeWarning", name="Rune warning below", description="Visible inventory rune quantity only; rune pouch contents are not counted.", position=4)
    default int runeWarning() { return 100; }
    @ConfigItem(keyName="notifySupplies", name="Desktop supplies alerts", description="Notify once when a chosen threshold is crossed. Local notifications only.", position=5)
    default boolean notifySupplies() { return false; }
}
