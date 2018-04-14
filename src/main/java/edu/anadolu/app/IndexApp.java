package edu.anadolu.app;

import edu.anadolu.Indexer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Application to index MPD
 */
public class IndexApp {

    public static void main(String[] args) {
        if (args.length == 2) {
            Path indexPath = Paths.get(args[0]);
            Path mpdPath = Paths.get(args[1]);

            Indexer indexer = new Indexer();

            try {
                int indexSize = indexer.index(indexPath, mpdPath);

                System.out.println(indexSize + " documents are indexed");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Index path and MPD path should be given as arguments");
        }
    }
}
