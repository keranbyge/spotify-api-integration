package com.spotify.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

import org.apache.hc.core5.http.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.specification.User;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class SpotifyAuthService {

    private static final Logger logger = LoggerFactory.getLogger(SpotifyAuthService.class);

    private final SpotifyApi spotifyApi;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String tokenFilePath = "spotify_tokens.json";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SpotifyAuthService(
            @Value("${spotify.client.id}") String clientId,
            @Value("${spotify.client.secret}") String clientSecret,
            @Value("${spotify.redirect.uri}") String redirectUri) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;

        this.spotifyApi = new SpotifyApi.Builder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRedirectUri(URI.create(redirectUri))
                .build();
    }

    @PostConstruct
    public void init() {
        validateConfiguration();
        loadStoredTokens();
        logger.info("[OAuth] SpotifyAuthService initialized. Client ID length: {}", clientId != null ? clientId.length() : 0);
    }

    private void validateConfiguration() {
        if (clientId == null || clientId.isEmpty() ||
            clientSecret == null || clientSecret.isEmpty() ||
            redirectUri == null || redirectUri.isEmpty()) {
            throw new IllegalStateException("Spotify configuration properties are missing or invalid");
        }
    }

    public boolean hasValidRefreshToken() {
        return spotifyApi.getRefreshToken() != null && !spotifyApi.getRefreshToken().isEmpty();
    }

    public URI getAuthorizationUri() {
        AuthorizationCodeUriRequest authorizationCodeUriRequest = spotifyApi.authorizationCodeUri()
                .scope("user-read-currently-playing,user-read-playback-state,user-read-recently-played,user-top-read")
                .show_dialog(true)
                .build();
        return authorizationCodeUriRequest.execute();
    }

    public void exchangeCodeForTokens(String code) throws IOException, SpotifyWebApiException, ParseException {
        var credentials = spotifyApi.authorizationCode(code).build().execute();
        spotifyApi.setAccessToken(credentials.getAccessToken());
        logger.info("[OAuth] Access token set after authorization code exchange");
        if (credentials.getRefreshToken() != null) {
            spotifyApi.setRefreshToken(credentials.getRefreshToken());
            logger.info("[OAuth] Refresh token obtained and set");
            saveTokensToFile(credentials.getRefreshToken());
        }

        User currentUser = spotifyApi.getCurrentUsersProfile().build().execute();
        logger.info("[OAuth] Logged in Spotify user: {}", currentUser.getDisplayName());
    }

    public void refreshAccessToken() throws IOException, SpotifyWebApiException, ParseException {
        if (spotifyApi.getRefreshToken() == null || spotifyApi.getRefreshToken().isEmpty()) {
            logger.warn("[OAuth] No refresh token found. User must login.");
            throw new IllegalStateException("No refresh token available to refresh access token");
        }
        
        logger.info("[OAuth] Attempting token refresh");
        try {
            var credentials = spotifyApi.authorizationCodeRefresh().build().execute();
            spotifyApi.setAccessToken(credentials.getAccessToken());
            logger.info("[OAuth] Refresh successful");
        } catch (SpotifyWebApiException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("invalid") || msg.contains("client") || msg.contains("400")) {
                logger.error("[OAuth] Refresh failed - invalid client. Clearing stored tokens.");
                clearStoredTokens();
                spotifyApi.setRefreshToken(null);
                spotifyApi.setAccessToken(null);
            }
            throw e;
        }
    }

    private void saveTokensToFile(String refreshToken) throws IOException {
        TokenData tokenData = new TokenData(refreshToken);
        objectMapper.writeValue(new File(tokenFilePath), tokenData);
        logger.info("Refresh token saved to {}", tokenFilePath);
    }

    private void loadStoredTokens() {
        try {
            if (Files.exists(Paths.get(tokenFilePath))) {
                TokenData tokenData = objectMapper.readValue(new File(tokenFilePath), TokenData.class);
                if (tokenData.getRefreshToken() != null && !tokenData.getRefreshToken().isEmpty()) {
                    spotifyApi.setRefreshToken(tokenData.getRefreshToken());
                    logger.info("[OAuth] Loaded refresh token from {}", tokenFilePath);
                } else {
                    logger.warn("[OAuth] Token file exists but contains invalid data. Deleting.");
                    clearStoredTokens();
                }
            }
        } catch (Exception e) {
            logger.error("[OAuth] Failed to load refresh token: {}. Deleting invalid token file.", e.getMessage());
            clearStoredTokens();
        }
    }

    private void clearStoredTokens() {
        try {
            Files.deleteIfExists(Paths.get(tokenFilePath));
            logger.info("[OAuth] Cleared stored tokens");
        } catch (IOException e) {
            logger.error("[OAuth] Failed to delete token file: {}", e.getMessage());
        }
    }

    public SpotifyApi getSpotifyApi() {
        return spotifyApi;
    }

    private static class TokenData {
        private String refreshToken;

        public TokenData() {}

        public TokenData(String refreshToken) {
            this.refreshToken = refreshToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }
}