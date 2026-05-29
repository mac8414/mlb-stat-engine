package org.example.model;

import java.util.List;

public class RosterResponse {
    public int teamId;
    public String teamName;
    public String teamAbbrev;
    public List<Player> pitchers;
    public List<Player> catchers;
    public List<Player> infielders;
    public List<Player> outfielders;
    public List<Player> dh;

    public static class Player {
        public int id;
        public String name;
        public String jerseyNumber;
        public String position;
        public String positionAbbrev;
        public String bats;
        public String throws_;
        public int age;
        public String height;
        public int weight;
        // hitting
        public String avg;
        public String ops;
        public int hr;
        public int rbi;
        public int sb;
        public int gamesPlayed;
        // pitching
        public String era;
        public String whip;
        public String wins;
        public String losses;
        public int saves;
        public String ip;
        public int so;
        public boolean isPitcher;
    }
}
