package edu.anadolu.app;

import edu.anadolu.ExcludeSeeds;

import java.nio.file.Path;
import java.nio.file.Paths;

public class FillerApp {

    public static void main(String[] args) {

        if (args.length == 3) {

            Path indexPath = Paths.get(args[0]);
            Path challengePath = Paths.get(args[1]);
            Path resultPath = Paths.get(args[2]);

            ExcludeSeeds excludeSeeds = new ExcludeSeeds(indexPath, challengePath);
            excludeSeeds.fillInTheBlanks(resultPath);

        } else {
            System.out.println("Index path and result path should be given as arguments");
        }
    }
}