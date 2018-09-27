package edu.anadolu;

class RecommendedTrack  {

    String trackURI;

    int searchResultFrequency;

    double maxScore;

    RecommendedTrack(String trackURI) {
        this.trackURI = trackURI;
    }

    int getSearchResultFrequency() {
        return searchResultFrequency;
    }

    double getMaxScore() {
        return maxScore;
    }
}
