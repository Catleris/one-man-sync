package lt.oneman.sync;

public enum MonstersProfile
{
    NONE("Off"), LIZARDMAN_SHAMAN("Lizardman Shaman");

    private final String label;
    MonstersProfile(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
