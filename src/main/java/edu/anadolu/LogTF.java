package edu.anadolu;

import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.SimilarityBase;

/**
 * Yet Another Frequentist Approach
 */
public class LogTF extends SimilarityBase {

    protected double score(BasicStats stats, double freq, double docLen) {
        return log2(freq + 1);
    }

    protected float score(BasicStats stats, float freq, float docLen) {
        return (float) log2(freq + 1);
    }

    @Override
    public String toString() {
        return "LogTF";
    }
}
