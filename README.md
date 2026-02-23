# SantaseService

Backend service for [deck.bg](https://deck.bg) — a real-time online multiplayer platform for **Santase** (Sixty-Six), the popular Bulgarian two-player trick-taking card game.

Built with **Spring Boot 4**, **Java 25**, **WebSockets (STOMP)**, and **PostgreSQL**.

---

## Table of Contents

- [Overview](#overview)
- [Game Rules Summary](#game-rules-summary)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [API Endpoints](#api-endpoints)
- [WebSocket Communication](#websocket-communication)
- [Ranking System](#ranking-system)
- [Security](#security)
- [Infrastructure & Deployment](#infrastructure--deployment)
- [Environment Variables](#environment-variables)
- [Getting Started](#getting-started)

---

## Overview

SantaseService is the backend for a full-stack card game platform where users can register, find opponents via matchmaking, and play Santase in real time. The server manages the complete game lifecycle — from matchmaking and dealing cards, through trick evaluation and scoring, to Elo-based ranking updates.

---

## Game Rules Summary

Santase is played with a 24-card deck (9, J, Q, K, 10, A in four suits). Each deal starts with 6 cards per player and a trump card. Players take turns playing cards into tricks. Key mechanics include:

- **Trick-taking** — higher card of the led suit wins; trump suit beats non-trump.
- **Drawing** — after each trick, both players draw from the deck (winner first) while cards remain.
- **Announcements (20/40)** — playing a King or Queen while holding its matching partner scores 20 points (or 40 if in the trump suit).
- **Trump card replacement** — the player holding the Nine of trumps can swap it for the trump card.
- **Closing the deck** — a player can close the deck to prevent further drawing, committing to reach 66 points.
- **Scoring** — the first player to reach 66 points wins the deal. Deal points (1–3) are awarded based on the loser's score. The match is won by the first player to reach 11 deal points with a lead of at least 2.

---

## Features

### Authentication & User Management
- **Registration** with email confirmation
- **Login** with JWT access + refresh token flow
- **Token refresh** for seamless session continuation
- **Forgot password** flow with email-based reset link and token verification
- **Change password** (authenticated, requires current password)
- **Account deletion** with email confirmation link
- **Profile retrieval** (username, email, rank, rating, win/loss stats)

### Real-Time Multiplayer Game
- **Matchmaking queue** — concurrent lock-free queue (`ConcurrentLinkedQueue`) pairs players automatically
- **Play card** — full rule enforcement including forced play when the deck is empty
- **Announce combination** — 20/40 point bonuses for King+Queen pairs
- **Close deck** — strategic option to stop drawing
- **Replace trump card** — swap Nine of trumps for the face-up trump
- **Finish deal** — end-of-deal scoring with 1/2/3 point awards
- **Surrender** — forfeit the current game
- **Inactivity handling** — automatic surrender after 3 consecutive inactivity timeouts (33-second turn timer)
- **Extend time** — request additional time for the current move
- **Auto-cancel search on disconnect** — WebSocket disconnect removes the player from the matchmaking queue

### Ranking System
- **Elo-based rating** starting at 1500
- **Placement phase** — first 10 games use a higher K-factor (40 vs 24)
- **Rank tiers**: Unranked → Bronze → Silver → Gold → Platinum → Diamond → Legend

### Infrastructure
- **WebSocket (STOMP over SockJS)** for pushing game state updates to both players
- **Rate-limited message delivery** — per-user 50ms minimum delay via virtual threads to prevent flooding
- **Scheduled inactivity enforcement** — server-side timer auto-surrenders inactive players
- **Liquibase** database migrations
- **CI/CD** via GitHub Actions → Docker Hub → AWS EC2
- **Nginx reverse proxy** with Cloudflare-only access, TLS termination, and per-endpoint rate limiting

---

## Tech Stack

| Layer            | Technology                                      |
|------------------|--------------------------------------------------|
| Language         | Java 25                                          |
| Framework        | Spring Boot 4.0.2                                |
| Real-time        | Spring WebSocket (STOMP + SockJS)                |
| Security         | Spring Security + JWT (jjwt 0.12.7)              |
| Database         | PostgreSQL                                       |
| ORM              | Spring Data JPA / Hibernate                      |
| Migrations       | Liquibase                                        |
| Email            | Spring Mail (SMTP)                               |
| Mapping          | MapStruct 1.6.3                                  |
| Logging          | Log4j2                                           |
| Build            | Maven                                            |
| Containerization | Docker (multi-stage build)                       |
| Proxy            | Nginx (with Cloudflare integration)              |
| CI/CD            | GitHub Actions → Docker Hub → EC2                |
| Virtual Threads  | Enabled (`spring.threads.virtual.enabled: true`)  |

---

## Architecture

```
src/main/java/bg/deck/santaseservice/
├── config/             # App config, WebSocket, email properties, async executor
├── constant/           # Application-wide constants (game, ranking, validation, logging)
├── controller/         # REST controllers (Auth, Game, User)
├── enums/              # Enums for ranks, roles, card suits/ranks, statuses
├── exception/          # Custom exceptions + global exception handler
├── model/              # JPA entities (User, Player, Game, GameState, Card, etc.)
│   ├── base/           # BaseEntity, BaseUser mapped superclasses
│   ├── dto/            # Data transfer objects
│   ├── request/        # Incoming request models
│   └── response/       # Outgoing response models
├── repository/         # Spring Data JPA repositories
├── security/           # JWT filter, JWT properties, SecurityConfig
├── service/            # Business logic (Auth, Game, User, Ranking, WebSocket, Email)
└── util/               # MapStruct mappers (Card, User, Enum)
```

---

## API Endpoints

### Auth (`/auth`) — public

| Method | Path                          | Description                          |
|--------|-------------------------------|--------------------------------------|
| POST   | `/auth/login`                 | Authenticate and receive JWT tokens  |
| POST   | `/auth/register`              | Register a new account               |
| POST   | `/auth/refresh`               | Refresh access token                 |
| GET    | `/auth/confirm-email`         | Confirm email via token (redirect)   |
| POST   | `/auth/forgot-password`       | Request password reset email         |
| GET    | `/auth/forgot-password/verify`| Verify forgot-password token         |
| POST   | `/auth/change-password`       | Set new password via reset token     |

### Game (`/game`) — requires `ROLE_USER`

| Method | Path                 | Description                              |
|--------|----------------------|------------------------------------------|
| POST   | `/game/search`       | Join matchmaking queue                   |
| GET    | `/game/state`        | Request current game state via WebSocket |
| POST   | `/game/play-card`    | Play a card from hand                    |
| POST   | `/game/announce`     | Announce a 20/40 combination             |
| POST   | `/game/close-deck`   | Close the deck                           |
| POST   | `/game/replace-card` | Replace trump card with Nine of trumps   |
| POST   | `/game/finish-deal`  | Finish the current deal and score        |
| POST   | `/game/surrender`    | Surrender the game                       |
| POST   | `/game/inactivity`   | Report own inactivity timeout            |
| POST   | `/game/extend-time`  | Extend the turn timer                    |

### User (`/user`) — requires `ROLE_USER` (except confirm-deletion)

| Method | Path                    | Description                              |
|--------|-------------------------|------------------------------------------|
| GET    | `/user/profile`         | Get user profile (rank, rating, stats)   |
| POST   | `/user/confirm-email`   | Resend email confirmation                |
| POST   | `/user/change-password` | Change password (authenticated)          |
| POST   | `/user/delete-user`     | Request account deletion email           |
| GET    | `/user/confirm-deletion`| Confirm account deletion via token       |

---

## WebSocket Communication

- **Endpoint**: `/ws-game` (STOMP over SockJS)
- **Subscribe topics**:
  - `/topic/game/{gameId}/{username}` — receive game state updates
  - `/topic/game/{username}` — receive matchmaking status updates
- **Allowed origins**: `https://deck.bg` (prod) / `http://localhost:3000` (dev)
- **Disconnect handling**: automatically removes the player from the matchmaking queue

---

## Ranking System

The Elo rating system determines player rankings:

| Rank      | Rating Threshold |
|-----------|-----------------|
| Unranked  | < 10 games      |
| Bronze    | < 1400          |
| Silver    | 1400 – 1549     |
| Gold      | 1550 – 1699     |
| Platinum  | 1700 – 1899     |
| Diamond   | 1900 – 2099     |
| Legend    | 2100+           |

- **Initial rating**: 1500
- **Placement K-factor**: 40 (first 10 games)
- **Ranked K-factor**: 24

---

## Security

- **Stateless JWT** authentication (no server-side sessions)
- **Role-based access control** — `ROLE_USER` for game and user endpoints; auth endpoints are public
- **BCrypt** password hashing
- **Cloudflare-only access** — Nginx blocks all non-Cloudflare IPs
- **Rate limiting** (Nginx):
  - Auth: 10 req/min (burst 5)
  - Game: 30 req/min (burst 10)
  - User: 10 req/min (burst 5)
- **TLS** via Cloudflare origin certificates
- **Real IP extraction** from `CF-Connecting-IP` header for accurate rate limiting and logging

---

## Infrastructure & Deployment

### Docker

Multi-stage Dockerfile:
1. **Build stage** — `eclipse-temurin:25-jdk-noble`, compiles the JAR with Maven
2. **Runtime stage** — `eclipse-temurin:25-jre-noble`, runs as non-root `springuser`

### Docker Compose

Three services on a shared bridge network:
- **nginx** — reverse proxy (ports 80/443), Cloudflare TLS, rate limiting
- **backend** — Spring Boot application (env from `.env` file)

### CI/CD (GitHub Actions)

On push to `master`:
1. Build JAR with Maven (JDK 25)
2. Build and push Docker image to Docker Hub
3. SCP `docker-compose.yml` and `nginx.conf` to EC2
4. SSH into EC2 → pull latest images → recreate containers → prune old images

---

## Environment Variables

| Variable                | Description                          |
|-------------------------|--------------------------------------|
| `DB_URL_SANTASE`        | PostgreSQL JDBC connection URL       |
| `DB_USERNAME`           | Database username                    |
| `DB_PASSWORD`           | Database password                    |
| `DB_DIALECT`            | Hibernate dialect                    |
| `JWT_SECRET`            | Secret key for signing JWTs          |
| `JWT_EXPIRATION`        | Access token expiration (ms)         |
| `JWT_REFRESH_EXPIRATION`| Refresh token expiration (ms)        |
| `SANTASE_MAIL_HOST`     | SMTP server host                     |
| `SANTASE_MAIL_PORT`     | SMTP server port                     |
| `SANTASE_MAIL_USERNAME` | SMTP username                        |
| `SANTASE_MAIL_PASSWORD` | SMTP password                        |
| `DOCKERHUB_USERNAME`    | Docker Hub username (for deployment) |

---

## Getting Started

### Prerequisites

- **Java 25**
- **Maven 3.9+**
- **PostgreSQL**
- **Docker** (optional, for containerized deployment)

### Local Development

1. **Clone the repository**
   ```bash
   git clone https://github.com/<your-username>/SantaseService.git
   cd SantaseService
   ```

2. **Set environment variables** (or create a `.env` file)

3. **Run the application**
   ```bash
   ./mvnw spring-boot:run
   ```
   The server starts on `http://localhost:8080` with the `dev` profile active.

### Docker

```bash
docker build -t santase-backend .
docker run -p 8080:8080 --env-file .env santase-backend
```
