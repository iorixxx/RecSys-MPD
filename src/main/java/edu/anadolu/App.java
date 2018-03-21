package edu.anadolu;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) throws IOException {
        System.out.println("Hello World!");

        Path indexPath = Paths.get("/Users/iorixxx/Desktop/MPD.Index");
        Path mdp = Paths.get("/Users/iorixxx/Downloads/mpd.v1/data");


        Indexer indexer = new Indexer();

        int numIndexed = indexer.index(indexPath, mdp);

        System.out.println("Total " + numIndexed + " documents indexed.");

    }
}
