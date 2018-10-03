package edu.anadolu.app;

import edu.anadolu.BestSearcher;
import edu.anadolu.CustomSorterConfig;
import edu.anadolu.SimilarityConfig;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Application to search tracks in MPD index
 */
public class BestSearchApp {

    public static void main(String[] args) {
        if (args.length == 7) {
            Path indexPath = Paths.get(args[0]);
            Path challengePath = Paths.get(args[1]);
            Path resultPath = Paths.get(args[2]);
            SimilarityConfig similarityConfig = SimilarityConfig.valueOf(args[3]);
            Integer maxPlaylist = Integer.valueOf(args[4]);
            Integer maxTrack = Integer.valueOf(args[5]);
            CustomSorterConfig sorterConfig = CustomSorterConfig.valueOf(args[6]);

            try (BestSearcher searcher = new BestSearcher(indexPath, challengePath, resultPath, similarityConfig, maxPlaylist, maxTrack, sorterConfig)) {
                searcher.search();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Index path, challenge path, result path and format should be given as arguments");
        }
    }
}
