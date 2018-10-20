package edu.anadolu;

import com.google.gson.Gson;
import edu.anadolu.models.*;
import edu.anadolu.sorter.CustomSorter;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.LMSimilarity;
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
import java.util.stream.Collectors;

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

    private static void incrementMap(HashMap<String, Integer> countMap, String key) {
        countMap.put(key, countMap.getOrDefault(key, 0) + 1);
    }

    private static void clearMaps(Map... maps) {
        for (Map map : maps)
            map.clear();
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
            return; //TODO handle such cases
        }

        HashMap<String, Integer> albumFreq = new HashMap<>();
        HashMap<String, String> track2album = new HashMap<>();

        HashMap<String, Integer> artistFreq = new HashMap<>();
        HashMap<String, String> track2artist = new HashMap<>();

        for (ScoreDoc hit : hits) {
            int docID = hit.doc;

            Document doc = searcher.doc(docID);

            if (Integer.parseInt(doc.get("id")) == playlistID) continue;

            String[] trackURIs = whiteSpaceSplitter.split(doc.get("track_uris"));
            String[] albumURIs = whiteSpaceSplitter.split(doc.get("album_uris"));
            String[] artistURIs = whiteSpaceSplitter.split(doc.get("artist_uris"));

            if (albumURIs.length != trackURIs.length) throw new RuntimeException("albumURIs.length!=trackURIs.length");

            int pos = trackURIs.length;

            for (int i = 0; i < trackURIs.length; i++) {

                String trackURI = trackURIs[i];

                pos--;

                if (seeds.contains(trackURI)) continue;


                incrementMap(albumFreq, albumURIs[i]);
                track2album.put(trackURI, albumURIs[i]);

                incrementMap(artistFreq, artistURIs[i]);
                track2artist.put(trackURI, artistURIs[i]);

                RecommendedTrack rt = recommendations.getOrDefault(trackURI, new RecommendedTrack(trackURI));
                rt.searchResultFrequency += 1;
                rt.pIdList.add(hit.doc);

                if (rt.pIdList.size() != rt.searchResultFrequency)
                    throw new RuntimeException("playlist size and searchResultFrequency are not equal to each other");

                if (rt.maxScore < hit.score) {
                    rt.maxScore = hit.score;
                    rt.pos = pos;
                    rt.playlistId = Integer.parseInt(doc.get("id"));
                    rt.luceneId = hit.doc;
                    rt.playlist_length = Long.parseLong(doc.get("playlist_length"));
                }

                recommendations.putIfAbsent(trackURI, rt);

            }
        }

        for (Map.Entry<String, RecommendedTrack> entry : recommendations.entrySet()) {
            String trackURI = entry.getKey();
            entry.getValue().searchResultAlbumFrequency = albumFreq.get(track2album.get(trackURI));
            entry.getValue().searchResultArtistFrequency = artistFreq.get(track2artist.get(trackURI));
        }

        clearMaps(albumFreq, track2album, track2artist, artistFreq);

        List<RecommendedTrack> recommendedTracks = new ArrayList<>(recommendations.values());

        sorter.sort(recommendedTracks);

        List<RecommendedTrack> subList = recommendedTracks.size() > maxTrack ? recommendedTracks.subList(0, maxTrack) : recommendedTracks;

        //album(searcher, tracks, subList, Track::album_uri, "album_uris");

        //album(searcher, tracks, subList, Track::artist_uri, "artist_uris");

        album(searcher, tracks, subList, Track::track_uri, "track_uris");

        export(playlistID, subList, out.get());

        System.out.println("Tracks only search for pid: " + playlistID);

        recommendedTracks.clear();
        recommendations.clear();

    }


    class DocTermStat {

        private final long dl;
        private final int tf;
        private final String word;

        DocTermStat(String word, long dl, int tf) {
            this.dl = dl;
            this.tf = tf;
            this.word = word;
        }
    }

    private void album(IndexSearcher searcher, Track[] tracks, List<RecommendedTrack> recommendedTracks, Function<Track, String> function, String field) throws IOException {

        CollectionStatistics collectionStatistics = searcher.collectionStatistics(field);

        Map<String, TermStatistics> termStatisticsMap = new HashMap<>();

        List<String> subParts = Arrays.stream(tracks).map(function).collect(Collectors.toList());

        for (String word : subParts) {
            if (termStatisticsMap.containsKey(word)) continue;
            Term term = new Term(field, word);
            TermStatistics termStatistics = searcher.termStatistics(term, TermContext.build(reader.getContext(), term));
            termStatisticsMap.put(word, termStatistics);
        }

        LinkedHashMap<RecommendedTrack, List<DocTermStat>> map = new LinkedHashMap<>();


        for (RecommendedTrack track : recommendedTracks) {
            if (map.containsKey(track)) throw new RuntimeException("map should not contain recommended track " + track);
            map.put(track, new ArrayList<>(subParts.size()));
        }

        for (String word : subParts)
            findDoc(map, word, field, searcher);

        for (Map.Entry<RecommendedTrack, List<DocTermStat>> entry : map.entrySet()) {

            Document doc = searcher.doc(entry.getKey().luceneId);

            //int f = 0;

            int sum = 0;
            for (DocTermStat docTermStat : entry.getValue()) {
                if (-1 == docTermStat.tf) continue;
                sum += docTermStat.tf;
            }

            StringBuilder builder = new StringBuilder();
            // System.out.print(Integer.toString(++f));
            //System.out.print(":");
            //System.out.print(sum);
            builder.append(sum).append(",");
            //System.out.print(" ");


            long dl_temp = -1;
            // the use of multiple weighting models in learning to rank, namely the combination of multiple weighting models.
            for (Model m : new Model[]{new PL2(), new DLM(), new DFIC(), new LGD(), new DPH()}) {

                double score = 0.0;

                for (DocTermStat docTermStat : entry.getValue()) {

                    if (-1 == docTermStat.tf) continue;

                    TermStatistics termStatistics = termStatisticsMap.get(docTermStat.word);

                    BasicStats stats = m instanceof DLM ? new LMSimilarity.LMStats(field, 1.0f) : new BasicStats(field, 1.0f);
                    //((LMSimilarity.LMStats) stats).setCollectionProbability((termStatistics.totalTermFreq() + 1F) / (collectionStatistics.sumTotalTermFreq() + 1F));

                    stats.setAvgFieldLength((float) collectionStatistics.sumTotalTermFreq() / collectionStatistics.docCount());

                    stats.setTotalTermFreq(termStatistics.totalTermFreq());
                    stats.setDocFreq(termStatistics.docFreq());

                    stats.setNumberOfDocuments(collectionStatistics.docCount());
                    stats.setNumberOfFieldTokens(collectionStatistics.sumTotalTermFreq());


                    if (m instanceof DLM)
                        ((LMSimilarity.LMStats) stats).setCollectionProbability(new LMSimilarity.DefaultCollectionModel().computeProbability(stats));

                    score += m.score(stats, (float) docTermStat.tf, (float) docTermStat.dl);

                    if (dl_temp == -1)
                        dl_temp = docTermStat.dl;
                    else if (dl_temp != docTermStat.dl)
                        throw new RuntimeException("playlist length should be same!");

                }

                //System.out.print(Integer.toString(++f));
                //System.out.print(":");
                //System.out.print(String.format("%.5f", score));
                builder.append(String.format("%.5f", score)).append(",");
                //System.out.print(" ");

            }

            long dl = Long.parseLong(doc.get("playlist_length"));

            if (dl_temp != dl) throw new RuntimeException("playlist length should be same! " + dl_temp + " " + dl);

            builder.deleteCharAt(builder.length() - 1);
            recommendedTracks.stream().filter(recommendedTrack -> recommendedTrack.luceneId == entry.getKey().luceneId).forEach(recommendedTrack -> {
                if ("album_uris".equals(field))
                    recommendedTrack.album = builder.toString();
                else if ("artist_uris".equals(field))
                    recommendedTrack.artist = builder.toString();
                else if ("track_uris".equals(field))
                    recommendedTrack.track = builder.toString();
            });
        }
    }

    private void findDoc(LinkedHashMap<RecommendedTrack, List<DocTermStat>> map, String word, String
            field, IndexSearcher searcher) throws IOException {

        Term term = new Term(field, word);
        PostingsEnum postingsEnum = MultiFields.getTermDocsEnum(reader, field, term.bytes());

        if (postingsEnum == null) {
            System.out.println("Cannot find the uri " + word + " in the field " + field);
            for (RecommendedTrack i : map.keySet())
                map.get(i).add(new DocTermStat(word, -1, -1));
            return;
        }

        Set<Integer> set = map.keySet().stream().map(RecommendedTrack::getLuceneId).collect(Collectors.toSet());

        while (postingsEnum.nextDoc() != PostingsEnum.NO_MORE_DOCS) {

            final int luceneId = postingsEnum.docID();

            if (!set.contains(luceneId)) continue;

            final long dl = Long.parseLong(searcher.doc(luceneId).get("playlist_length"));
            final int freq = postingsEnum.freq();

            map.keySet().stream().filter(recommendedTrack -> recommendedTrack.luceneId == luceneId).forEach(recommendedTrack -> map.get(recommendedTrack).add(new DocTermStat(word, dl, freq)));
        }
    }


    private static synchronized void export(int playlistID, List<RecommendedTrack> tracks, PrintWriter out) {
        tracks.forEach(track -> {
            out.print(playlistID);
            out.print(",");
            out.print(track.trackURI);
            out.print(",");
            out.print(track.searchResultFrequency);
            out.print(",");
            out.print(String.format("%.5f", track.maxScore));
            out.print(",");
            out.print(track.pos);
            out.print(",");
            //out.print(track.playlist_length);
            //out.print(",");
            if (track.album != null) {
                out.print(track.album);
                out.print(",");
            }
            if (track.artist != null) {
                out.print(track.artist);
                out.print(",");
            }
            if (track.track != null) {
                out.print(track.track);
                out.print(",");
            }
            out.print(track.searchResultAlbumFrequency);
            out.print(",");
            out.print(track.searchResultArtistFrequency);
            out.println();
        });
    }
}