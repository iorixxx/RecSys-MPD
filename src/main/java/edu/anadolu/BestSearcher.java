package edu.anadolu;

import com.google.gson.Gson;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.spans.*;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Collectors.toMap;


/**
 * Create submission
 */
public class BestSearcher implements Closeable {

    private static final Pattern whiteSpaceSplitter = Pattern.compile("\\s+");

    private final MPD challenge;
    private final IndexReader reader;
    private final IndexSearcher searcher;

    private final AtomicReference<PrintWriter> out;

    private final Integer maxPlaylist;

    private final Integer maxTrack;

    public BestSearcher(Path indexPath, Path challengePath, Path resultPath, SimilarityConfig similarityConfig, Integer maxPlaylist, Integer maxTrack) throws Exception {
        if (!Files.exists(indexPath) || !Files.isDirectory(indexPath) || !Files.isReadable(indexPath)) {
            throw new IllegalArgumentException(indexPath + " does not exist or is not a directory.");
        }

        final Gson GSON = new Gson();

        this.reader = DirectoryReader.open(FSDirectory.open(indexPath));
        this.searcher = new IndexSearcher(reader);
        this.out = new AtomicReference<>(new PrintWriter(Files.newBufferedWriter(resultPath, StandardCharsets.US_ASCII)));
        this.maxPlaylist = maxPlaylist;
        this.maxTrack = maxTrack;

        try (BufferedReader reader = Files.newBufferedReader(challengePath)) {
            this.challenge = GSON.fromJson(reader, MPD.class);
        }

        this.searcher.setSimilarity(similarityConfig.getSimilarity());
    }

    public void search() {
        Arrays.stream(this.challenge.playlists).parallel().forEach(playlist -> {

            try {
                HashSet<String> results = new LinkedHashSet<>();

                if (playlist.tracks.length == 0) {
                    titleOnly(playlist.name, playlist.pid, results);
                }
                else {
                    if (playlist.isSequential()) {
                        firstNTracks(playlist.tracks, playlist.pid, results);
                    }
                    else {
                        tracksOnly(playlist.tracks, playlist.pid);
                    }
                }

                results.clear();
            }
            catch (IOException | ParseException e) {
                throw new RuntimeException(e);
            }
        });

        out.get().flush();
        out.get().close();
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    /**
     * Predict tracks for a playlist given its title only.
     */
    private void titleOnly(String title, int playlistID, HashSet<String> results) throws ParseException, IOException {

        QueryParser queryParser = new QueryParser("name", Indexer.icu());
        queryParser.setDefaultOperator(QueryParser.Operator.AND);

        Query query = queryParser.parse(QueryParserBase.escape(title));

        ScoreDoc[] hits = searcher.search(query, maxPlaylist).scoreDocs;

        /*
         * Try with OR operator, relaxed mode.
         */
        if (hits.length == 0) {
            queryParser.setDefaultOperator(QueryParser.Operator.OR);

            hits = searcher.search(query, maxPlaylist).scoreDocs;
        }

        for (ScoreDoc hit : hits) {
            int docID = hit.doc, pos = -1;

            Document doc = searcher.doc(docID);

            if (Integer.parseInt(doc.get("id")) == playlistID) continue;

            String[] trackURIs = whiteSpaceSplitter.split(doc.get("track_uris"));

            for (String trackURI : trackURIs) {
                if (!results.contains(trackURI)) {
                    if (results.size() < maxTrack) {
                        pos ++;
                        results.add(trackURI);

                        export(playlistID, trackURI, hit.score, trackURIs.length - pos);
                    }
                    else break;
                }
            }
        }
    }

    /**
     * Predict tracks for a playlist given its tracks only.
     * Works best with random tracks category 8 and category 10.
    private void tracksOnly(Track[] tracks, int playlistID, HashSet<String> results) throws ParseException, IOException {

        QueryParser queryParser = new QueryParser("track_uris", new WhitespaceAnalyzer());
        queryParser.setDefaultOperator(QueryParser.Operator.OR);

        HashSet<String> seeds = new HashSet<>(100);

        StringBuilder builder = new StringBuilder();

        for (Track track : tracks) {
            String trackURI = track.track_uri;

            if (seeds.contains(trackURI)) continue;

            builder.append(trackURI).append(' ');

            seeds.add(trackURI);
        }

        Query query = queryParser.parse(QueryParserBase.escape(builder.toString().trim()));

        ScoreDoc[] hits = searcher.search(query, maxPlaylist).scoreDocs;

        for (ScoreDoc hit : hits) {
            int docID = hit.doc, pos = -1;

            Document doc = searcher.doc(docID);

            if (Integer.parseInt(doc.get("id")) == playlistID) continue;

            String[] trackURIs = whiteSpaceSplitter.split(doc.get("track_uris"));

            if (trackURIs.length <= seeds.size())
                System.out.println("**** document length " + trackURIs.length + " is less than or equal to query length " + seeds.size());

            for (String trackURI : trackURIs) {
                if (!results.contains(trackURI) && !seeds.contains(trackURI)) {
                    if (results.size() < MAX_RESULT_SIZE) {
                        pos ++;
                        results.add(trackURI);

                        export(playlistID, trackURI, hit.score, trackURIs.length - pos);
                    }
                    else break;
                }
            }
        }

        seeds.clear();

        System.out.println("Tracks only search for pid: " + playlistID);
    }
    */
    private void tracksOnly(Track[] tracks, int playlistID) throws ParseException, IOException {

        QueryParser queryParser = new QueryParser("track_uris", new WhitespaceAnalyzer());
        queryParser.setDefaultOperator(QueryParser.Operator.OR);

        HashSet<String> seeds = new HashSet<>(100);

        StringBuilder builder = new StringBuilder();

        for (Track track : tracks) {
            String trackURI = track.track_uri;

            if (seeds.contains(trackURI)) continue;

            builder.append(trackURI).append(' ');

            seeds.add(trackURI);
        }

        HashMap<String, Integer> recommendations = new HashMap<>();

        Query query = queryParser.parse(QueryParserBase.escape(builder.toString().trim()));

        ScoreDoc[] hits = searcher.search(query, maxPlaylist).scoreDocs;

        for (ScoreDoc hit : hits) {
            int docID = hit.doc, pos = -1;

            Document doc = searcher.doc(docID);

            if (Integer.parseInt(doc.get("id")) == playlistID) continue;

            String[] trackURIs = whiteSpaceSplitter.split(doc.get("track_uris"));

            for (String trackURI : trackURIs) {
                if (!seeds.contains(trackURI)) {
                    if (!recommendations.containsKey(trackURI)) {
                        recommendations.put(trackURI, 1);
                    }
                    else {
                        recommendations.put(trackURI, recommendations.get(trackURI) + 1);
                    }
                }
            }

        }

        Map<String, Integer> sorted = recommendations
                .entrySet()
                .stream()
                .sorted(Collections.reverseOrder(comparingByValue()))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));

        int count = 0;

        for (String track : sorted.keySet()) {
            count ++;

            export(playlistID, track, recommendations.get(track), 0);

            if (count == maxTrack)
                break;
        }

        seeds.clear();

        System.out.println("Tracks only search for pid: " + playlistID);
    }

    /**
     * Predict tracks for a playlist given its first N tracks, where N can equal 1, 5, 10, 25, or 100.
     */
    private void firstNTracks(Track[] tracks, int playlistID, HashSet<String> results) throws ParseException, IOException {

        LinkedHashSet<String> seeds = new LinkedHashSet<>(100);

        ArrayList<SpanQuery> clauses = new ArrayList<>(tracks.length);


        for (Track track : tracks) {

            // skip duplicate tracks in the playlist. Only consider the first occurrence of the track.
            if (seeds.contains(track.track_uri)) continue;

            seeds.add(track.track_uri);
            clauses.add(new SpanTermQuery(new Term("track_uris", track.track_uri)));
        }

        //TODO try to figure out n from tracks.length

        final int n;
        if (tracks.length < 6)
            n = tracks.length + 2; // for n=1 and n=5 use 2 and 7
        else if (tracks.length < 26)
            n = (int) (tracks.length * 1.5); // for n=10 and n=25 use 15 and 37
        else
            n = (int) (tracks.length * 1.25); // for n=100 use 125

        //TODO experiment with SpanOrQuery or SpanNearQuery. Which one performs better?
        final SpanFirstQuery
            spanFirstQuery = new SpanFirstQuery(
                    clauses.size() == 1 ? clauses.get(0) : new SpanNearQuery(clauses.toArray(new SpanQuery[clauses.size()]), 0, true), n);


        // todo ScoreDoc[] hits = searcher.search(spanFirstQuery, Integer.MAX_VALUE).scoreDocs;
        ScoreDoc[] hits = searcher.search(spanFirstQuery, maxPlaylist).scoreDocs;

        if (hits.length == 0) {
            System.out.println("SpanFirst found zero result found for playlistID : " + playlistID + " first " + tracks.length);
            tracksOnly(tracks, playlistID);
        }

        for (ScoreDoc hit : hits) {
            int docID = hit.doc, pos = -1;

            Document doc = searcher.doc(docID);

            if (Integer.parseInt(doc.get("id")) == playlistID) continue;

            String[] trackURIs = whiteSpaceSplitter.split(doc.get("track_uris"));

            for (String trackURI : trackURIs) {
                if (!results.contains(trackURI)) {
                    if (results.size() < maxTrack) {
                        pos ++;
                        results.add(trackURI);

                        export(playlistID, trackURI, hit.score, trackURIs.length - pos);
                    }
                    else break;
                }
            }
        }

        seeds.clear();
        clauses.clear();
    }

    private synchronized void export(int playlistID, String trackURI, double score, int position) {
        out.get().print(playlistID);
        out.get().print(",");
        out.get().print(trackURI);
        out.get().print(",");
        out.get().print(score);
        out.get().print(",");
        out.get().print(position);
        out.get().println();
    }
}