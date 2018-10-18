package edu.anadolu.models;

import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;

public class DLM extends LMDirichletSimilarity implements Model {

    public DLM() {
        super(new DefaultCollectionModel(), 2000);
    }

    @Override
    public float score(BasicStats stats, float freq, float docLen) {
        return super.score(stats, freq, docLen);
    }

    @Override
    public String toString() {
        return "DLM";
    }
}
