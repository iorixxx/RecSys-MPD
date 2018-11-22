package edu.anadolu;

import edu.anadolu.models.DLM;
import edu.anadolu.models.DPH;
import org.apache.lucene.search.similarities.*;

/**
 * Similarity config can be BM25, IB or DFI.
 */
public enum SimilarityConfig {

    BM25,
    DPH,
    IB,
    DFI,
    PL2,
    DLM;

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

            case DLM:
                return new DLM();

            case DPH:
                return new DPH();

            default:
                throw new AssertionError(this);
        }
    }
}
