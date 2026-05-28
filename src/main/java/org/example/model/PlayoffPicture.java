package org.example.model;

import java.util.List;

public class PlayoffPicture {
    public List<LeaguePlayoff> leagues;

    public static class LeaguePlayoff {
        public String leagueName;   // "American League" / "National League"
        public String leagueShort;  // "AL" / "NL"
        public List<PlayoffSeed> seeds; // 6 seeds, sorted 1-6
    }

    public static class PlayoffSeed {
        public int seed;
        public int teamId;
        public String teamName;
        public String teamAbbrev;
        public int wins;
        public int losses;
        public String gb;           // games back / games ahead
        public boolean isDivLeader;
        public String divisionName; // for seed 1-3
        public String clinchIndicator; // "z"=div, "y"=league, "x"=playoff, "e"=eliminated, ""=none
        public String magicNumber;
        public String eliminationNumber;
    }
}
