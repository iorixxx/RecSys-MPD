package edu.anadolu.models;

import org.apache.lucene.search.similarities.*;

public class PL2 extends DFRSimilarity implements Model {

    public PL2() {
        super(new BasicModelP(), new AfterEffectL(), new NormalizationH2());
    }

    @Override
    public float score(BasicStats stats, float freq, float docLen) {
        return super.score(stats, freq, docLen);
    }

    @Override
    public String toString() {
        return "PL2";
    }
}
