package edu.anadolu;

import com.google.gson.Gson;

public class MPD {

    public static final String PLAYLIST_ID = "pid";
    public static final String PLAYLIST_TITLE = "name";
    public static final String PLAYLIST_TRACK_URIS = "track_uris";

    public static final Gson GSON = new Gson();

    Playlist[] playlists;
}
