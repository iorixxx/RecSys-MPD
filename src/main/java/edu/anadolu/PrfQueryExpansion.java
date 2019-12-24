package edu.anadolu;

import com.google.gson.Gson;
import edu.anadolu.sorter.CustomSorter;
import edu.anadolu.sorter.RM1;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermStatistics;
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

public class PrfQueryExpansion implements Closeable {

    public static final Pattern whiteSpaceSplitter = Pattern.compile("\\s+");

    private final MPD challenge;
    private final IndexReader reader;

    private final SimilarityConfig similarityConfig;

    private final Integer maxPlaylist;

    private final Integer maxTrack;

    private final SearchFieldConfig searchFieldConfig;

    public PrfQueryExpansion(Path indexPath, Path challengePath, SimilarityConfig similarityConfig, Integer maxPlaylist, Integer maxTrack, SearchFieldConfig searchFieldConfig) throws Exception {

        if (!Files.exists(indexPath) || !Files.isDirectory(indexPath) || !Files.isReadable(indexPath)) {
            throw new IllegalArgumentException(indexPath + " does not exist or is not a directory.");
        }

        final Gson GSON = new Gson();

        this.reader = DirectoryReader.open(FSDirectory.open(indexPath));
        this.maxPlaylist = maxPlaylist;
        this.maxTrack = maxTrack;
        this.similarityConfig = similarityConfig;
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

                    case Whole:
                        tracksOnly(playlist.tracks, playlist.pid, out, Track::whole, "whole_playlist");
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

    IndexSearcher searcher;

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

        HashSet<String> candidates = new HashSet<>();

        Query query = queryParser.parse(QueryParserBase.escape(builder.toString().trim()));

        searcher = new IndexSearcher(reader);

        searcher.setSimilarity(similarityConfig.getSimilarity());

        ScoreDoc[] hits = searcher.search(query, maxPlaylist).scoreDocs;

        if (hits.length == 0) {
            return; //TODO handle such cases
        }

        List<Document> topK = new ArrayList<>();

        Map<Integer, Float> precomputed = new HashMap<>();

        for (ScoreDoc hit : hits) {
            int docID = hit.doc;

            Document doc = searcher.doc(docID);

            if (Integer.parseInt(doc.get("id")) == playlistID) continue;

            topK.add(doc);

            String[] trackURIs = whiteSpaceSplitter.split(doc.get("track_uris"));

            for (int i = 0; i < trackURIs.length; i++) {

                String trackURI = trackURIs[i];
                if (!seeds.contains(trackURI)) candidates.add(trackURI);
            }

            float seed_prf = 1;

            for (Track seed : tracks) {
                seed_prf *= rf(seed.track_uri, trackURIs);
            }

            precomputed.put(Integer.parseInt(doc.get("id")), seed_prf);
        }

        List<RecommendedTrack> recommendedTracks = rm1(topK, tracks, new ArrayList<>(candidates), precomputed);

        CustomSorter cs = new RM1();

        cs.sort(recommendedTracks);

        List<RecommendedTrack> subList = recommendedTracks.size() > maxTrack ? recommendedTracks.subList(0, maxTrack) : recommendedTracks;

        export(playlistID, subList, out.get());

        recommendedTracks.clear();
    }

    private List<RecommendedTrack> rm1(List<Document> topK, Track[] seeds, List<String> uniqueTracks, Map<Integer, Float> precomputed) {
        List<RecommendedTrack> rs = new ArrayList<>();

        for (String track_uri : uniqueTracks) {

            float prf = 0;

            for (Document document : topK) {
                String[] trackURIs = whiteSpaceSplitter.split(document.get("track_uris"));

                float track_prf = rf(track_uri, trackURIs);

                prf += (track_prf * precomputed.get(Integer.parseInt(document.get("id"))));
            }

            RecommendedTrack tr = new RecommendedTrack(track_uri);
            tr.maxScore = prf;

            rs.add(tr);
        }

        return rs;
    }

    private float rf(String track, String[] tracks) {

        Set<String> trackSet = new HashSet<>(Arrays.asList(tracks));

        if (trackSet.contains(track))

            return 1 / trackSet.size();
        else return 0f;

    }

    private float rf_dirichlet(String track, String[] tracks, double mu) throws IOException {

        Set<String> trackSet = new HashSet<>(Arrays.asList(tracks));

        int cwd = (trackSet.contains(track)) ? 1 : 0;

        Term term = new Term("track_uris", track);

        TermStatistics termStatistics = searcher.termStatistics(term, TermContext.build(reader.getContext(), term));
        float playlistFreqOfTrack = (float) termStatistics.docFreq() / 1_000_000L;

        float rValue = (float) (cwd + mu * playlistFreqOfTrack) / (float) (trackSet.size() + mu);

        if(rValue > 0)
            return rValue;
        else throw new RuntimeException("smooted probability should not be zero.");

    }

    private static synchronized void export(int playlistID, List<RecommendedTrack> tracks, PrintWriter out) {
        tracks.forEach(track -> {
            out.print(playlistID);
            out.print(",");
            out.print(track.trackURI);
            out.println();
        });
    }
}