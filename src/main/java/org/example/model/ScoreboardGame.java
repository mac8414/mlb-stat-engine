package org.example.model;

import java.util.List;

public class ScoreboardGame {
    public int    awayTeamId;
    public int    homeTeamId;
    public String awayAbbrev;
    public String homeAbbrev;
    public String awayName;
    public String homeName;
    public String status;
    public int awayScore;
    public int homeScore;
    public int awayHits;
    public int homeHits;
    public int awayErrors;
    public int homeErrors;
    public List<Integer> awayInnings;
    public List<Integer> homeInnings;
    public String currentInning;
    public String gameTime;
    public boolean isLive;
    public boolean isFinal;
    public boolean isTopInning;

    public int gamePk;

    // Live game state (only populated when isLive)
    public int balls;
    public int strikes;
    public int outs;
    public String batterName;
    public String pitcherName;
    public boolean runnerOnFirst;
    public boolean runnerOnSecond;
    public boolean runnerOnThird;
    public String tvNetwork;     // e.g. "FS1", "MASN", "" if no TV info
}
