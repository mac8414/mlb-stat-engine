package org.example.model;

import java.util.List;

public class PlayerStatsResponse {
    // Set when disambiguation is needed
    public List<PlayerMatch> multipleMatches;

    // Set when stats are found
    public int playerId;
    public String fullName, position, team, season, asOf;
    public HittingStats hitting;
    public PitchingStats pitching;
    public String error;
}
