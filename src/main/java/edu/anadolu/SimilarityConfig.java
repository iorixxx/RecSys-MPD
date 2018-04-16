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
    LogTF,
    RawTF;

    public Similarity getSimilarity() {

        switch (this) {

            case BM25:
                return new BM25Similarity();

            case IB:
                return new IBSimilarity(new DistributionLL(), new LambdaDF(), new NormalizationH2());

            case DFI:
                return new DFISimilarity(new IndependenceChiSquared());

            case PL2:
                return new DFRSimilarity(new BasicModelP(), new AfterEffectL(), new NormalizationH2());

            case TFIDF:
                return new ClassicSimilarity();

            case LogTF:
                return new LogTF();

            case RawTF:
                return new RawTF();

            default:
                throw new AssertionError(this);
        }
    }
}
