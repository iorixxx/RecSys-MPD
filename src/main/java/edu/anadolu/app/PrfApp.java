package edu.anadolu.app;

import edu.anadolu.*;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PrfApp {

    public static void main(String[] args) {
        if (args.length == 7) {
            Path indexPath = Paths.get(args[0]);
            Path challengePath = Paths.get(args[1]);
            Path resultPath = Paths.get(args[2]);
            SimilarityConfig similarityConfig = SimilarityConfig.valueOf(args[3]);
            Integer maxPlaylist = Integer.valueOf(args[4]);
            Integer maxTrack = Integer.valueOf(args[5]);
            SearchFieldConfig searchFieldConfig = SearchFieldConfig.valueOf(args[6]);

            try (PrfQueryExpansion searcher = new PrfQueryExpansion(indexPath, challengePath, similarityConfig, maxPlaylist, maxTrack, searchFieldConfig)) {
                System.out.println("Sampling has started...");
                searcher.search(resultPath);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Index path, challenge path, result path and format should be given as arguments");
        }
    }
}
