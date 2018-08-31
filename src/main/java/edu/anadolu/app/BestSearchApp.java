package edu.anadolu.app;

import edu.anadolu.BestSearcher;
import edu.anadolu.Filler;
import edu.anadolu.SimilarityConfig;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Application to search tracks in MPD index
 */
public class BestSearchApp {

    public static void main(String[] args) {
        if (args.length == 4) {
            Path indexPath = Paths.get(args[0]);
            Path challengePath = Paths.get(args[1]);
            Path resultPath = Paths.get(args[2]);
            SimilarityConfig similarityConfig = SimilarityConfig.valueOf(args[3]);

            try (BestSearcher searcher = new BestSearcher(indexPath, challengePath, resultPath, similarityConfig)) {
                searcher.search();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Index path, challenge path, result path and format should be given as arguments");
        }
    }
}
