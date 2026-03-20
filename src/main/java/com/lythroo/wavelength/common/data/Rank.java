package com.lythroo.wavelength.common.data;

public enum Rank {
    DEVELOPER("Developer"),
    KING     ("King"),
    QUEEN    ("Queen"),
    MAYOR    ("Mayor"),
    KNIGHT   ("Knight"),
    GUARD    ("Guard"),
    BUILDER  ("Builder"),
    FARMER   ("Farmer"),
    MINER    ("Miner"),
    CITIZEN  ("Citizen"),
    TRAVELER ("Traveler"),
    FOUNDER  ("Founder"),
    NONE     ("");

    public final String displayName;

    Rank(String displayName) {
        this.displayName = displayName;
    }

    public String format(String townName) {
        if (this == NONE) return "";
        if (townName == null || townName.isBlank()) return displayName;
        return displayName + " of " + townName;
    }

    public static Rank[] selectable() {

        return java.util.Arrays.stream(values())
                .filter(r -> r != DEVELOPER)
                .toArray(Rank[]::new);
    }
}