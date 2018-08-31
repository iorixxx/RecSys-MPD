package edu.anadolu;

public class Playlist {

    String name;

    int pid;

    Track[] tracks;

    int num_tracks;

    int num_followers;

    public boolean isSequential() {
        int currentPos = 0;

        for (Track track : tracks) {
            if (currentPos != track.pos) return false;

            currentPos ++;
        }

        return true;
    }
}
