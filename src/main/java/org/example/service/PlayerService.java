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
}
