package edu.anadolu;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RecommendedTrack {

    String trackURI;

    int searchResultFrequency;

    int searchResultAlbumFrequency;

    int searchResultArtistFrequency;

    double maxScore;

    int pos;

    int playlistId;

    int luceneId;

    long playlist_length;

    String album;
    String artist;
    String track;
    List<Integer> pIdList;

    RecommendedTrack(String trackURI) {
        this.trackURI = trackURI;
        this.searchResultFrequency = 0;
        this.maxScore = 0;
        this.pos = 0;
        this.pIdList = new ArrayList<>();
    }

    public int getLuceneId() {
        return luceneId;
    }

    public int getSearchResultFrequency() {
        return searchResultFrequency;
    }

    public double getMaxScore() {
        return maxScore;
    }

    public double getGeometricMean() {
        return Math.sqrt(searchResultFrequency * maxScore);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecommendedTrack that = (RecommendedTrack) o;
        return Objects.equals(trackURI, that.trackURI);
    }

    @Override
    public String toString() {
        return "RecommendedTrack{" +
                "trackURI='" + trackURI + '\'' +
                ", searchResultFrequency=" + searchResultFrequency +
                ", maxScore=" + maxScore +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(trackURI);
    }
}
