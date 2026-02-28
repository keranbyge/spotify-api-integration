package com.spotify.api.service;

import org.apache.hc.core5.http.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlaying;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.PlayHistory;
import se.michaelthelin.spotify.model_objects.specification.PagingCursorbased;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.model_objects.specification.User;
import com.spotify.api.dto.NoPlaybackResponse;

import java.io.IOException;
import java.time.Instant;

@Service
public class SpotifyService {

    private static final Logger logger = LoggerFactory.getLogger(SpotifyService.class);

    private final SpotifyApi spotifyApi;
    private final SpotifyAuthService spotifyAuthService;

    public SpotifyService(SpotifyAuthService spotifyAuthService) {
        this.spotifyAuthService = spotifyAuthService;
        this.spotifyApi = spotifyAuthService.getSpotifyApi();
    }

    @Scheduled(fixedDelay = 44 * 60 * 1000)
    public void refreshAccessToken() throws ParseException {
        try {
            if (spotifyApi.getRefreshToken() == null) {
                logger.info("No refresh token available, skipping refresh.");
                return;
            }
            spotifyAuthService.refreshAccessToken();
            logger.info("Access token refreshed by scheduler");
        } catch (IOException | SpotifyWebApiException e) {
            logger.error("Failed to refresh Spotify token: {}", e.getMessage());
        }
    }

    public Object getCurrentlyPlaying() throws IOException, ParseException {
        try {
            var request = spotifyApi.getUsersCurrentlyPlayingTrack().build();
            CurrentlyPlaying currentlyPlaying = request.execute(); // null indicates 204 No Content
            logger.debug("Spotify getUsersCurrentlyPlayingTrack response: {}", String.valueOf(currentlyPlaying));

            if (currentlyPlaying == null) {
                return NoPlaybackResponse.ofNoPlayback();
            }
            if (currentlyPlaying.getItem() == null) {
                return NoPlaybackResponse.ofNoPlayback();
            }
            return currentlyPlaying;
        } catch (SpotifyWebApiException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("invalid") || msg.contains("expired") || msg.contains("401")) {
                try {
                    logger.warn("Access token may be invalid/expired. Attempting refresh...");
                    spotifyAuthService.refreshAccessToken();
                    var retried = spotifyApi.getUsersCurrentlyPlayingTrack().build().execute();
                    logger.debug("Spotify retried getUsersCurrentlyPlayingTrack: {}", String.valueOf(retried));
                    if (retried == null || retried.getItem() == null) {
                        return NoPlaybackResponse.ofNoPlayback();
                    }
                    return retried;
                } catch (Exception ex) {
                    logger.error("Token refresh failed: {}", ex.getMessage());
                }
            }
            logger.error("Spotify API error: {}", e.getMessage());
            return NoPlaybackResponse.ofNoPlayback();
        } catch (IOException e) {
            logger.error("IO error while calling Spotify API: {}", e.getMessage());
            return NoPlaybackResponse.ofNoPlayback();
        }
    }

    public User getCurrentUserProfile() throws IOException, ParseException, SpotifyWebApiException {
        try {
            return spotifyApi.getCurrentUsersProfile().build().execute();
        } catch (SpotifyWebApiException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("invalid") || msg.contains("expired") || msg.contains("401")) {
                logger.warn("Access token invalid/expired while fetching profile. Refreshing...");
                spotifyAuthService.refreshAccessToken();
                return spotifyApi.getCurrentUsersProfile().build().execute();
            }
            throw e;
        }
    }

    public Paging<Track> getTopTracks(int limit) throws IOException, ParseException, SpotifyWebApiException {
        try {
            return spotifyApi.getUsersTopTracks().limit(limit).build().execute();
        } catch (SpotifyWebApiException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("invalid") || msg.contains("expired") || msg.contains("401")) {
                logger.warn("Access token invalid/expired while fetching top tracks. Refreshing...");
                spotifyAuthService.refreshAccessToken();
                return spotifyApi.getUsersTopTracks().limit(limit).build().execute();
            }
            throw e;
        }
    }

    public PagingCursorbased<PlayHistory> getRecentlyPlayed(int limit) throws IOException, ParseException, SpotifyWebApiException {
        try {
            return spotifyApi.getCurrentUsersRecentlyPlayedTracks().limit(limit).build().execute();
        } catch (SpotifyWebApiException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("invalid") || msg.contains("expired") || msg.contains("401")) {
                logger.warn("Access token invalid/expired while fetching recently played. Refreshing...");
                spotifyAuthService.refreshAccessToken();
                return spotifyApi.getCurrentUsersRecentlyPlayedTracks().limit(limit).build().execute();
            }
            throw e;
        }
    }
}