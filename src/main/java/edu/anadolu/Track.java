package edu.anadolu;

public class Track {

    int pos;
    String artist_name;
    String track_uri;
    String artist_uri;
    String album_uri;
    int duration_ms;

    String track_uri() {
        return track_uri;
    }

    String artist_uri() {
        return artist_uri;
    }

    String album_uri() {
        return album_uri;
    }
}
