package com.spotify.api.controllers;

import com.spotify.api.service.SpotifyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.PlayHistory;
import se.michaelthelin.spotify.model_objects.specification.PagingCursorbased;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.model_objects.specification.User;

@RestController
@RequestMapping("/api")
public class SpotifyController {

    private final SpotifyService spotifyService;

    public SpotifyController(SpotifyService spotifyService) {
        this.spotifyService = spotifyService;
    }

    // ===============================
    // CURRENTLY PLAYING
    // ===============================

    @GetMapping("/currently-playing")
    public ResponseEntity<?> getCurrentlyPlaying() {
        try {
            return ResponseEntity.ok(spotifyService.getCurrentlyPlaying());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error fetching currently playing track: " + e.getMessage());
        }
    }

    // ===============================
    // USER PROFILE
    // ===============================

    @GetMapping("/me")
    public ResponseEntity<?> getMe() {
        try {
            User user = spotifyService.getCurrentUserProfile();
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error fetching profile: " + e.getMessage());
        }
    }

    // ===============================
    // TOP TRACKS
    // ===============================

    @GetMapping("/top-tracks")
    public ResponseEntity<?> getTopTracks() {
        try {
            Paging<Track> topTracks = spotifyService.getTopTracks(10);
            return ResponseEntity.ok(topTracks);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error fetching top tracks: " + e.getMessage());
        }
    }

    // ===============================
    // RECENTLY PLAYED
    // ===============================

    @GetMapping("/recent")
    public ResponseEntity<?> getRecentlyPlayed() {
        try {
            PagingCursorbased<PlayHistory> recent =
                    spotifyService.getRecentlyPlayed(10);
            return ResponseEntity.ok(recent);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error fetching recent tracks: " + e.getMessage());
        }
    }
}