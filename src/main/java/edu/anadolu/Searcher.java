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
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;


/**
 * Create submission
 */
public class Searcher implements Closeable {

    private final LinkedHashMap<Integer, Integer> pageCount = new LinkedHashMap<>();

    private static final int RESULT_SIZE = 500;

    static final Pattern whiteSpaceSplitter = Pattern.compile("\\s+");
    private final MPD challenge;
    private final IndexReader reader;

    private final StringBuilder builder = new StringBuilder();

    public Searcher(Path indexPath, Path challengePath) throws IOException, ParseException {
        if (!Files.exists(indexPath) || !Files.isDirectory(indexPath) || !Files.isReadable(indexPath)) {
            throw new IllegalArgumentException(indexPath + " does not exist or is not a directory.");
        }

        this.reader = DirectoryReader.open(FSDirectory.open(indexPath));

        try (BufferedReader reader = Files.newBufferedReader(challengePath)) {
            this.challenge = MPD.GSON.fromJson(reader, MPD.class);
        }

        for (int i = 1; i <= 100; i++)
            pageCount.put(i, 0);

        pageCount.put(-1, 0);
    }

    public void search(Format format) throws IOException, ParseException {

        for (Playlist playlist : this.challenge.playlists) {

            LinkedHashSet<String> submission;
            if (playlist.tracks.length == 0) {
                submission = titleOnly(playlist.name, playlist.pid);
            } else {
                submission = tracksOnly(playlist.tracks, playlist.pid);
            }

            if (submission.size() == RESULT_SIZE)
                export(submission, playlist.pid, format);
        }
    }

    public void exportResultsToFile(Path resultPath) {
        try (BufferedWriter writer = Files.newBufferedWriter(resultPath, Charset.forName("UTF-8"))) {
            writer.write(builder.toString());
        } catch (IOException e) {
            e.printStackTrace();
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

                if (submission.size() < RESULT_SIZE) {
                    submission.add(t);
                } else {
                    finish = true;
                    break;
                }

            }

            incrementPageCountMap(i);

            if (finish) break;

        }


        if (submission.size() != RESULT_SIZE)
            System.out.println("warning result set for " + pId + " size " + submission.size());

        return submission;
    }

    private void incrementPageCountMap(int i) {
        // count max page count: How many results do we iterate to fill quota of 500?
        if (pageCount.containsKey(i + 1)) {
            int count = pageCount.get(i + 1);
            count++;
            pageCount.put(i + 1, count);
        } else {
            int count = pageCount.get(-1);
            count++;
            pageCount.put(-1, count);
        }
    }

    public void printPageCountMap() {
        pageCount.entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .filter(o -> o.getValue() > 0)
                .forEach(o -> System.out.println(o.getKey() + "\t" + o.getValue()));
    }

    /**
     * Predict tracks for a playlist given its tracks only
     */
    public LinkedHashSet<String> tracksOnly(Track[] tracks, int pId) throws ParseException, IOException {

        IndexSearcher searcher = new IndexSearcher(reader);
        searcher.setSimilarity(new BM25Similarity());
        QueryParser queryParser = new QueryParser("track_uris", Indexer.analyzer());
        queryParser.setDefaultOperator(QueryParser.Operator.OR);

        HashSet<String> seeds = new HashSet<>(100);

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

                if (submission.size() < RESULT_SIZE) {
                    submission.add(t);
                } else {
                    finish = true;
                    break;
                }

            }

            incrementPageCountMap(i);

            if (finish) break;

        }

        seeds.clear();

        if (submission.size() != RESULT_SIZE)
            System.out.println("warning result set for " + pId + " size " + submission.size());

        return submission;
    }

    private void export(LinkedHashSet<String> submission, int pid, Format format) {
        switch (format) {
            case RECSYS:
                builder.append(pid);

                for (String s : submission) {
                    builder.append(", ");
                    builder.append(s);
                }

                builder.append("\n");
                break;
            case TREC:
                int i = 1;
                for (String s : submission) {
                    builder.append(pid);
                    builder.append("\tQ0\t");
                    builder.append(s);
                    builder.append("\t");
                    builder.append(i);
                    builder.append("\t");
                    builder.append(i);
                    builder.append("\t");
                    builder.append("BM25");
                    builder.append("\n");

                    i++;
                }
                break;
        }
    }
}

