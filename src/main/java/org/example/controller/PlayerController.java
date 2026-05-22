package org.example.controller;

import org.example.model.LeadersResponse;
import org.example.model.PlayerStatsResponse;
import org.example.service.PlayerService;
import org.springframework.web.bind.annotation.*;

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
}
