package com.spotify.api.service;

import com.spotify.api.dto.PlaybackStatusResponse;
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



import java.io.IOException;

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
    public void refreshAccessToken() {
        try {
            if (spotifyApi.getRefreshToken() == null) {
                logger.info("No refresh token available, skipping refresh.");
                return;
            }
            spotifyAuthService.refreshAccessToken();
            logger.info("Access token refreshed by scheduler");
        } catch (Exception e) {
            logger.error("Failed to refresh Spotify token: {}", e.getMessage());
        }
    }

    // ===============================
    // UPDATED METHOD
    // ===============================

   public PlaybackStatusResponse getCurrentlyPlaying() throws IOException, ParseException {

    try {
        CurrentlyPlaying currentlyPlaying =
                spotifyApi.getUsersCurrentlyPlayingTrack().build().execute();

        // If nothing returned
        if (currentlyPlaying == null || currentlyPlaying.getItem() == null) {
            return new PlaybackStatusResponse(
                    "OFFLINE",
                    "No active Spotify playback",
                    null
            );
        }

        // 🔥 IMPORTANT CHECK
        if (!currentlyPlaying.getIs_playing()) {
            return new PlaybackStatusResponse(
                    "OFFLINE",
                    "Spotify is paused",
                    null
            );
        }

        Track track = (Track) currentlyPlaying.getItem();

        String songDetails =
                track.getName() + " - " + track.getArtists()[0].getName();

        return new PlaybackStatusResponse(
                "PLAYING",
                "Now Playing",
                songDetails
        );

    } catch (Exception e) {
        return new PlaybackStatusResponse(
                "OFFLINE",
                "Error fetching playback",
                null
        );
    }
}

    // =========================================
    // Other methods unchanged
    // =========================================

    public User getCurrentUserProfile() throws Exception {
        return spotifyApi.getCurrentUsersProfile().build().execute();
    }

    public Paging<Track> getTopTracks(int limit) throws Exception {
        return spotifyApi.getUsersTopTracks().limit(limit).build().execute();
    }

    public PagingCursorbased<PlayHistory> getRecentlyPlayed(int limit) throws Exception {
        return spotifyApi.getCurrentUsersRecentlyPlayedTracks()
                .limit(limit)
                .build()
                .execute();
    }
}