package org.example.model;

import java.util.List;

public class LiveGameDetail {

    public String awayName;
    public String homeName;

    public List<BatterLine> awayBatters;
    public List<BatterLine> homeBatters;
    public List<PitcherLine> awayPitchers;
    public List<PitcherLine> homePitchers;

    public static class BatterLine {
        public String name;
        public String position;
        public int ab;
        public int h;
        public int hr;
        public int rbi;
        public int bb;
        public int k;
        public int r;
        public boolean isCurrentBatter;
        public boolean isOnDeck;
        public int battingOrder; // 100, 200, ... 900
    }

    public static class PitcherLine {
        public String name;
        public String ip;
        public int h;
        public int er;
        public int bb;
        public int k;
        public int pitches;
        public boolean isCurrent;
    }
}
