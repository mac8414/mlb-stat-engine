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

            boolean isEra = statType.equalsIgnoreCase("ERA");
            String category = isEra ? "era" : "battingAverage";
            String group = isEra ? "pitching" : "hitting";
            response.statLabel = isEra ? "ERA" : "AVG";
            response.title = isEra ? "ERA Leaders (Lowest)" : "Batting Average Leaders";
            response.yearRange = startYear == endYear ? String.valueOf(startYear) : startYear + "-" + endYear;

            List<double[]> values = new ArrayList<>();
            List<LeaderEntry> entries = new ArrayList<>();

            for (int year = startYear; year <= endYear; year++) {
                String url = "https://statsapi.mlb.com/api/v1/stats/leaders"
                        + "?leaderCategories=" + category
                        + "&season=" + year
                        + "&sportId=1&limit=10&statGroup=" + group;
                JSONObject json = new JSONObject(get(url));
                JSONArray leagueLeaders = json.optJSONArray("leagueLeaders");
                if (leagueLeaders == null || leagueLeaders.isEmpty()) continue;

                JSONArray leaders = leagueLeaders.getJSONObject(0).optJSONArray("leaders");
                if (leaders == null) continue;

                for (int i = 0; i < leaders.length(); i++) {
                    JSONObject leader = leaders.getJSONObject(i);
                    String val = leader.getString("value");
                    try {
                        double numeric = Double.parseDouble(val);
                        LeaderEntry entry = new LeaderEntry();
                        entry.playerName = leader.getJSONObject("person").getString("fullName");
                        entry.team = leader.optJSONObject("team") != null
                                ? leader.getJSONObject("team").getString("name") : "---";
                        entry.value = val;
                        entry.season = String.valueOf(year);
                        values.add(new double[]{numeric});
                        entries.add(entry);
                    } catch (NumberFormatException ignored) {}
                }
            }

            if (entries.isEmpty()) {
                response.error = "No leaders found for " + response.yearRange + ".";
                return response;
            }

            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < entries.size(); i++) indices.add(i);
            indices.sort((a, b) -> isEra
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
