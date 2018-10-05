package edu.anadolu;

import edu.anadolu.sorter.*;

/**
 * Created by aliyurekli on 10/3/2018.
 */
public enum CustomSorterConfig {

    NoSort,
    TwoLevelSort,
    GeometricMeanSort;

    public CustomSorter getCustomSorter() {
        switch (this) {
            case NoSort:
                return new NoSort();

            case TwoLevelSort:
                return new TwoLevelSort();

            case GeometricMeanSort:
                return new GeometricMeanSort();

            default:
                throw new AssertionError(this);
        }
    }
}
