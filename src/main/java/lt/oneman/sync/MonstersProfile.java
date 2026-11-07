package lt.oneman.sync;

public enum MonstersProfile
{
    NONE("Off"), LIZARDMAN_SHAMAN("Lizardman Shaman"), GROTESQUE_GUARDIANS("Grotesque Guardians");

    private final String label;
    MonstersProfile(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
