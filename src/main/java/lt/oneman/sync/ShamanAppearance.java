package lt.oneman.sync;

import java.awt.Color;
import net.runelite.client.config.ConfigManager;

/** One-shot visual presets; later manual edits remain authoritative. */
enum ShamanAppearance
{
    SUBTLE("Subtle", new Color(170,130,210), new Color(195,155,230), 55, 75, 0, 1, false),
    CLEAR("Clear", new Color(190,110,240), new Color(220,110,255), 85, 100, 20, 2, true),
    VIVID("Vivid", new Color(255,175,70), new Color(255,215,100), 100, 100, 35, 3, true);

    private final String label;
    private final Color tile, model;
    private final int tileOpacity, modelOpacity, fillOpacity, width;
    private final boolean pulse;

    ShamanAppearance(String label, Color tile, Color model, int tileOpacity, int modelOpacity,
        int fillOpacity, int width, boolean pulse)
    {
        this.label=label; this.tile=tile; this.model=model; this.tileOpacity=tileOpacity;
        this.modelOpacity=modelOpacity; this.fillOpacity=fillOpacity; this.width=width; this.pulse=pulse;
    }

    void apply(ConfigManager configs)
    {
        String group="one-man-sync";
        configs.setConfiguration(group,"monstersShamanTiles",true);
        configs.setConfiguration(group,"monstersShamanOutline",true);
        configs.setConfiguration(group,"monstersShamanFill",fillOpacity>0);
        configs.setConfiguration(group,"monstersShamanLabel",true);
        configs.setConfiguration(group,"monstersShamanColor",tile);
        configs.setConfiguration(group,"monstersShamanModelColor",model);
        configs.setConfiguration(group,"monstersShamanTileOpacity",tileOpacity);
        configs.setConfiguration(group,"monstersShamanModelOpacity",modelOpacity);
        configs.setConfiguration(group,"monstersShamanFillOpacity",fillOpacity);
        configs.setConfiguration(group,"monstersShamanOutlineWidth",width);
        configs.setConfiguration(group,"monstersShamanPulse",pulse);
    }

    @Override public String toString() { return label; }
}
