package edu.anadolu.app;

import edu.anadolu.Filler;

import java.nio.file.Path;
import java.nio.file.Paths;

public class FillerApp {

    public static void main(String[] args) {

        if (args.length == 2) {

            Path indexPath = Paths.get(args[0]);
            Path resultPath = Paths.get(args[1]);

            Filler filler = new Filler(indexPath);
            filler.fillInTheBlanks(resultPath);

        } else {
            System.out.println("Index path and result path should be given as arguments");
        }
    }
}
