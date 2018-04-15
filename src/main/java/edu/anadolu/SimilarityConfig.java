package edu.anadolu;

import org.apache.lucene.search.similarities.*;

/**
 * Similarity config can be BM25, IB or DFI.
 */
public enum SimilarityConfig {

    BM25,
    IB,
    DFI,
    PL2,
    TFIDF,
    RawTF;


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
            case PL2:
                s = new DFRSimilarity(new BasicModelP(), new AfterEffectL(), new NormalizationH2());
                break;
            case TFIDF:
                s = new ClassicSimilarity();
                break;
            case RawTF:
                s = new RawTF();
                break;
            default:
                throw new AssertionError(this);
        }

        return s;
    }
}
