package lt.oneman.sync;

public enum GemCrabSound
{
    FANFARE("Fanfare", "fanfare.wav"),
    CRYSTAL_CHIME("Crystal chime", "chime.wav"),
    ARCADE("Arcade", "arcade.wav"),
    WATCH_BELL("Watch bell", "bell.wav");

    private final String label;
    final String resource;

    GemCrabSound(String label, String resource)
    {
        this.label = label;
        this.resource = "gem-crab/" + resource;
    }

    @Override
    public String toString()
    {
        return label;
    }
}
