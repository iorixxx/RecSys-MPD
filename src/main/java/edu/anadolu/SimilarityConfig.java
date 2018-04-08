package edu.anadolu;

import org.apache.lucene.search.similarities.*;

/**
 * Similarity config can be BM25, IB or DFI.
 */
public enum SimilarityConfig {

    BM25,
    IB,
    DFI;

    public Similarity getSimilarity() {
        Similarity s = null;

        switch (this) {
            case BM25:
                s = new BM25Similarity();
                break;
            case IB:
                s = new IBSimilarity(new DistributionLL(), new LambdaDF(), new NormalizationH2());
                break;
            case DFI:
                s = new DFISimilarity(new IndependenceChiSquared());
                break;
        }

        return s;
    }
}
