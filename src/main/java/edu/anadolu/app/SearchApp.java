package edu.anadolu.app;

import edu.anadolu.*;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Application to search tracks in MPD index
 */
public class SearchApp {

    public static void main(String[] args) {
        if (args.length == 8) {
            Path indexPath = Paths.get(args[0]);
            Path challengePath = Paths.get(args[1]);
            Path resultPath = Paths.get(args[2]);
            Format format = Format.valueOf(args[3]);
            SimilarityConfig similarityConfig = SimilarityConfig.valueOf(args[4]);
            Filler filler = Filler.valueOf(args[5]);

            boolean useOnlyLonger = Boolean.valueOf(args[6]);

            SpanNearConfig.RelaxMode relaxMode = SpanNearConfig.RelaxMode.valueOf(args[7]);

            try (Searcher searcher = new Searcher(indexPath, challengePath, similarityConfig, filler, useOnlyLonger)) {
                searcher.search(format, resultPath, relaxMode);
                searcher.printPageCountMap();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Index path, challenge path, result path and format should be given as arguments");
        }
    }
}
