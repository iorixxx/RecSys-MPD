package edu.anadolu;

public class RecommendedTrack  {

    String trackURI;

    int searchResultFrequency;

    double maxScore;

    RecommendedTrack(String trackURI) {
        this.trackURI = trackURI;
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
}
