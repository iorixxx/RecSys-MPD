package edu.anadolu.models;

import org.apache.lucene.search.similarities.BasicStats;

public interface Model {
    float score(BasicStats stats, float freq, float docLen);
}
