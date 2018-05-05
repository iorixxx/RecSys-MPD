package edu.anadolu;

import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.SimilarityBase;

/**
 * This class implements the DPH hypergeometric weighting model. P
 * stands for Popper's normalization. This is a parameter-free
 * weighting model. Even if the user specifies a parameter value, it will <b>NOT</b>
 * affect the results. It is highly recommended to use the model with query expansion.
 * <p><b>References</b>
 * <ol>
 * <li>FUB, IASI-CNR and University of Tor Vergata at TREC 2007 Blog Track. G. Amati
 * and E. Ambrosi and M. Bianchi and C. Gaibisso and G. Gambosi. Proceedings of
 * the 16th Text REtrieval Conference (TREC-2007), 2008.</li>
 * <li>Frequentist and Bayesian approach to  Information Retrieval. G. Amati. In
 * Proceedings of the 28th European Conference on IR Research (ECIR 2006).
 * LNCS vol 3936, pages 13--24.</li>
 * </ol>
 */
public class DPH extends SimilarityBase {

    protected double score(BasicStats stats, double freq, double docLen) {
        double f = freq / docLen;
        double norm = (1d - f) * (1d - f) / (freq + 1d);

        // averageDocumentLength => stats.getAvgFieldLength()
        // numberOfDocuments => stats.getNumberOfDocuments()
        // termFrequency => stats.getTotalTermFreq()

        return norm
                * (freq * log2((freq *
                stats.getAvgFieldLength() / docLen) *
                stats.getNumberOfDocuments() / stats.getTotalTermFreq())
                + 0.5d * log2(2d * Math.PI * freq * (1d - f))
        );
    }

    protected float score(BasicStats stats, float freq, float docLen) {
        double f = freq / docLen;
        double norm = (1d - f) * (1d - f) / (freq + 1d);

        // averageDocumentLength => stats.getAvgFieldLength()
        // numberOfDocuments => stats.getNumberOfDocuments()
        // termFrequency => stats.getTotalTermFreq()

        return (float) (norm
                * (freq * log2((freq *
                stats.getAvgFieldLength() / docLen) *
                stats.getNumberOfDocuments() / stats.getTotalTermFreq())
                + 0.5d * log2(2d * Math.PI * freq * (1d - f))
        ));
    }

    @Override
    public String toString() {
        return "DPH";
    }
}
