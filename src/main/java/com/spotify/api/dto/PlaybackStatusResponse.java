package com.spotify.api.dto;

public class PlaybackStatusResponse {

    private String status;
    private String message;
    private String song;

    public PlaybackStatusResponse(String status, String message, String song) {
        this.status = status;
        this.message = message;
        this.song = song;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getSong() {
        return song;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setSong(String song) {
        this.song = song;
    }
}