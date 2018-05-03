package edu.anadolu;

import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.SimilarityBase;

/**
 * Frequentist Approach
 */
public class RawTF extends SimilarityBase {

    @Override
    protected double score(BasicStats stats, double freq, double docLen) {
        return freq;
    }

    @Override
    public String toString() {
        return "RawTF";
    }
}
