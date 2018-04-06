package edu.anadolu;

import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) throws IOException, ParseException {

        Path indexPath = Paths.get("/Users/iorixxx/Desktop/MPD.Index");
        Path mdp = Paths.get("/Users/iorixxx/Downloads/mpd.v1/data");


        //  Indexer indexer = new Indexer();

        //  int numIndexed = indexer.index(indexPath, mdp);

        //  System.out.println("Total " + numIndexed + " documents indexed.");


        Path challenge = Paths.get("/Users/iorixxx/Downloads/challenge.v1/challenge_set.json");
        Searcher searcher = new Searcher(indexPath, challenge);

        searcher.search(Format.TREC);

        searcher.close();

    }
}
