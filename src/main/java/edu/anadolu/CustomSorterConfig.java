package edu.anadolu;

import edu.anadolu.sorter.CustomSorter;
import edu.anadolu.sorter.FreqSort;
import edu.anadolu.sorter.GeoSort;
import edu.anadolu.sorter.LuceneSort;

/**
 * topK-2-topN components.
 * All distinct tracks of topK playlist > n, thus recall changes.
 */
public enum CustomSorterConfig {

    LuceneSort,
    FreqSort,
    GeoSort;

    public CustomSorter getCustomSorter() {

        switch (this) {

            case LuceneSort:
                return new LuceneSort();

            case FreqSort:
                return new FreqSort();

            case GeoSort:
                return new GeoSort();

            default:
                throw new AssertionError(this);
        }
    }
}
