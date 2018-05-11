package edu.anadolu.app;

import edu.anadolu.Filler;

import java.nio.file.Path;
import java.nio.file.Paths;

public class FillerApp {

    public static void main(String[] args) {

        if (args.length == 3) {

            Path indexPath = Paths.get(args[0]);
            Path challengePath = Paths.get(args[1]);
            Path resultPath = Paths.get(args[2]);

            Filler filler = new Filler(indexPath, challengePath);
            filler.fillInTheBlanks(resultPath);

            Path justPIDs = filler.dumpJustPIDs();
            filler.fillInTheBlanks(justPIDs);


        } else {
            System.out.println("Index path and result path should be given as arguments");
        }
    }
}
