package com.spotify.api.service;

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

import java.io.IOException;
import java.net.URI;

@Service
public class SpotifyAuthService {

    private static final Logger logger = LoggerFactory.getLogger(SpotifyAuthService.class);

    private final SpotifyApi spotifyApi;
    private String refreshToken; // in-memory only

    public SpotifyAuthService(
            @Value("${spotify.client.id}") String clientId,
            @Value("${spotify.client.secret}") String clientSecret,
            @Value("${spotify.redirect.uri}") String redirectUri) {

        if (clientId == null || clientId.isEmpty()
                || clientSecret == null || clientSecret.isEmpty()
                || redirectUri == null || redirectUri.isEmpty()) {
            throw new IllegalStateException("Spotify configuration is missing!");
        }

        this.spotifyApi = new SpotifyApi.Builder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRedirectUri(URI.create(redirectUri))
                .build();

        logger.info("SpotifyAuthService initialized successfully");
    }

    @PostConstruct
    public void init() {
        logger.info("SpotifyAuthService ready. Login required if no refresh token.");
    }

    public URI getAuthorizationUri() {
        AuthorizationCodeUriRequest request = spotifyApi.authorizationCodeUri()
                .scope("user-read-currently-playing,user-read-playback-state,user-read-recently-played,user-top-read")
                .show_dialog(true)
                .build();
        return request.execute();
    }

    public void exchangeCodeForTokens(String code)
            throws IOException, SpotifyWebApiException, ParseException {

        var credentials = spotifyApi.authorizationCode(code).build().execute();

        spotifyApi.setAccessToken(credentials.getAccessToken());

        if (credentials.getRefreshToken() != null) {
            this.refreshToken = credentials.getRefreshToken();
            spotifyApi.setRefreshToken(refreshToken);
            logger.info("Refresh token stored in memory.");
        }

        User currentUser = spotifyApi.getCurrentUsersProfile().build().execute();
        logger.info("Logged in as: {}", currentUser.getDisplayName());
    }

    public void refreshAccessToken()
            throws IOException, SpotifyWebApiException, ParseException {

        if (refreshToken == null) {
            throw new IllegalStateException("User must login first.");
        }

        var credentials = spotifyApi.authorizationCodeRefresh().build().execute();
        spotifyApi.setAccessToken(credentials.getAccessToken());
        logger.info("Access token refreshed successfully.");
    }

    public SpotifyApi getSpotifyApi() {
        return spotifyApi;
    }

    public boolean isLoggedIn() {
        return refreshToken != null;
    }
}