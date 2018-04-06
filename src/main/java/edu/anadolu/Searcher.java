package edu.anadolu;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.regex.Pattern;


/**
 * Create submission
 */
public class Searcher implements Closeable {


    static final Pattern whiteSpaceSplitter = Pattern.compile("\\s+");
    private final MPD challenge;
    private final IndexReader reader;


    public Searcher(Path indexPath, Path challengePath) throws IOException, ParseException {
        if (!Files.exists(indexPath) || !Files.isDirectory(indexPath) || !Files.isReadable(indexPath)) {
            throw new IllegalArgumentException(indexPath + " does not exist or is not a directory.");
        }


        this.reader = DirectoryReader.open(FSDirectory.open(indexPath));

        try (BufferedReader reader = Files.newBufferedReader(challengePath)) {
            this.challenge = Indexer.gson.fromJson(reader, MPD.class);
        }


        for (Playlist playlist : this.challenge.playlists) {

            if (playlist.tracks.length == 0) {
                titleOnly(playlist.name, playlist.pid);
            } else {
                tracksOnly(playlist.tracks, playlist.pid);
            }
        }
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    /**
     * Predict tracks for a playlist given its title only
     */
    public void titleOnly(String title, int pId) throws ParseException, IOException {

        IndexSearcher searcher = new IndexSearcher(reader);
        searcher.setSimilarity(new BM25Similarity());
        QueryParser queryParser = new QueryParser("name", Indexer.analyzer());
        queryParser.setDefaultOperator(QueryParser.Operator.AND);

        Query query = queryParser.parse(QueryParserBase.escape(title));


        LinkedHashSet<String> submission = new LinkedHashSet<>(500);

        ScoreDoc[] hits = searcher.search(query, Integer.MAX_VALUE).scoreDocs;

        if (hits.length == 0) {
            System.out.println("Zero result found for title : " + title);
            return;
        }


        boolean finish = false;

        for (int i = 0; i < hits.length; i++) {
            int docId = hits[i].doc;
            Document doc = searcher.doc(docId);

            // if given pId found in the index, skip it: to use 1M index for all kind of tasks (original test set, or the one we created

            if (Integer.parseInt(doc.get("id")) == pId) continue;

            String trackURIs = doc.get("track_uris");

            String[] tracks = whiteSpaceSplitter.split(trackURIs);

            for (String t : tracks) {

                if (submission.size() < 500) {
                    submission.add(t);
                } else {
                    finish = true;
                    break;
                }

            }

            if (finish) break;

        }


        if (submission.size() < 500)
            System.out.println("warning result set for " + pId + " size " + submission.size());

        TREC(submission, pId);


    }

    /**
     * Predict tracks for a playlist given its tracks only
     */
    public void tracksOnly(Track[] tracks, int pId) throws ParseException, IOException {

        IndexSearcher searcher = new IndexSearcher(reader);
        searcher.setSimilarity(new BM25Similarity());
        QueryParser queryParser = new QueryParser("track_uris", Indexer.analyzer());
        queryParser.setDefaultOperator(QueryParser.Operator.OR);

        LinkedHashSet<String> seeds = new LinkedHashSet<>(100);

        StringBuilder builder = new StringBuilder();
        for (Track track : tracks) {
            builder.append(track.track_uri).append(' ');
            seeds.add(track.artist_uri);
        }
        Query query = queryParser.parse(QueryParserBase.escape(builder.toString().trim()));


        ScoreDoc[] hits = searcher.search(query, Integer.MAX_VALUE).scoreDocs;

        if (hits.length == 0) {
            System.out.println("Zero result found for pId : " + pId);
            return;
        }

        LinkedHashSet<String> submission = new LinkedHashSet<>(500);

        boolean finish = false;

        for (int i = 0; i < hits.length; i++) {
            int docId = hits[i].doc;
            Document doc = searcher.doc(docId);
            if (Integer.parseInt(doc.get("id")) == pId) continue;

            String trackURIs = doc.get("track_uris");


            for (String t : whiteSpaceSplitter.split(trackURIs)) {

                if (seeds.contains(t)) continue;

                if (submission.size() < 500) {
                    submission.add(t);
                } else {
                    finish = true;
                    break;
                }

            }

            if (finish) break;

        }


        if (submission.size() != 500)
            System.out.println("warning result set for " + pId + " size " + submission.size());

        TREC(submission, pId);


    }

    void ResSys(LinkedHashSet<String> submission, int pId) {
        System.out.print(pId);

        for (String s : submission)
            System.out.print(' ' + s);

        System.out.println();

    }

    void TREC(LinkedHashSet<String> submission, int pId) {
        int i = 0;
        for (String s : submission) {

            i++;

            System.out.print(pId);
            System.out.print("\tQ0\t");
            System.out.print(s);
            System.out.print("\t");
            System.out.print(i);
            System.out.print("\t");
            System.out.print(i);
            System.out.print("\t");
            System.out.print("BM25");

            System.out.println();

        }

    }

}

