package org.example.controller;

import org.example.model.LeadersResponse;
import org.example.model.LiveGameDetail;
import org.example.model.PlayerStatsResponse;
import org.example.model.ScoreboardGame;
import org.example.model.StandingsResponse;
import org.example.model.WorldSeriesResult;
import org.example.service.PlayerService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping("/player")
    public PlayerStatsResponse getPlayer(
            @RequestParam String name,
            @RequestParam String year,
            @RequestParam(required = false) String team) {
        return playerService.getPlayerStats(name, year, team);
    }

    @GetMapping("/leaders")
    public LeadersResponse getLeaders(
            @RequestParam String startYear,
            @RequestParam String endYear,
            @RequestParam String stat) {
        return playerService.getLeaders(startYear, endYear, stat);
    }

    @GetMapping("/scores")
    public List<ScoreboardGame> getScores(@RequestParam String division) {
        return playerService.getScores(division);
    }

    @GetMapping("/worldseries")
    public WorldSeriesResult getWorldSeries(@RequestParam String year) {
        return playerService.getWorldSeries(year);
    }

    @GetMapping("/standings")
    public StandingsResponse getStandings() {
        return playerService.getStandings();
    }

    @GetMapping("/game/{gamePk}")
    public LiveGameDetail getLiveGame(@PathVariable int gamePk) {
        return playerService.getLiveGame(gamePk);
    }
}
