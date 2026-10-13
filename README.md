# Office time

A weekly calendar for saying where you'll be each day: **In office**, **Remote** or **Away**.
Spring Boot REST API + Vue 3 frontend, served together as one app.

- Pick a status on any day. Pick the same status again to clear the day.
- Add an optional note to a day ("Team lunch", "Client visit").
- Weeks run Monday to Sunday. The summary counts weekdays you haven't set yet.
- Changes show immediately and save in the background. If a save fails, the day snaps back to what the server has and a message explains.

## What you need

- Java 21
- Maven 3.9+
- Node 20+ (the Maven build runs `npm` for you)

## Run it

```bash
mvn spring-boot:run
```

This installs and builds the frontend, then starts the app at <http://localhost:8080>.

Or build one jar:

```bash
mvn clean package
java -jar target/office-time-0.1.0.jar
```

Data is stored in `./data/office-time*` (an H2 file database) next to wherever you start the app. Delete that folder to start fresh.

## Develop

Run the backend and the frontend dev server separately for hot reload:

```bash
# terminal 1: backend only, no frontend build
mvn spring-boot:run -DskipFrontend=true

# terminal 2: frontend on http://localhost:5173, proxying /api to :8080
cd frontend
npm install
npm run dev
```

## Test

```bash
mvn test -DskipFrontend=true     # backend: API tests with MockMvc and in-memory H2
cd frontend && npm test          # frontend: date logic and the state/save behavior
```

## API

All dates are ISO (`2026-10-07`). A day without an entry is "not set".

| Method   | Path                          | Body                                  | Result                                   |
| -------- | ----------------------------- | ------------------------------------- | ---------------------------------------- |
| `GET`    | `/api/presence?from=…&to=…`   |                                       | `200` entries in the range (max 92 days) |
| `PUT`    | `/api/presence/{date}`        | `{"status":"OFFICE","note":"…"}`      | `200` the saved entry (creates or updates) |
| `DELETE` | `/api/presence/{date}`        |                                       | `204` (also when the day was already empty) |

`status` is `OFFICE`, `REMOTE` or `AWAY`. `note` is optional, up to 200 characters; a blank note is stored as no note.
Invalid input (unknown status, bad date, reversed or oversized range, long note) returns `400`.

## Layout

```
pom.xml                                   Maven build; also builds the frontend into target/classes/static
src/main/java/com/officetime/presence/    entity, repository, service, REST controller
src/test/java/…/PresenceApiTest.java      API tests
frontend/src/App.vue                      page: week header, summary, board
frontend/src/components/                  DayColumn, WeekNav, WeekSummary
frontend/src/usePresence.js               week state, optimistic saves, error recovery
frontend/src/dates.js                     week math (local dates, Monday start, ISO week number)
```

## Not included yet

There is no login: everyone who opens the app shares one calendar. To make it per person, add authentication and a user column on `presence_entry` (the unique key would become user + date).
