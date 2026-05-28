package org.example.model;

import java.util.List;

public class PlayerStatsResponse {
    // Set when disambiguation is needed
    public List<PlayerMatch> multipleMatches;

    // Set when stats are found
    public int playerId;
    public String fullName, position, team, season, asOf;
    public String careerBestAvg;
    public String careerBestAvgYear;
    public String careerBestEra;
    public String careerBestEraYear;
    public HittingStats hitting;
    public PitchingStats pitching;
    public RecentForm recentForm;
    public String error;

    public static class RecentForm {
        public SplitLine l7;
        public SplitLine l14;
        public SplitLine l30;
    }

    public static class SplitLine {
        public int games;
        // Hitting
        public String avg;
        public String ops;
        public String obp;
        public String slg;
        public int homeRuns;
        public int rbi;
        public int strikeOuts;
        // Pitching
        public String era;
        public String whip;
        public String inningsPitched;
        public int wins;
        public int losses;
    }
}
