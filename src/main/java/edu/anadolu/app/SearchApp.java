package edu.anadolu.app;

import edu.anadolu.Format;
import edu.anadolu.Searcher;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Application to search tracks in MPD index
 */
public class SearchApp {

    private Path indexPath;

    private Path challengePath;

    public SearchApp(String indexPath, String challengePath) {
        this.indexPath = Paths.get(indexPath);
        this.challengePath = Paths.get(challengePath);
    }

    public void execute() {
        try {
            Searcher searcher = new Searcher(indexPath, challengePath);

            searcher.search(Format.TREC);
            searcher.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length == 2) {
            String indexPath = args[0];
            String challengePath = args[1];

            SearchApp app = new SearchApp(indexPath, challengePath);

            app.execute();
        }
        else {
            System.out.println("Index path and challenge path should be given as arguments");
        }
    }
}
