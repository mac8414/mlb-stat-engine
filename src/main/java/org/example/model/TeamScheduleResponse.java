package org.example.model;

import java.util.List;

public class TeamScheduleResponse {
    public int teamId;
    public String teamName;
    public String teamAbbrev;
    public int wins;
    public int losses;
    public List<ScheduleGame> games;

    public static class ScheduleGame {
        public String date;          // "2026-05-27"
        public String gameTime;      // ISO UTC string for TZ display
        public int opponentId;
        public String opponentName;
        public String opponentAbbrev;
        public boolean isHome;
        public int teamScore;
        public int opponentScore;
        public String result;        // "W", "L", "Upcoming", "Live"
        public int teamWins;         // cumulative record after this game
        public int teamLosses;
        public String tvNetwork;     // e.g. "FS1", "MASN", "" if unknown
    }
}
