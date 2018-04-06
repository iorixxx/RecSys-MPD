package edu.anadolu.app;

import edu.anadolu.Indexer;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Application to index MPD
 */
public class IndexApp {

    private Path indexPath;

    private Path mpdPath;

    public IndexApp(String indexPath, String mpdPath) {
        this.indexPath = Paths.get(indexPath);
        this.mpdPath = Paths.get(mpdPath);
    }

    public void execute() {
        Indexer indexer = new Indexer();

        try {
            int indexSize = indexer.index(indexPath, mpdPath);

            System.out.println(indexSize + " documents are indexed");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length == 2) {
            String indexPath = args[0];
            String mpdPath = args[1];

            IndexApp app = new IndexApp(indexPath, mpdPath);

            app.execute();
        }
        else {
            System.out.println("Index path and MPD path should be given as arguments");
        }
    }
}
