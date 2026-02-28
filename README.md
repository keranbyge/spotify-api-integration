# Spotify API Spring Boot Backend

A professional, portfolio-ready Spring Boot backend integrating with Spotify's Web API. It implements OAuth 2.0 Authorization Code flow, standard API response wrapping, global exception handling, structured logging, and user-centric endpoints.

## Overview

This backend provides endpoints to authenticate with Spotify, retrieve the current playback, user profile, top tracks, and recently played tracks. It includes robust token handling with automatic refresh, and standardized JSON response envelopes for consistency.

## Architecture

- config: Web configuration (CORS, etc.)
- controllers: REST endpoints
- service: Business logic and Spotify SDK integration
- dto: Data transfer objects and response wrappers
- exception: Global exception handling

## OAuth Flow

1. Client hits /login to obtain an authorization URL.
2. Spotify redirects back to /callback with an authorization code.
3. Backend exchanges the code for access and refresh tokens.
4. Refresh token is stored locally (file-based for demo) and used to refresh access tokens automatically.

Scopes used: user-read-currently-playing, user-read-playback-state, user-read-recently-played, user-top-read

## Endpoints

- GET /login: Initiates Spotify login. Redirects to Spotify if no refresh token is present.
- GET /callback?code=...: Handles OAuth callback; exchanges code for tokens.
- GET /currently-playing: Returns current playback or a NoPlaybackResponse if nothing is playing.
- GET /me: Returns current Spotify user profile.
- GET /top-tracks: Returns top 10 tracks for the current user.
- GET /recent: Returns 10 most recently played tracks for the current user.

All endpoints return: { success, data, message, timestamp }

## Tech Stack

- Java 17+
- Spring Boot
- Spotify Web API Java SDK (se.michaelthelin.spotify)
- SLF4J + Logback
- Maven

## How to Run

1. Configure application.properties with:
   - spotify.client.id
   - spotify.client.secret
   - spotify.redirect.uri (e.g., http://localhost:8080/callback)
2. Build:
   - mvn clean install
3. Run:
   - mvn spring-boot:run
4. Open the app and start the OAuth flow via /login.

## Token Handling

- Access token is refreshed automatically when expired or invalid responses are detected.
- Scheduled refresh runs every ~44 minutes if a refresh token exists.
- Logs are emitted for token generation and refresh events.

## Future Improvements

- Persist refresh tokens securely (DB or secret manager).
- Add pagination and filters for top tracks and recently played.
- Introduce integration tests and contract tests.
- Add rate limiting and resilience patterns (retry/backoff, circuit breaker).

## Notes

- Do not commit real credentials.
- The file-based token storage is for demo/development only.
