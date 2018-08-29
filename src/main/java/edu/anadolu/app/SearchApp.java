package edu.anadolu.app;

import edu.anadolu.Searcher;
import edu.anadolu.SimilarityConfig;
import edu.anadolu.SpanNearConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Application to search tracks in MPD index
 */
public class SearchApp {

    public static void main(String[] args) throws IOException {
        if (args.length == 8) {
            Path indexPath = Paths.get(args[0]);
            Path challengePath = Paths.get(args[1]);
            Path resultPath = Paths.get(args[2]);
            SimilarityConfig similarityConfig = SimilarityConfig.valueOf(args[4]);
            boolean useOnlyLonger = Boolean.valueOf(args[5]);
            SpanNearConfig.RelaxMode relaxMode = SpanNearConfig.RelaxMode.valueOf(args[6]);
            boolean sortByFollower = Boolean.valueOf(args[7]);
            try (Searcher searcher = new Searcher(indexPath, challengePath, similarityConfig, useOnlyLonger, sortByFollower)) {
                searcher.search(resultPath, relaxMode);
            }
        } else {
            System.out.println("Index path, challenge path, result path and format should be given as arguments");
        }
    }
}
