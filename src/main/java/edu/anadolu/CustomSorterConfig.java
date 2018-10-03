package edu.anadolu;

import edu.anadolu.sorter.*;

/**
 * Created by aliyurekli on 10/3/2018.
 */
public enum CustomSorterConfig {

    NoSort,
    TwoLevelSort,
    GeometricMeanSort,
    LinearWeightingSort;

    public CustomSorter getCustomSorter() {
        switch (this) {
            case NoSort:
                return new NoSort();

            case TwoLevelSort:
                return new TwoLevelSort();

            case GeometricMeanSort:
                return new GeometricMeanSort();

            case LinearWeightingSort:
                return new LinearWeightingSort();

            default:
                throw new AssertionError(this);
        }
    }
}
