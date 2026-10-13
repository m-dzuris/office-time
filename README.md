# Office time

A weekly calendar for saying where you'll be each day: **In office**, **Remote** or **Away**.
Spring Boot REST API + Vue 3 frontend, served together as one app.

- Everyone has an account. Each person has one calendar of their own days.
- Pick a status on any day. Pick the same status again to clear the day. Add an optional note.
- **Groups** let people see each other's days. Create one, join with its code, or invite people by email.
- Your days are global: they're stored once, and every group you're in shows the same thing. A group only decides who can see them. Leave a group and it stops seeing you; your days are untouched.
- Notes are private. Groups only ever see statuses.
- Changes show immediately and save in the background. If a save fails, the day snaps back to what the server has and a message explains.

## What you need

- Java 21
- Maven 3.9+ (3.6.3+ works for Spring Boot 3.5)
- Node 20+ (the Maven build runs `npm` for you)

## Run it

```bash
mvn spring-boot:run
```

This installs and builds the frontend, then starts the app at <http://localhost:8080>. Create an account on the first screen.

Or build one jar:

```bash
mvn clean package
java -jar target/office-time-0.1.0.jar
```

Data is stored in `./data/office-time*` (an H2 file database) next to wherever you start the app.

**If you ran the earlier single-user version, delete the `data` folder first.** Its calendar had no owner, and the database can't upgrade that table in place.

Serving over HTTPS? Set `SESSION_COOKIE_SECURE=true` so the sign-in cookie is only sent over HTTPS.

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
cd frontend && npm test          # frontend: dates, state/save behavior, API layer, group helpers
```

## How it works

- **Sign-in** is a session cookie (HttpOnly, SameSite=Lax). Passwords are hashed with bcrypt. Changing requests need the CSRF token, which the page reads from the `XSRF-TOKEN` cookie and sends as `X-XSRF-TOKEN`.
- **Groups** have an owner and members. Any member can invite. Only the owner can remove others, make a new join code, or delete the group. If the owner leaves, the longest-standing member takes over; the last person leaving closes the group.
- **Invitations** are addressed to an email address and appear at the top of the page when someone signs in with it, even if they sign up later. No email is sent.
- A group you're not in looks exactly like one that doesn't exist (`404`).

## API

Everything under `/api` needs a signed-in user, except sign-up, sign-in and `GET /api/auth/me`. Dates are ISO (`2026-10-07`). Errors are `{"message": "…"}`.

| Method   | Path                                   | Notes                                                    |
| -------- | -------------------------------------- | -------------------------------------------------------- |
| `POST`   | `/api/auth/register`                   | `{email, displayName, password}`; signs you in           |
| `POST`   | `/api/auth/login`                      | `{email, password}`                                      |
| `POST`   | `/api/auth/logout`                     | `204`                                                    |
| `GET`    | `/api/auth/me`                         | `200` the user, or `204` when signed out                 |
| `GET`    | `/api/presence?from=…&to=…`            | your own days, notes included (max 92 days)              |
| `PUT`    | `/api/presence/{date}`                 | `{status: OFFICE\|REMOTE\|AWAY, note?}`; creates or updates |
| `DELETE` | `/api/presence/{date}`                 | `204`, also when the day was already empty               |
| `GET`    | `/api/groups`                          | your groups                                              |
| `POST`   | `/api/groups`                          | `{name}`; you become the owner                           |
| `POST`   | `/api/groups/join`                     | `{code}`                                                 |
| `GET`    | `/api/groups/{id}`                     | members, pending invitations, join code (members only)   |
| `DELETE` | `/api/groups/{id}`                     | owner only                                               |
| `POST`   | `/api/groups/{id}/join-code`           | owner only; the old code stops working                   |
| `POST`   | `/api/groups/{id}/invitations`         | `{email}`; any member                                    |
| `DELETE` | `/api/groups/{id}/invitations/{inv}`   | any member                                               |
| `DELETE` | `/api/groups/{id}/members/{userId}`    | your own id to leave; the owner can remove others        |
| `GET`    | `/api/groups/{id}/presence?from=…&to=…`| every member's statuses, no notes                        |
| `GET`    | `/api/invitations`                     | invitations sent to your email                           |
| `POST`   | `/api/invitations/{id}/accept`         |                                                          |
| `DELETE` | `/api/invitations/{id}`                | decline                                                  |

## Layout

```
pom.xml                                   Maven build; also builds the frontend into target/classes/static
src/main/java/com/officetime/auth/        accounts, sign-in, security configuration
src/main/java/com/officetime/group/       groups, memberships, invitations
src/main/java/com/officetime/presence/    each person's days
src/test/java/…                           API tests (auth, groups, presence)
frontend/src/App.vue                      sign-in screen or the app
frontend/src/components/                  OfficeTime (my week), Group* (group views), DayColumn, …
frontend/src/api.js                       all server calls, CSRF and expired-session handling
frontend/src/usePresence.js               my week: state, optimistic saves, error recovery
frontend/src/useGroups.js, useGroupPresence.js, groupView.js   groups and the group table
```

## Known limits

- **Email addresses aren't verified.** Anyone can register with any address, so anyone who registers an invited address can accept that invitation. Treat invitations as convenience, not proof of identity. For a company deployment, add email verification or single sign-on before relying on them.
- **No rate limiting** on sign-in or join codes. Join codes are 12 random characters, which is hard to guess, but put the app behind a reverse proxy with rate limits if it's exposed to the internet.
- No password reset, and no way to delete an account.
- H2 is fine for a team or a trial. For heavier use, point `spring.datasource.*` at PostgreSQL and add a migration tool instead of `ddl-auto=update`.
