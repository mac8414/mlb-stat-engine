# ⚾ MLB Stat Engine

A Fenway Park-inspired MLB stats web application built with **Java Spring Boot** and the free [MLB Stats API](https://statsapi.mlb.com). Runs entirely in the browser — no database, no auth required.

---

## Features

| Tab | Description |
|-----|-------------|
| **Stats** | Look up any player's season stats (hitting or pitching) by name and year. Handles disambiguation when multiple players share a name. |
| **Leaderboards** | Historical ERA and batting average leaders with year range filter. |
| **Scoreboard** | Live Fenway-style scoreboard for any MLB division. Auto-refreshes every 30 seconds with tile-drop animation. |
| **Live Game** | Per-game: balls/strikes/outs indicator lights, base runners, inning half arrows, at-bat/pitching names, pitch count. |
| **Lineup Dropdown** | Click ▾ on any game to expand: batting lineup (highlighted batter), pitcher stats, and play-by-play feed. |
| **Win Probability** | Live win probability bar rendered in each team's colors. |
| **Standings** | Full MLB standings — all 6 divisions, W-L/PCT/GB/WC GB/Streak/Run Differential. |
| **World Series** | Look up any World Series champion by year with team cap logo. |
| **Schedule** | Full 162-game schedule for any team with W/L results, scores, and upcoming games. |
| **Playoff Picture** | AL/NL bracket showing current playoff seeds (div leaders + wild card), clinch indicators, and GB. |

---

## Tech Stack

- **Java 17** + **Spring Boot 3.2**
- **org.json** for MLB API JSON parsing
- **Java HttpClient** for all outbound API calls
- **SLF4J / Logback** for logging
- Single-page frontend: plain **HTML + CSS + Vanilla JS** (`src/main/resources/static/index.html`)
- No database — all data fetched live from `https://statsapi.mlb.com/api/v1/`

---

## Getting Started

### Prerequisites
- Java 17+
- Maven (IntelliJ bundled or system)

### Run

```bash
# Package
mvn package

# Start server
mvn exec:exec
```

App runs on **http://localhost:8080**

### Stop

```bash
# Kill the process on port 8080
lsof -ti:8080 | xargs kill -9
```

---

## Project Structure

```
src/
├── main/
│   ├── java/org/example/
│   │   ├── Main.java                      # Spring Boot entry point
│   │   ├── controller/
│   │   │   └── PlayerController.java      # All REST endpoints
│   │   ├── service/
│   │   │   └── PlayerService.java         # MLB API calls & data mapping
│   │   └── model/
│   │       ├── PlayerStatsResponse.java
│   │       ├── LeadersResponse.java
│   │       ├── ScoreboardGame.java
│   │       ├── LiveGameDetail.java
│   │       ├── GamePlayFeed.java
│   │       ├── StandingsResponse.java
│   │       ├── WorldSeriesResult.java
│   │       ├── TeamScheduleResponse.java
│   │       └── PlayoffPicture.java
│   └── resources/
│       ├── static/index.html              # Entire frontend (HTML + CSS + JS)
│       └── application.properties
logs/
    mlb-stat-engine.log                    # Rolling log file (auto-created)
```

---

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/player?name=&year=&team=` | Player season stats |
| GET | `/api/leaders?startYear=&endYear=&stat=` | Stat leaderboard |
| GET | `/api/scores?division=` | Live scoreboard for a division |
| GET | `/api/standings` | Full MLB standings |
| GET | `/api/worldseries?year=` | World Series result |
| GET | `/api/game/{gamePk}` | Live lineup + pitching for a game |
| GET | `/api/game/{gamePk}/plays` | Play-by-play + win probability |
| GET | `/api/schedule/{teamId}` | Full season schedule for a team |
| GET | `/api/playoff` | Current playoff picture |

---

## Logging

Logs are written to both console and `logs/mlb-stat-engine.log` (rolling, 7-day history).

To increase verbosity for debugging, set in `application.properties`:

```properties
logging.level.org.example=DEBUG
```

---

## Data Source

All data is fetched live from the **free MLB Stats API** — no API key required.

- Base URL: `https://statsapi.mlb.com/api/v1/`
- Team logos: `https://www.mlbstatic.com/team-logos/{teamId}.svg`
- Cap logos: `https://www.mlbstatic.com/team-logos/team-cap-on-dark/{teamId}.svg`
