package edu.anadolu.models;

import org.apache.lucene.search.similarities.BasicStats;

/**
 * MULTIPLE DOCUMENT WEIGHTING MODEL FEATURES
 * RQ1: whether the use of multiple weighting models as query dependent features leads to an increased effectiveness as measured by nDCG@k
 */
public interface Model {
    float score(BasicStats stats, float freq, float docLen);
}
