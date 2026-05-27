package org.example.model;

import java.util.List;

public class StandingsResponse {
    public static class TeamStanding {
        public String teamName;
        public int teamId;
        public int wins;
        public int losses;
        public String pct;
        public String gb;
        public String wcGb;
        public int divisionRank;
        public int leagueRank;
        public String streak;
        public int runDiff;
    }

    public static class DivisionStandings {
        public String division;
        public String league;
        public List<TeamStanding> teams;
    }

    public List<DivisionStandings> divisions;
    public String error;
}
