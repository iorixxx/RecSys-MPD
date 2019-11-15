package edu.anadolu.sorter;

import edu.anadolu.RecommendedTrack;

import java.util.Comparator;
import java.util.List;

public class RM1 implements CustomSorter {

    @Override
    public void sort(List<RecommendedTrack> tracks) {
        tracks.sort(Comparator
                .comparingDouble(RecommendedTrack::getMaxScore)
                .reversed()
        );
    }
}
