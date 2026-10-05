package dev.cricklive.match.domain.enums;

public enum MatchFormat {
    TEST(0, 4), ODI(50, 2), T20I(20, 2), T20(20, 2), LIST_A(50, 2), FIRST_CLASS(0, 4);

    private final int maxOvers;
    private final int maxInnings;

    MatchFormat(int maxOvers, int maxInnings) {
        this.maxOvers = maxOvers;
        this.maxInnings = maxInnings;
    }

    /** Overs per innings; 0 means unlimited (multi-day formats). */
    public int maxOvers() {
        return maxOvers;
    }

    public int maxInnings() {
        return maxInnings;
    }

    public boolean isLimitedOvers() {
        return maxOvers > 0;
    }
}
