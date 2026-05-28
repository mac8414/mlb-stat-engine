package org.example.model;

import java.util.List;

public class GamePlayFeed {
    public double homeWinProbability;   // current, 0–100
    public double awayWinProbability;
    public List<PlayItem> recentPlays;   // last ~8 plays (scoring or notable)
    public List<PlayItem> scoringPlays;  // all scoring plays this game

    public static class PlayItem {
        public String description;
        public String event;
        public int inning;
        public String halfInning;     // "top" or "bottom"
        public int awayScore;
        public int homeScore;
        public int rbi;
        public boolean isScoringPlay;
        public double homeWinProbability;
        public double awayWinProbability;
    }
}
