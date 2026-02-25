package com.spotify.api.dto;

public class NoPlaybackResponse {
    private String status;
    private String message;

    public NoPlaybackResponse() {}

    public NoPlaybackResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public static NoPlaybackResponse ofNoPlayback() {
        return new NoPlaybackResponse("NO_PLAYBACK", "No active Spotify playback detected");
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
