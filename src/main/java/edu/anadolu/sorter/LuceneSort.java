package edu.anadolu.sorter;

import edu.anadolu.RecommendedTrack;

import java.util.Comparator;
import java.util.List;

/**
 * Created by aliyurekli on 10/3/2018.
 */
public class LuceneSort implements CustomSorter {

    @Override
    public void sort(List<RecommendedTrack> tracks) {

        tracks.sort(Comparator
                .comparingDouble(RecommendedTrack::getMaxScore)
                .thenComparingInt(RecommendedTrack::getSearchResultFrequency)
                .reversed()
        );
    }
}
