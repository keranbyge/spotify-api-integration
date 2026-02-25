package com.spotify.api.service;

import org.apache.hc.core5.http.ParseException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlaying;
import com.spotify.api.dto.NoPlaybackResponse;

import java.io.IOException;

@Service
public class SpotifyService {

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
                System.out.println("No refresh token available, skipping refresh.");
                return;
            }
            spotifyAuthService.refreshAccessToken();
        } catch (IOException | SpotifyWebApiException e) {
            System.err.println("Failed to refresh Spotify token: " + e.getMessage());
        }
    }

    public Object getCurrentlyPlaying() throws IOException, ParseException {
        try {
            var request = spotifyApi.getUsersCurrentlyPlayingTrack().build();
            CurrentlyPlaying currentlyPlaying = request.execute(); // null indicates 204 No Content
            System.out.println("Spotify getUsersCurrentlyPlayingTrack response: " + String.valueOf(currentlyPlaying));

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
                    spotifyAuthService.refreshAccessToken();
                    var retried = spotifyApi.getUsersCurrentlyPlayingTrack().build().execute();
                    System.out.println("Spotify retried getUsersCurrentlyPlayingTrack: " + String.valueOf(retried));
                    if (retried == null || retried.getItem() == null) {
                        return NoPlaybackResponse.ofNoPlayback();
                    }
                    return retried;
                } catch (Exception ex) {
                    System.err.println("Token refresh failed: " + ex.getMessage());
                }
            }
            System.err.println("Spotify API error: " + e.getMessage());
            return NoPlaybackResponse.ofNoPlayback();
        } catch (IOException e) {
            System.err.println("IO error while calling Spotify API: " + e.getMessage());
            return NoPlaybackResponse.ofNoPlayback();
        }
    }
}