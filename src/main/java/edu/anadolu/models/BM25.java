package edu.anadolu.models;

import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.SimilarityBase;

public class BM25 extends SimilarityBase implements Model {
    @Override
    public float score(BasicStats stats, float freq, float docLen) {
        return 0;
    }

    @Override
    public String toString() {
        return "BM25";
    }
}
