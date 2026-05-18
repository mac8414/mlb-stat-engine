package org.example.service;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class PlayerService {

    private final HttpClient client = HttpClient.newHttpClient();

    public void lookupPlayer(String name, String year) {
        try {
            // Step 1: Search for player by name
            String searchUrl = "https://statsapi.mlb.com/api/v1/people/search?names="
                    + name.replace(" ", "+") + "&sportIds=1";
            String searchBody = get(searchUrl); // REST api get
            JSONObject searchJson = new JSONObject(searchBody);
            JSONArray people = searchJson.optJSONArray("people");

            if (people == null || people.isEmpty()) {
                System.out.println("No players found for: " + name);
                return;
            }

            // If multiple results, show list and pick first
            if (people.length() > 1) {
                System.out.println("Found " + people.length() + " players:");
                for (int i = 0; i < people.length(); i++) {
                    JSONObject p = people.getJSONObject(i);
                    System.out.printf("  [%d] %s%n", i + 1, p.getString("fullName"));
                }
                System.out.println("Showing stats for: " + people.getJSONObject(0).getString("fullName"));
            }

            JSONObject player = people.getJSONObject(0);
            int id = player.getInt("id");
            String fullName = player.getString("fullName");
            String position = player.optJSONObject("primaryPosition") != null
                    ? player.getJSONObject("primaryPosition").getString("name")
                    : "Unknown";

            System.out.println("\n--- " + fullName + " | " + position + " ---");

            // Step 2: Fetch stats for the requested season
            int currentYear = java.time.Year.now().getValue();
            boolean isCurrentSeason = year.equals(String.valueOf(currentYear));
            String asOf = isCurrentSeason
                    ? java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy"))
                    : "end of season";
            String statsUrl = "https://statsapi.mlb.com/api/v1/people/" + id
                    + "/stats?stats=season&group=hitting,pitching&season=" + year;
            String statsBody = get(statsUrl);
            JSONObject statsJson = new JSONObject(statsBody);
            JSONArray statsArray = statsJson.optJSONArray("stats");

            if (statsArray == null || statsArray.isEmpty()) {
                System.out.println("No stats available for " + year + ".");
                return;
            }

            boolean foundAny = false;
            boolean headerPrinted = false;
            for (int i = 0; i < statsArray.length(); i++) {
                JSONObject statGroup = statsArray.getJSONObject(i);
                String group = statGroup.getJSONObject("group").getString("displayName");
                JSONArray splits = statGroup.optJSONArray("splits");
                if (splits == null || splits.isEmpty()) continue;

                JSONObject stat = splits.getJSONObject(0).getJSONObject("stat");
                String team = splits.getJSONObject(0).optJSONObject("team") != null
                        ? splits.getJSONObject(0).getJSONObject("team").getString("name")
                        : "";

                foundAny = true;
                if (!headerPrinted) {
                    System.out.printf("Team: %s | Season: %s | As of: %s%n", team, year, asOf);
                    headerPrinted = true;
                }
                if (group.equals("hitting")) {
                    System.out.println("[Hitting]");
                    System.out.printf("  G: %-5s  AVG: %-6s  HR: %-5s  RBI: %-5s  R: %-5s%n",
                            stat.optInt("gamesPlayed"),
                            stat.optString("avg", "---"),
                            stat.optInt("homeRuns"),
                            stat.optInt("rbi"),
                            stat.optInt("runs"));
                    System.out.printf("  OBP: %-6s  SLG: %-6s  OPS: %-6s  SB: %-4s  K: %s%n",
                            stat.optString("obp", "---"),
                            stat.optString("slg", "---"),
                            stat.optString("ops", "---"),
                            stat.optInt("stolenBases"),
                            stat.optInt("strikeOuts"));
                } else if (group.equals("pitching")) {
                    System.out.println("[Pitching]");
                    System.out.printf("  G: %-5s  W-L: %s-%s  ERA: %-6s  IP: %-7s%n",
                            stat.optInt("gamesPlayed"),
                            stat.optInt("wins"),
                            stat.optInt("losses"),
                            stat.optString("era", "---"),
                            stat.optString("inningsPitched", "---"));
                    System.out.printf("  SO: %-5s  BB: %-5s  WHIP: %s%n",
                            stat.optInt("strikeOuts"),
                            stat.optInt("baseOnBalls"),
                            stat.optString("whip", "---"));
                }
            }

            if (!foundAny) {
                System.out.println("No stats found for the " + year + " season.");
            }

        } catch (Exception e) {
            System.out.println("Error fetching stats: " + e.getMessage());
        }
    }

    private String get(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }
}
