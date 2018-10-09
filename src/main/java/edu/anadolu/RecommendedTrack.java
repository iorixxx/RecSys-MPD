package edu.anadolu;

import java.util.Objects;

public class RecommendedTrack {

    String trackURI;

    int searchResultFrequency;

    double maxScore;

    int pos;

    RecommendedTrack(String trackURI) {
        this.trackURI = trackURI;
        this.searchResultFrequency = 0;
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
