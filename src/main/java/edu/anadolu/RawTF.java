package edu.anadolu;

import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.SimilarityBase;

/**
 * Frequentist Approach
 */
public class RawTF extends SimilarityBase {

    @Override
    protected float score(BasicStats stats, float freq, float docLen) {
        return freq;
    }

    @Override
    public String toString() {
        return "RawTF";
    }
}
