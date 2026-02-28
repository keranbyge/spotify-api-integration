package com.spotify.api.controllers;

import java.io.IOException;

import org.apache.hc.core5.http.ParseException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spotify.api.dto.ApiResponse;
import com.spotify.api.service.SpotifyService;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.PlayHistory;
import se.michaelthelin.spotify.model_objects.specification.PagingCursorbased;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.model_objects.specification.User;

@RestController
public class SpotifyController {

    private final SpotifyService spotifyService;

    public SpotifyController(SpotifyService spotifyService) {
        this.spotifyService = spotifyService;
    }

    @GetMapping("/currently-playing")
    public ResponseEntity<ApiResponse<?>> getCurrentlyPlaying() throws ParseException {
        try {
            Object result = spotifyService.getCurrentlyPlaying();
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(ApiResponse.failure("Error fetching currently playing track: " + e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<?>> getMe() throws ParseException {
        try {
            User me = spotifyService.getCurrentUserProfile();
            return ResponseEntity.ok(ApiResponse.success(me));
        } catch (IOException | SpotifyWebApiException e) {
            return ResponseEntity.internalServerError().body(ApiResponse.failure("Error fetching profile: " + e.getMessage()));
        }
    }

    @GetMapping("/top-tracks")
    public ResponseEntity<ApiResponse<?>> getTopTracks() throws ParseException {
        try {
            Paging<Track> topTracks = spotifyService.getTopTracks(10);
            return ResponseEntity.ok(ApiResponse.success(topTracks));
        } catch (IOException | SpotifyWebApiException e) {
            return ResponseEntity.internalServerError().body(ApiResponse.failure("Error fetching top tracks: " + e.getMessage()));
        }
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<?>> getRecentlyPlayed() throws ParseException {
        try {
            PagingCursorbased<PlayHistory> recent = spotifyService.getRecentlyPlayed(10);
            return ResponseEntity.ok(ApiResponse.success(recent));
        } catch (IOException | SpotifyWebApiException e) {
            return ResponseEntity.internalServerError().body(ApiResponse.failure("Error fetching recent tracks: " + e.getMessage()));
        }
    }
}
