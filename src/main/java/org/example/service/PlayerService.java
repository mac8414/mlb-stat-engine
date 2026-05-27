package org.example.service;

import org.example.model.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class PlayerService {

    private final HttpClient client = HttpClient.newHttpClient();

    public PlayerStatsResponse getPlayerStats(String name, String year, String teamHint) {
        PlayerStatsResponse response = new PlayerStatsResponse();
        try {
            String searchUrl = "https://statsapi.mlb.com/api/v1/people/search?names="
                    + name.replace(" ", "+") + "&sportIds=1";
            JSONArray people = new JSONObject(get(searchUrl)).optJSONArray("people");

            if (people == null || people.isEmpty()) {
                response.error = "No players found for: " + name;
                return response;
            }

            JSONObject player;
            if (people.length() > 1 && (teamHint == null || teamHint.isBlank())) {
                // Need  return candidate listdisambiguation 
                List<PlayerMatch> matches = new ArrayList<>();
                for (int i = 0; i < people.length(); i++) {
                    JSONObject p = people.getJSONObject(i);
                    PlayerMatch m = new PlayerMatch();
                    m.id = p.getInt("id");
                    m.fullName = p.getString("fullName");
                    m.team = getTeamForYear(m.id, year);
                    matches.add(m);
                }
                response.multipleMatches = matches;
                return response;
            } else if (people.length() > 1) {
                // Use teamHint to pick the right player
                JSONObject match = null;
                for (int i = 0; i < people.length(); i++) {
                    JSONObject p = people.getJSONObject(i);
                    String team = getTeamForYear(p.getInt("id"), year);
                    if (team.toLowerCase().contains(teamHint.toLowerCase())) {
                        match = p;
                        break;
                    }
                }
                player = match != null ? match : people.getJSONObject(0);
            } else {
                player = people.getJSONObject(0);
            }

            int id = player.getInt("id");
            response.playerId = id;
            response.fullName = player.getString("fullName");
            response.position = player.optJSONObject("primaryPosition") != null
                    ? player.getJSONObject("primaryPosition").getString("name") : "Unknown";
            response.season = year;

            int currentYear = Year.now().getValue();
            response.asOf = year.equals(String.valueOf(currentYear))
                    ? LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
                    : "End of season";

            String statsUrl = "https://statsapi.mlb.com/api/v1/people/" + id
                    + "/stats?stats=season&group=hitting,pitching&season=" + year;
            JSONArray statsArray = new JSONObject(get(statsUrl)).optJSONArray("stats");

            if (statsArray == null || statsArray.isEmpty()) {
                response.error = "No stats found for " + response.fullName + " in " + year + ".";
                return response;
            }

            for (int i = 0; i < statsArray.length(); i++) {
                JSONObject statGroup = statsArray.getJSONObject(i);
                String group = statGroup.getJSONObject("group").getString("displayName");
                JSONArray splits = statGroup.optJSONArray("splits");
                if (splits == null || splits.isEmpty()) continue;

                JSONObject stat = splits.getJSONObject(0).getJSONObject("stat");
                if (response.team == null && splits.getJSONObject(0).optJSONObject("team") != null) {
                    response.team = splits.getJSONObject(0).getJSONObject("team").getString("name");
                }

                if (group.equals("hitting")) {
                    HittingStats h = new HittingStats();
                    h.games = stat.optInt("gamesPlayed");
                    h.avg = stat.optString("avg", "---");
                    h.obp = stat.optString("obp", "---");
                    h.slg = stat.optString("slg", "---");
                    h.ops = stat.optString("ops", "---");
                    h.homeRuns = stat.optInt("homeRuns");
                    h.rbi = stat.optInt("rbi");
                    h.runs = stat.optInt("runs");
                    h.stolenBases = stat.optInt("stolenBases");
                    h.strikeOuts = stat.optInt("strikeOuts");
                    response.hitting = h;
                } else if (group.equals("pitching")) {
                    PitchingStats p = new PitchingStats();
                    p.games = stat.optInt("gamesPlayed");
                    p.wins = stat.optInt("wins");
                    p.losses = stat.optInt("losses");
                    p.era = stat.optString("era", "---");
                    p.whip = stat.optString("whip", "---");
                    p.inningsPitched = stat.optString("inningsPitched", "---");
                    p.strikeOuts = stat.optInt("strikeOuts");
                    p.walks = stat.optInt("baseOnBalls");
                    response.pitching = p;
                }
            }

            if (response.hitting == null && response.pitching == null) {
                response.error = "No stats found for " + response.fullName + " in " + year + ".";
            }

            // Fetch career best batting average
            try {
                String careerUrl = "https://statsapi.mlb.com/api/v1/people/" + id
                        + "/stats?stats=yearByYear&group=hitting&sportId=1";
                JSONArray careerStats = new JSONObject(get(careerUrl)).optJSONArray("stats");
                if (careerStats != null && !careerStats.isEmpty()) {
                    JSONArray splits = careerStats.getJSONObject(0).optJSONArray("splits");
                    double bestAvg = -1;
                    String bestYear = null;
                    if (splits != null) {
                        for (int i = 0; i < splits.length(); i++) {
                            JSONObject split = splits.getJSONObject(i);
                            String avgStr = split.getJSONObject("stat").optString("avg", "");
                            String splitYear = split.optString("season", "");
                            int atBats = split.getJSONObject("stat").optInt("atBats", 0);
                            if (!avgStr.isEmpty() && atBats >= 50) {
                                try {
                                    double avg = Double.parseDouble(avgStr);
                                    if (avg > bestAvg) { bestAvg = avg; bestYear = splitYear; }
                                } catch (NumberFormatException ignored) {}
                            }
                        }
                    }
                    if (bestYear != null) {
                        response.careerBestAvg = String.format("%.3f", bestAvg);
                        response.careerBestAvgYear = bestYear;
                    }
                }
            } catch (Exception ignored) {}

            // Fetch career best ERA
            try {
                String pitchingCareerUrl = "https://statsapi.mlb.com/api/v1/people/" + id
                        + "/stats?stats=yearByYear&group=pitching&sportId=1";
                JSONArray careerPitching = new JSONObject(get(pitchingCareerUrl)).optJSONArray("stats");
                if (careerPitching != null && !careerPitching.isEmpty()) {
                    JSONArray splits = careerPitching.getJSONObject(0).optJSONArray("splits");
                    double bestEra = Double.MAX_VALUE;
                    String bestYear = null;
                    if (splits != null) {
                        for (int i = 0; i < splits.length(); i++) {
                            JSONObject split = splits.getJSONObject(i);
                            String eraStr = split.getJSONObject("stat").optString("era", "");
                            String splitYear = split.optString("season", "");
                            // Require minimum innings to avoid fluky small samples
                            String ipStr = split.getJSONObject("stat").optString("inningsPitched", "0");
                            double ip = Double.parseDouble(ipStr.isEmpty() ? "0" : ipStr);
                            if (!eraStr.isEmpty() && ip >= 30) {
                                try {
                                    double era = Double.parseDouble(eraStr);
                                    if (era < bestEra) { bestEra = era; bestYear = splitYear; }
                                } catch (NumberFormatException ignored) {}
                            }
                        }
                    }
                    if (bestYear != null) {
                        response.careerBestEra = String.format("%.2f", bestEra);
                        response.careerBestEraYear = bestYear;
                    }
                }
            } catch (Exception ignored) {}
        } catch (Exception e) {
            response.error = "Error fetching stats: " + e.getMessage();
        }
        return response;
    }

    public LeadersResponse getLeaders(String startYearStr, String endYearStr, String statType) {
        LeadersResponse response = new LeadersResponse();
        try {
            int startYear = Integer.parseInt(startYearStr.trim());
            int endYear = Integer.parseInt(endYearStr.trim());
            if (endYear < startYear) endYear = startYear;

            boolean isEra   = statType.equalsIgnoreCase("ERA");
            boolean isEraRp = statType.equalsIgnoreCase("ERA_RP");
            boolean isPitching = isEra || isEraRp;
            response.statLabel = isPitching ? "ERA" : "AVG";
            response.title = isEra ? "ERA Leaders — Starters (Lowest)"
                    : isEraRp ? "ERA Leaders — Relievers (Lowest)"
                    : "Batting Average Leaders";
            response.yearRange = startYear == endYear ? String.valueOf(startYear) : startYear + "-" + endYear;

            List<double[]> values = new ArrayList<>();
            List<LeaderEntry> entries = new ArrayList<>();

            // MLB qualification thresholds:
            //   Batting: 3.1 PA/game = 502 PA over 162 games
            //   Starters ERA: 1 IP/game = 162 IP over 162 games
            //   Relievers ERA: 50–161 IP (meaningful sample, excludes starters)
            final int MIN_PA          = 502;
            final double MIN_IP_SP    = 162.0;
            final double MIN_IP_RP    = 50.0;
            final double MAX_IP_RP    = 161.2;

            for (int year = startYear; year <= endYear; year++) {
                if (isPitching) {
                    String url = "https://statsapi.mlb.com/api/v1/stats"
                            + "?stats=season&group=pitching&season=" + year
                            + "&sportIds=1&playerPool=all&limit=1000&sortStat=era&order=asc";
                    JSONObject json = new JSONObject(get(url));
                    JSONArray statsArr = json.optJSONArray("stats");
                    if (statsArr == null || statsArr.isEmpty()) continue;
                    JSONArray splits = statsArr.getJSONObject(0).optJSONArray("splits");
                    if (splits == null) continue;

                    for (int i = 0; i < splits.length(); i++) {
                        JSONObject split = splits.getJSONObject(i);
                        JSONObject stat = split.getJSONObject("stat");
                        String eraStr = stat.optString("era", "");
                        String ipStr  = stat.optString("inningsPitched", "0");
                        if (eraStr.isEmpty()) continue;
                        try {
                            double era = Double.parseDouble(eraStr);
                            double ip  = Double.parseDouble(ipStr.isEmpty() ? "0" : ipStr);
                            // Apply IP window based on starter vs reliever mode
                            if (isEra   && ip < MIN_IP_SP) continue;
                            if (isEraRp && (ip < MIN_IP_RP || ip > MAX_IP_RP)) continue;
                            String playerName = split.getJSONObject("player").getString("fullName");
                            String team = split.optJSONObject("team") != null
                                    ? split.getJSONObject("team").getString("name") : "---";
                            LeaderEntry entry = new LeaderEntry();
                            entry.playerName = playerName;
                            entry.team = team;
                            entry.value = eraStr;
                            entry.season = String.valueOf(year);
                            values.add(new double[]{era});
                            entries.add(entry);
                        } catch (NumberFormatException ignored) {}
                    }
                } else {
                    // Fetch all hitters via stats endpoint; apply 502 PA minimum (MLB standard qualifier).
                    String url = "https://statsapi.mlb.com/api/v1/stats"
                            + "?stats=season&group=hitting&season=" + year
                            + "&sportIds=1&playerPool=all&limit=1000&sortStat=avg&order=desc";
                    JSONObject json = new JSONObject(get(url));
                    JSONArray statsArr = json.optJSONArray("stats");
                    if (statsArr == null || statsArr.isEmpty()) continue;
                    JSONArray splits = statsArr.getJSONObject(0).optJSONArray("splits");
                    if (splits == null) continue;

                    for (int i = 0; i < splits.length(); i++) {
                        JSONObject split = splits.getJSONObject(i);
                        JSONObject stat = split.getJSONObject("stat");
                        String avgStr = stat.optString("avg", "");
                        int pa = stat.optInt("plateAppearances", 0);
                        if (avgStr.isEmpty() || pa < MIN_PA) continue;
                        try {
                            double avg = Double.parseDouble(avgStr);
                            String playerName = split.getJSONObject("player").getString("fullName");
                            String team = split.optJSONObject("team") != null
                                    ? split.getJSONObject("team").getString("name") : "---";
                            LeaderEntry entry = new LeaderEntry();
                            entry.playerName = playerName;
                            entry.team = team;
                            entry.value = avgStr;
                            entry.season = String.valueOf(year);
                            values.add(new double[]{avg});
                            entries.add(entry);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }

            if (entries.isEmpty()) {
                response.error = "No leaders found for " + response.yearRange + ".";
                return response;
            }

            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < entries.size(); i++) indices.add(i);
            indices.sort((a, b) -> isPitching
                    ? Double.compare(values.get(a)[0], values.get(b)[0])
                    : Double.compare(values.get(b)[0], values.get(a)[0]));

            List<LeaderEntry> top = new ArrayList<>();
            for (int i = 0; i < Math.min(5, indices.size()); i++) {
                LeaderEntry e = entries.get(indices.get(i));
                e.rank = i + 1;
                top.add(e);
            }
            response.leaders = top;

        } catch (NumberFormatException e) {
            response.error = "Invalid year format.";
        } catch (Exception e) {
            response.error = "Error fetching leaders: " + e.getMessage();
        }
        return response;
    }

    private String getTeamForYear(int playerId, String year) {
        try {
            String url = "https://statsapi.mlb.com/api/v1/people/" + playerId
                    + "/stats?stats=season&group=hitting,pitching&season=" + year;
            JSONArray stats = new JSONObject(get(url)).optJSONArray("stats");
            if (stats != null) {
                for (int i = 0; i < stats.length(); i++) {
                    JSONArray splits = stats.getJSONObject(i).optJSONArray("splits");
                    if (splits != null && !splits.isEmpty()) {
                        JSONObject team = splits.getJSONObject(0).optJSONObject("team");
                        if (team != null) return team.getString("name");
                    }
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    private String get(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    public List<ScoreboardGame> getScores(String division) {
        List<ScoreboardGame> games = new ArrayList<>();
        try {
            // Map short division name to MLB API division name substring
            String divFilter = switch (division.toUpperCase().replace("-", " ").trim()) {
                case "AL EAST"    -> "American League East";
                case "AL CENTRAL" -> "American League Central";
                case "AL WEST"    -> "American League West";
                case "NL EAST"    -> "National League East";
                case "NL CENTRAL" -> "National League Central";
                case "NL WEST"    -> "National League West";
                default           -> division;
            };

            String today = LocalDate.now().toString();
            String url = "https://statsapi.mlb.com/api/v1/schedule?sportId=1&date=" + today
                    + "&hydrate=linescore,team";
            JSONObject json = new JSONObject(get(url));
            JSONArray dates = json.optJSONArray("dates");
            if (dates == null || dates.isEmpty()) return games;

            JSONArray gameList = dates.getJSONObject(0).optJSONArray("games");
            if (gameList == null) return games;

            for (int i = 0; i < gameList.length(); i++) {
                JSONObject g = gameList.getJSONObject(i);
                JSONObject away = g.getJSONObject("teams").getJSONObject("away");
                JSONObject home = g.getJSONObject("teams").getJSONObject("home");
                JSONObject awayTeam = away.getJSONObject("team");
                JSONObject homeTeam = home.getJSONObject("team");

                String awayDiv = awayTeam.optJSONObject("division") != null
                        ? awayTeam.getJSONObject("division").optString("name", "") : "";
                String homeDiv = homeTeam.optJSONObject("division") != null
                        ? homeTeam.getJSONObject("division").optString("name", "") : "";

                if (!awayDiv.equals(divFilter) && !homeDiv.equals(divFilter)) continue;

                ScoreboardGame sg = new ScoreboardGame();
                sg.gamePk     = g.optInt("gamePk", 0);
                sg.awayAbbrev = awayTeam.optString("abbreviation", awayTeam.getString("name"));
                sg.homeAbbrev = homeTeam.optString("abbreviation", homeTeam.getString("name"));
                sg.awayName   = awayTeam.getString("name");
                sg.homeName   = homeTeam.getString("name");
                sg.awayScore  = away.optInt("score", 0);
                sg.homeScore  = home.optInt("score", 0);

                String state = g.getJSONObject("status").getString("detailedState");
                sg.isFinal = state.equals("Final") || state.equals("Game Over");
                sg.isLive  = state.equals("In Progress") || state.contains("Warmup") || state.contains("Delayed");
                sg.status  = sg.isFinal ? "FINAL" : sg.isLive ? "LIVE" : state.toUpperCase();

                JSONObject ls = g.optJSONObject("linescore");
                sg.awayInnings = new ArrayList<>();
                sg.homeInnings = new ArrayList<>();

                if (ls != null) {
                    JSONArray innings = ls.optJSONArray("innings");
                    if (innings != null) {
                        for (int j = 0; j < innings.length(); j++) {
                            JSONObject inn = innings.getJSONObject(j);
                            sg.awayInnings.add(inn.getJSONObject("away").optInt("runs", -1));
                            sg.homeInnings.add(inn.getJSONObject("home").optInt("runs", -1));
                        }
                    }
                    JSONObject awayTotals = ls.optJSONObject("teams") != null
                            ? ls.getJSONObject("teams").optJSONObject("away") : null;
                    JSONObject homeTotals = ls.optJSONObject("teams") != null
                            ? ls.getJSONObject("teams").optJSONObject("home") : null;
                    if (awayTotals != null) {
                        sg.awayHits   = awayTotals.optInt("hits", 0);
                        sg.awayErrors = awayTotals.optInt("errors", 0);
                    }
                    if (homeTotals != null) {
                        sg.homeHits   = homeTotals.optInt("hits", 0);
                        sg.homeErrors = homeTotals.optInt("errors", 0);
                    }
                    int inningNum = ls.optInt("currentInning", 0);
                    String inningHalf = ls.optString("inningHalf", "");
                    if (sg.isFinal) {
                        sg.currentInning = "FINAL";
                    } else if (inningNum > 0) {
                        sg.currentInning = inningHalf.toUpperCase() + " " + ordinal(inningNum);
                    }

                    // Live game state: count, batter, pitcher, baserunners
                    if (sg.isLive) {
                        sg.balls   = ls.optInt("balls", 0);
                        sg.strikes = ls.optInt("strikes", 0);
                        sg.outs    = ls.optInt("outs", 0);
                        JSONObject offense = ls.optJSONObject("offense");
                        JSONObject defense = ls.optJSONObject("defense");
                        if (offense != null) {
                            JSONObject batter = offense.optJSONObject("batter");
                            if (batter != null) sg.batterName = batter.optString("fullName", "");
                            sg.runnerOnFirst  = offense.optJSONObject("first")  != null;
                            sg.runnerOnSecond = offense.optJSONObject("second") != null;
                            sg.runnerOnThird  = offense.optJSONObject("third")  != null;
                        }
                        if (defense != null) {
                            JSONObject pitcher = defense.optJSONObject("pitcher");
                            if (pitcher != null) sg.pitcherName = pitcher.optString("fullName", "");
                        }
                    }
                }

                // Pass raw UTC ISO string to frontend for local-timezone display
                String gameDate = g.optString("gameDate", "");
                if (!gameDate.isEmpty() && !sg.isLive && !sg.isFinal) {
                    sg.status = gameDate; // frontend will parse and format in user's local TZ
                }

                games.add(sg);
            }
        } catch (Exception e) {
            // Return what we have on error
        }
        return games;
    }

    private String ordinal(int n) {
        return switch (n) {
            case 1 -> "1ST"; case 2 -> "2ND"; case 3 -> "3RD";
            default -> n + "TH";
        };
    }

    public WorldSeriesResult getWorldSeries(String yearStr) {
        WorldSeriesResult result = new WorldSeriesResult();
        result.year = yearStr.trim();
        try {
            String url = "https://statsapi.mlb.com/api/v1/schedule?sportId=1&season="
                    + result.year + "&gameType=W&hydrate=team";
            JSONObject json = new JSONObject(get(url));
            JSONArray dates = json.optJSONArray("dates");
            if (dates == null || dates.isEmpty()) {
                result.error = "No World Series data found for " + result.year + ".";
                return result;
            }

            // Collect all WS games and find the clinching game (highest seriesGameNumber)
            JSONObject clincher = null;
            int maxGame = 0;
            for (int i = 0; i < dates.length(); i++) {
                JSONArray games = dates.getJSONObject(i).optJSONArray("games");
                if (games == null) continue;
                for (int j = 0; j < games.length(); j++) {
                    JSONObject g = games.getJSONObject(j);
                    int gameNum = g.optInt("seriesGameNumber", 0);
                    if (gameNum > maxGame) { maxGame = gameNum; clincher = g; }
                }
            }
            if (clincher == null) {
                result.error = "Could not determine World Series winner for " + result.year + ".";
                return result;
            }

            JSONObject away = clincher.getJSONObject("teams").getJSONObject("away");
            JSONObject home = clincher.getJSONObject("teams").getJSONObject("home");
            String awayName = away.getJSONObject("team").getString("name");
            String homeName = home.getJSONObject("team").getString("name");
            int awayScore = away.optInt("score", 0);
            int homeScore = home.optInt("score", 0);
            boolean awayWon = awayScore > homeScore;

            // Count wins for each side across all games
            int awayWins = 0, homeWins = 0;
            for (int i = 0; i < dates.length(); i++) {
                JSONArray games = dates.getJSONObject(i).optJSONArray("games");
                if (games == null) continue;
                for (int j = 0; j < games.length(); j++) {
                    JSONObject g = games.getJSONObject(j);
                    JSONObject a = g.getJSONObject("teams").getJSONObject("away");
                    JSONObject h = g.getJSONObject("teams").getJSONObject("home");
                    String state = g.getJSONObject("status").getString("detailedState");
                    if (!state.equals("Final") && !state.equals("Game Over")) continue;
                    if (a.optInt("score", 0) > h.optInt("score", 0)) awayWins++;
                    else homeWins++;
                }
            }

            result.winner   = awayWon ? awayName : homeName;
            result.loser    = awayWon ? homeName : awayName;
            result.winnerId = awayWon
                    ? away.getJSONObject("team").optInt("id", 0)
                    : home.getJSONObject("team").optInt("id", 0);

            // Count wins per team name (home/away flips across games)
            int winnerWins = 0, loserWins = 0;
            for (int i = 0; i < dates.length(); i++) {
                JSONArray games = dates.getJSONObject(i).optJSONArray("games");
                if (games == null) continue;
                for (int j = 0; j < games.length(); j++) {
                    JSONObject g = games.getJSONObject(j);
                    JSONObject a = g.getJSONObject("teams").getJSONObject("away");
                    JSONObject h = g.getJSONObject("teams").getJSONObject("home");
                    String state = g.getJSONObject("status").getString("detailedState");
                    if (!state.equals("Final") && !state.equals("Game Over")) continue;
                    int aScore = a.optInt("score", 0);
                    int hScore = h.optInt("score", 0);
                    String winningTeam = aScore > hScore
                            ? a.getJSONObject("team").getString("name")
                            : h.getJSONObject("team").getString("name");
                    if (winningTeam.equals(result.winner)) winnerWins++;
                    else loserWins++;
                }
            }
            result.seriesResult = winnerWins + "-" + loserWins;
            result.clinchDate = clincher.optString("officialDate", "");
        } catch (Exception e) {
            result.error = "Error fetching World Series data: " + e.getMessage();
        }
        return result;
    }

    public StandingsResponse getStandings() {
        StandingsResponse response = new StandingsResponse();
        response.divisions = new ArrayList<>();
        try {
            int year = java.time.Year.now().getValue();
            String url = "https://statsapi.mlb.com/api/v1/standings?leagueId=103,104&season=" + year
                    + "&standingsTypes=regularSeason&hydrate=team,division,league";
            JSONObject json = new JSONObject(get(url));
            JSONArray records = json.optJSONArray("records");
            if (records == null) {
                response.error = "No standings available.";
                return response;
            }

            // Order: AL East, AL Central, AL West, NL East, NL Central, NL West
            List<StandingsResponse.DivisionStandings> al = new ArrayList<>();
            List<StandingsResponse.DivisionStandings> nl = new ArrayList<>();

            for (int i = 0; i < records.length(); i++) {
                JSONObject rec = records.getJSONObject(i);
                String divName = rec.getJSONObject("division").getString("name");
                String lgName  = rec.getJSONObject("league").getString("name");

                StandingsResponse.DivisionStandings div = new StandingsResponse.DivisionStandings();
                div.division = divName;
                div.league   = lgName;
                div.teams    = new ArrayList<>();

                JSONArray teamRecords = rec.optJSONArray("teamRecords");
                if (teamRecords != null) {
                    for (int j = 0; j < teamRecords.length(); j++) {
                        JSONObject tr = teamRecords.getJSONObject(j);
                        StandingsResponse.TeamStanding ts = new StandingsResponse.TeamStanding();
                        ts.teamName     = tr.getJSONObject("team").getString("name");
                        ts.teamId       = tr.getJSONObject("team").optInt("id", 0);
                        ts.wins         = tr.optInt("wins", 0);
                        ts.losses       = tr.optInt("losses", 0);
                        ts.pct          = tr.optString("winningPercentage", ".000");
                        ts.gb           = tr.optString("gamesBack", "-");
                        ts.wcGb         = tr.optString("wildCardGamesBack", "-");
                        ts.divisionRank = tr.optInt("divisionRank", 0);
                        ts.leagueRank   = tr.optInt("leagueRank", 0);
                        ts.runDiff      = tr.optInt("runDifferential", 0);
                        JSONObject streak = tr.optJSONObject("streak");
                        ts.streak = streak != null ? streak.optString("streakCode", "-") : "-";
                        div.teams.add(ts);
                    }
                }

                if (lgName.contains("American")) al.add(div);
                else nl.add(div);
            }
            // Sort each league: East, Central, West
            al.sort((a, b) -> divOrder(a.division) - divOrder(b.division));
            nl.sort((a, b) -> divOrder(a.division) - divOrder(b.division));
            response.divisions.addAll(al);
            response.divisions.addAll(nl);
        } catch (Exception e) {
            response.error = "Error fetching standings: " + e.getMessage();
        }
        return response;
    }

    private int divOrder(String name) {
        if (name.contains("East"))    return 0;
        if (name.contains("Central")) return 1;
        return 2;
    }

    public LiveGameDetail getLiveGame(int gamePk) {
        LiveGameDetail detail = new LiveGameDetail();
        detail.awayBatters  = new ArrayList<>();
        detail.homeBatters  = new ArrayList<>();
        detail.awayPitchers = new ArrayList<>();
        detail.homePitchers = new ArrayList<>();
        try {
            String url = "https://statsapi.mlb.com/api/v1/game/" + gamePk + "/boxscore";
            JSONObject box = new JSONObject(get(url));
            JSONObject teams = box.getJSONObject("teams");

            // Determine current batter, on-deck batter, and pitcher from linescore
            int currentBatterId = -1;
            int onDeckBatterId  = -1;
            String currentPitcherName = "";
            try {
                String lsUrl = "https://statsapi.mlb.com/api/v1/game/" + gamePk + "/linescore";
                JSONObject ls = new JSONObject(get(lsUrl));
                JSONObject offense = ls.optJSONObject("offense");
                JSONObject defense = ls.optJSONObject("defense");
                if (offense != null) {
                    JSONObject batter = offense.optJSONObject("batter");
                    if (batter != null) currentBatterId = batter.optInt("id", -1);
                    JSONObject onDeck = offense.optJSONObject("onDeck");
                    if (onDeck != null) onDeckBatterId = onDeck.optInt("id", -1);
                }
                if (defense != null) {
                    JSONObject pitcher = defense.optJSONObject("pitcher");
                    if (pitcher != null) currentPitcherName = pitcher.optString("fullName", "");
                }
            } catch (Exception ignored) {}

            for (String side : new String[]{"away", "home"}) {
                JSONObject team = teams.getJSONObject(side);
                JSONObject players = team.getJSONObject("players");
                String teamName = team.getJSONObject("team").optString("name", side);
                if (side.equals("away")) detail.awayName = teamName;
                else detail.homeName = teamName;

                // Batting order — collect all batters with a battingOrder field, sorted by it
                List<LiveGameDetail.BatterLine> sideList = new ArrayList<>();
                JSONArray batters = team.optJSONArray("batters");
                if (batters != null) {
                    for (int i = 0; i < batters.length(); i++) {
                        int pid = batters.getInt(i);
                        JSONObject p = players.optJSONObject("ID" + pid);
                        if (p == null) continue;
                        JSONObject stats = p.optJSONObject("stats");
                        if (stats == null) continue;
                        JSONObject bat = stats.optJSONObject("batting");
                        if (bat == null) continue;
                        int battingOrder = p.optInt("battingOrder", 0);
                        if (battingOrder == 0) continue; // not in lineup

                        String pos = "";
                        JSONArray allPos = p.optJSONArray("allPositions");
                        if (allPos != null && allPos.length() > 0) {
                            pos = allPos.getJSONObject(0).optString("abbreviation", "");
                        } else if (p.optJSONObject("position") != null) {
                            pos = p.getJSONObject("position").optString("abbreviation", "");
                        }

                        LiveGameDetail.BatterLine bl = new LiveGameDetail.BatterLine();
                        bl.name           = p.getJSONObject("person").optString("fullName", "");
                        bl.position       = pos;
                        bl.battingOrder   = battingOrder;
                        bl.isCurrentBatter = (pid == currentBatterId);
                        bl.isOnDeck       = (pid == onDeckBatterId);
                        bl.ab  = bat.optInt("atBats", 0);
                        bl.h   = bat.optInt("hits", 0);
                        bl.hr  = bat.optInt("homeRuns", 0);
                        bl.rbi = bat.optInt("rbi", 0);
                        bl.bb  = bat.optInt("baseOnBalls", 0);
                        bl.k   = bat.optInt("strikeOuts", 0);
                        bl.r   = bat.optInt("runs", 0);
                        sideList.add(bl);
                    }
                }
                // Sort by battingOrder (100, 200, ...) then substitutes after (1000+)
                sideList.sort((a, b) -> Integer.compare(a.battingOrder, b.battingOrder));
                if (side.equals("away")) detail.awayBatters = sideList;
                else detail.homeBatters = sideList;

                // Pitchers in order used
                JSONArray pitchers = team.optJSONArray("pitchers");
                if (pitchers != null) {
                    for (int i = 0; i < pitchers.length(); i++) {
                        JSONObject p = players.optJSONObject("ID" + pitchers.getInt(i));
                        if (p == null) continue;
                        JSONObject stats = p.optJSONObject("stats");
                        if (stats == null) continue;
                        JSONObject pit = stats.optJSONObject("pitching");
                        if (pit == null) continue;
                        LiveGameDetail.PitcherLine pl = new LiveGameDetail.PitcherLine();
                        pl.name    = p.getJSONObject("person").optString("fullName", "");
                        pl.ip      = pit.optString("inningsPitched", "0.0");
                        pl.h       = pit.optInt("hits", 0);
                        pl.er      = pit.optInt("earnedRuns", 0);
                        pl.bb      = pit.optInt("baseOnBalls", 0);
                        pl.k       = pit.optInt("strikeOuts", 0);
                        pl.pitches = pit.optInt("numberOfPitches", 0);
                        pl.isCurrent = !currentPitcherName.isEmpty() && pl.name.equals(currentPitcherName);
                        if (side.equals("away")) detail.awayPitchers.add(pl);
                        else detail.homePitchers.add(pl);
                    }
                }
            }
        } catch (Exception e) {
            // Return partial data on error
        }
        return detail;
    }
}