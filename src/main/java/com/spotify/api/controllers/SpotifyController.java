package com.spotify.api.controllers;

import java.io.IOException;

import org.apache.hc.core5.http.ParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spotify.api.service.SpotifyService;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlayingContext;

@RestController
public class SpotifyController {

    private final SpotifyService spotifyService;

    public SpotifyController(SpotifyService spotifyService) {
        this.spotifyService = spotifyService;
    }

    @GetMapping("/currently-playing")
    public ResponseEntity<?> getCurrentlyPlaying() throws ParseException {
        try {
            Object result = spotifyService.getCurrentlyPlaying();
            // When no playback, our service returns NoPlaybackResponse
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching currently playing track: " + e.getMessage());
        }
    }
}