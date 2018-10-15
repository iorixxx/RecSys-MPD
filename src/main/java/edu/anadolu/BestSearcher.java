package edu.anadolu;

import com.google.gson.Gson;
import edu.anadolu.sorter.CustomSorter;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Create submission
 */
public class BestSearcher implements Closeable {

    private static final Pattern whiteSpaceSplitter = Pattern.compile("\\s+");

    private final MPD challenge;
    private final IndexReader reader;

    private final SimilarityConfig similarityConfig;

    private final Integer maxPlaylist;

    private final Integer maxTrack;

    private final CustomSorter sorter;

    private final SearchFieldConfig searchFieldConfig;

    public BestSearcher(Path indexPath, Path challengePath, SimilarityConfig similarityConfig, Integer maxPlaylist, Integer maxTrack,
                        CustomSorterConfig sorterConfig, SearchFieldConfig searchFieldConfig) throws Exception {

        if (!Files.exists(indexPath) || !Files.isDirectory(indexPath) || !Files.isReadable(indexPath)) {
            throw new IllegalArgumentException(indexPath + " does not exist or is not a directory.");
        }

        final Gson GSON = new Gson();

        this.reader = DirectoryReader.open(FSDirectory.open(indexPath));
        this.maxPlaylist = maxPlaylist;
        this.maxTrack = maxTrack;
        this.similarityConfig = similarityConfig;
        this.sorter = sorterConfig.getCustomSorter();
        this.searchFieldConfig = searchFieldConfig;

        try (BufferedReader reader = Files.newBufferedReader(challengePath)) {
            this.challenge = GSON.fromJson(reader, MPD.class);
        }
    }

    public void search(Path resultPath) throws IOException {

        final AtomicReference<PrintWriter> out = new AtomicReference<>(new PrintWriter(Files.newBufferedWriter(resultPath, StandardCharsets.US_ASCII)));

        Arrays.stream(this.challenge.playlists).parallel().forEach(playlist -> {
            try {
                switch (searchFieldConfig) {
                    case Track:
                        tracksOnly(playlist.tracks, playlist.pid, out, Track::track_uri, "track_uris");
                        break;

                    case Artist:
                        tracksOnly(playlist.tracks, playlist.pid, out, Track::artist_uri, "artist_uris");
                        break;

                    case Album:
                        tracksOnly(playlist.tracks, playlist.pid, out, Track::album_uri, "album_uris");
                        break;
                }
            } catch (IOException | ParseException e) {
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

    private void tracksOnly(Track[] tracks, int playlistID, AtomicReference<PrintWriter> out, Function<Track, String> map, String field) throws ParseException, IOException {

        QueryParser queryParser = new QueryParser(field, new WhitespaceAnalyzer());
        queryParser.setDefaultOperator(QueryParser.Operator.OR);

        HashSet<String> seeds = new HashSet<>(100);

        StringBuilder builder = new StringBuilder();

        for (Track track : tracks) {
            String trackURI = track.track_uri;

            if (seeds.contains(trackURI)) continue;

            builder.append(map.apply(track)).append(' ');

            seeds.add(trackURI);
        }

        LinkedHashMap<String, RecommendedTrack> recommendations = new LinkedHashMap<>();

        Query query = queryParser.parse(QueryParserBase.escape(builder.toString().trim()));

        IndexSearcher searcher = new IndexSearcher(reader);

        searcher.setSimilarity(similarityConfig.getSimilarity());

        ScoreDoc[] hits = searcher.search(query, maxPlaylist).scoreDocs;

        if (hits.length == 0) {
            //TODO handle such cases
        }

        for (ScoreDoc hit : hits) {
            int docID = hit.doc;

            Document doc = searcher.doc(docID);

            if (Integer.parseInt(doc.get("id")) == playlistID) continue;

            String[] trackURIs = whiteSpaceSplitter.split(doc.get("track_uris"));
            int pos = trackURIs.length;

            for (String trackURI : trackURIs) {

                if (!seeds.contains(trackURI)) {

                    RecommendedTrack rt = recommendations.getOrDefault(trackURI, new RecommendedTrack(trackURI));
                    rt.searchResultFrequency += 1;

                    if (rt.maxScore < hit.score) {
                        rt.maxScore = hit.score;
                        rt.pos = pos;
                        rt.playlistId = Integer.parseInt(doc.get("id"));
                        rt.luceneId = hit.doc;
                    }

                    recommendations.putIfAbsent(trackURI, rt);
                }

                pos--;
            }
        }

        List<RecommendedTrack> recommendedTracks = new ArrayList<>(recommendations.values());

        sorter.sort(recommendedTracks);

        if (recommendedTracks.size() > maxTrack)
            export(playlistID, recommendedTracks.subList(0, maxTrack), out.get());
        else
            export(playlistID, recommendedTracks, out.get());

        System.out.println("Tracks only search for pid: " + playlistID);
        recommendedTracks.clear();
    }

    private void album(List<RecommendedTrack> recommendedTracks) {

    }


    private static synchronized void export(int playlistID, List<RecommendedTrack> tracks, PrintWriter out) {
        tracks.forEach(track -> {
            out.print(playlistID);
            out.print(",");
            out.print(track.trackURI);
            out.print(",");
            out.print(track.searchResultFrequency);
            out.print(",");
            out.print(track.maxScore);
            out.print(",");
            out.print(track.pos);
            out.println();
        });
    }
}