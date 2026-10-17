package lt.oneman.sync;

import java.awt.Color;
import java.util.Locale;

enum ClueTier
{
    BEGINNER("Beginner", "B", 0xaaaaaa), EASY("Easy", "E", 0x66cc66),
    MEDIUM("Medium", "M", 0x66aaff), HARD("Hard", "H", 0xff6666),
    ELITE("Elite", "El", 0xcc88ff), MASTER("Master", "Ma", 0xffcc55);

    final String label, shortName;
    final Color color;
    ClueTier(String label, String shortName, int rgb)
    {
        this.label = label; this.shortName = shortName; this.color = new Color(rgb);
    }
    String hex() { return String.format("%06x", color.getRGB() & 0xffffff); }
    static ClueTier scrollTier(String name)
    {
        String n = name.toLowerCase(Locale.ROOT);
        if (!(n.startsWith("clue scroll (") || n.startsWith("reward casket (")
            || n.startsWith("clue nest (") || n.startsWith("clue bottle (") || n.startsWith("clue geode ("))) return null;
        for (ClueTier tier : values()) if (n.contains("(" + tier.label.toLowerCase(Locale.ROOT) + ")")) return tier;
        return null;
    }
}
