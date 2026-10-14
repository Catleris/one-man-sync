package lt.oneman.sync;

public class BirdHouseDecoder {
    public static final String[] BIRDHOUSE_NAMES = new String[]{"Bird House", "Oak Bird House", "Willow Bird House", "Teak Bird House", "Maple Bird House", "Mahogany Bird House", "Yew Bird House", "Magic Bird House", "Redwood Bird House"};

    public static State getState(int varp) {
        if (varp < 0 || varp > BIRDHOUSE_NAMES.length * 3) {
            return State.UNKNOWN;
        }
        if (varp == 0) {
            return State.EMPTY;
        }
        if (varp % 3 == 0) {
            return State.SEEDED;
        }
        return State.BUILT;
    }

    public static String getHouseName(int varp) {
        int index = (varp - 1) / 3;
        if (varp <= 0 || index < 0 || index >= BIRDHOUSE_NAMES.length) {
            return null;
        }
        return BIRDHOUSE_NAMES[index];
    }

    public static enum State {
        SEEDED,
        BUILT,
        EMPTY,
        UNKNOWN;

    }
}

