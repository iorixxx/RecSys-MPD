package edu.anadolu;

public class RecommendedTrack  {

    String trackURI;

    int searchResultFrequency;

    double maxScore;

    double linearWeighting;

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

    public double getLinearWeighting() {
        return linearWeighting;
    }

    /**
     *
     * @param alpha coefficient
     * @param minSRF minimum of search result frequency
     * @param maxSFR maximum of search result frequency
     * @param minMS minimum of max Lucene score
     * @param maxMS maximum of max Lucene score
     */
    public void computeLinearWeighting(double alpha, int minSRF, int maxSFR, double minMS, double maxMS) {
        linearWeighting = alpha * ((double) (searchResultFrequency - minSRF) / (maxSFR - minSRF)) + (1 - alpha) * ((maxScore - minMS) / (maxMS - minMS));
    }
}
