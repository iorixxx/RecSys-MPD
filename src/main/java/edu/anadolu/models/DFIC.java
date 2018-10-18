package edu.anadolu.models;

import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.DFISimilarity;
import org.apache.lucene.search.similarities.IndependenceChiSquared;

/**
 * Create DFI with the IndependenceChiSquared divergence from independence measure
 */
public class DFIC extends DFISimilarity implements Model {

    public DFIC() {
        super(new IndependenceChiSquared());
    }

    @Override
    public float score(BasicStats stats, float freq, float docLen) {
        return super.score(stats, freq, docLen);
    }

    @Override
    public String toString() {
        return "DFIC";
    }
}
