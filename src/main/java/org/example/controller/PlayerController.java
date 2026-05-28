package org.example.controller;

import org.example.model.GamePlayFeed;
import org.example.model.LeadersResponse;
import org.example.model.LiveGameDetail;
import org.example.model.PlayoffPicture;
import org.example.model.PlayerStatsResponse;
import org.example.model.ScoreboardGame;
import org.example.model.StandingsResponse;
import org.example.model.TeamScheduleResponse;
import org.example.model.WorldSeriesResult;
import org.example.service.PlayerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PlayerController {

    private static final Logger log = LoggerFactory.getLogger(PlayerController.class);

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping("/player")
    public PlayerStatsResponse getPlayer(
            @RequestParam String name,
            @RequestParam String year,
            @RequestParam(required = false) String team) {
        log.info("GET /api/player name={} year={} team={}", name, year, team);
        return playerService.getPlayerStats(name, year, team);
    }

    @GetMapping("/leaders")
    public LeadersResponse getLeaders(
            @RequestParam String startYear,
            @RequestParam String endYear,
            @RequestParam String stat) {
        log.info("GET /api/leaders stat={} {}-{}", stat, startYear, endYear);
        return playerService.getLeaders(startYear, endYear, stat);
    }

    @GetMapping("/scores")
    public List<ScoreboardGame> getScores(@RequestParam String division,
                                          @RequestParam(required = false) String date) {
        log.info("GET /api/scores division={} date={}", division, date);
        return playerService.getScores(division, date);
    }

    @GetMapping("/worldseries")
    public WorldSeriesResult getWorldSeries(@RequestParam String year) {
        log.info("GET /api/worldseries year={}", year);
        return playerService.getWorldSeries(year);
    }

    @GetMapping("/standings")
    public StandingsResponse getStandings() {
        log.info("GET /api/standings");
        return playerService.getStandings();
    }

    @GetMapping("/game/{gamePk}")
    public LiveGameDetail getLiveGame(@PathVariable int gamePk) {
        log.info("GET /api/game/{}", gamePk);
        return playerService.getLiveGame(gamePk);
    }

    @GetMapping("/game/{gamePk}/plays")
    public GamePlayFeed getPlayFeed(@PathVariable int gamePk) {
        log.info("GET /api/game/{}/plays", gamePk);
        return playerService.getPlayFeed(gamePk);
    }

    @GetMapping("/schedule/{teamId}")
    public TeamScheduleResponse getTeamSchedule(@PathVariable int teamId) {
        log.info("GET /api/schedule/{}", teamId);
        return playerService.getTeamSchedule(teamId);
    }

    @GetMapping("/playoff")
    public PlayoffPicture getPlayoffPicture() {
        log.info("GET /api/playoff");
        return playerService.getPlayoffPicture();
    }
}
