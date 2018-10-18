package edu.anadolu.models;

import org.apache.lucene.search.similarities.*;

public class LGD extends IBSimilarity implements Model {

    public LGD() {
        super(new DistributionLL(), new LambdaDF(), new NormalizationH2());
    }

    @Override
    public float score(BasicStats stats, float freq, float docLen) {
        return super.score(stats, freq, docLen);
    }

    @Override
    public String toString() {
        return "LGD";
    }
}
