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

    public void search(Format format) throws IOException, ParseException {

        for (Playlist playlist : this.challenge.playlists) {

            LinkedHashSet<String> submission;
            if (playlist.tracks.length == 0) {
                submission = titleOnly(playlist.name, playlist.pid);
            } else {
                submission = tracksOnly(playlist.tracks, playlist.pid);
            }

            if (Format.RecSys.equals(format))
                ResSys(submission, playlist.pid);
            else if (Format.TREC.equals(format))
                TREC(submission, playlist.pid);
        }
    }

    public Searcher(Path indexPath, Path challengePath) throws IOException, ParseException {
        if (!Files.exists(indexPath) || !Files.isDirectory(indexPath) || !Files.isReadable(indexPath)) {
            throw new IllegalArgumentException(indexPath + " does not exist or is not a directory.");
        }


        this.reader = DirectoryReader.open(FSDirectory.open(indexPath));

        try (BufferedReader reader = Files.newBufferedReader(challengePath)) {
            this.challenge = Indexer.gson.fromJson(reader, MPD.class);
        }


    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    /**
     * Predict tracks for a playlist given its title only
     */
    public LinkedHashSet<String> titleOnly(String title, int pId) throws ParseException, IOException {

        IndexSearcher searcher = new IndexSearcher(reader);
        searcher.setSimilarity(new BM25Similarity());
        QueryParser queryParser = new QueryParser("name", Indexer.analyzer());
        queryParser.setDefaultOperator(QueryParser.Operator.AND);

        Query query = queryParser.parse(QueryParserBase.escape(title));


        LinkedHashSet<String> submission = new LinkedHashSet<>(500);

        ScoreDoc[] hits = searcher.search(query, Integer.MAX_VALUE).scoreDocs;

        /**
         * Try with OR operator, relaxed mode.
         */
        if (hits.length == 0) {

            queryParser = new QueryParser("name", Indexer.analyzer());
            queryParser.setDefaultOperator(QueryParser.Operator.OR);

            query = queryParser.parse(QueryParserBase.escape(title));


            hits = searcher.search(query, Integer.MAX_VALUE).scoreDocs;

            if (hits.length == 0) {
                System.out.println("Zero result found for title : " + title);
                return new LinkedHashSet<>();
            }

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

        return submission;


    }

    /**
     * Predict tracks for a playlist given its tracks only
     */
    public LinkedHashSet<String> tracksOnly(Track[] tracks, int pId) throws ParseException, IOException {

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
            return new LinkedHashSet<>();
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


        seeds.clear();
        if (submission.size() != 500)
            System.out.println("warning result set for " + pId + " size " + submission.size());

        return submission;


    }

    /**
     * this is a sample submission for the recsys challenge
     * all fields are comma separated. It is ok, but optional
     * to have whitespace before and after the comma.
     */
    void ResSys(LinkedHashSet<String> submission, int pId) {

        if (submission.isEmpty()) return;

        System.out.print(pId);

        for (String s : submission)
            System.out.print(", " + s);

        System.out.println();

    }

    void TREC(LinkedHashSet<String> submission, int pId) {

        if (submission.isEmpty()) return;

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

