package edu.anadolu;

import com.google.gson.Gson;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.FlattenGraphFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.shingle.ShingleFilterFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.*;
import org.apache.lucene.search.spans.SpanFirstQuery;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.PriorityQueue;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static edu.anadolu.Helper.*;
import static edu.anadolu.Indexer.icu;
//import static edu.anadolu.SpanNearConfig.cacheKeys;


/**
 * Create submission
 */
public class Searcher implements Closeable {

    static final int RESULT_SIZE = 500;

    static final Pattern whiteSpace = Pattern.compile("\\s+");
    private final MPD challenge;
    private final IndexReader reader;
    private final IndexSearcher searcher;
    private final SimilarityConfig similarityConfig;

    private final boolean useOnlyLonger;

    private final boolean sortByFollower;

    private static final SortField SORT_FOLLOWERS = new SortField("num_followers", SortField.Type.INT, true);

    public Searcher(Path indexPath, Path challengePath, SimilarityConfig similarityConfig, boolean useOnlyLonger, boolean sortByFollower) throws IOException {
        if (!Files.exists(indexPath) || !Files.isDirectory(indexPath) || !Files.isReadable(indexPath)) {
            throw new IllegalArgumentException(indexPath + " does not exist or is not a directory.");
        }

        final Gson GSON = new Gson();
        this.reader = DirectoryReader.open(FSDirectory.open(indexPath));
        this.similarityConfig = similarityConfig;
        this.searcher = new IndexSearcher(reader);
        this.searcher.setSimilarity(this.similarityConfig.getSimilarity());
        this.useOnlyLonger = useOnlyLonger;
        this.sortByFollower = sortByFollower;

        try (BufferedReader reader = Files.newBufferedReader(challengePath)) {
            this.challenge = GSON.fromJson(reader, MPD.class);
        }
    }

    public void search(Path resultPath, SpanNearConfig.RelaxMode relaxMode) throws IOException {

        final AtomicReference<PrintWriter> out = new AtomicReference<>(new PrintWriter(Files.newBufferedWriter(resultPath, StandardCharsets.US_ASCII)));

        AtomicInteger titleOnly = new AtomicInteger(0);
        AtomicInteger firstN = new AtomicInteger(0);
        AtomicInteger first100 = new AtomicInteger(0);
        AtomicInteger random = new AtomicInteger(0);
        AtomicInteger first = new AtomicInteger(0);

        //SpanNearConfig.warmSpanCache(relaxMode);

        BooleanQuery.setMaxClauseCount(1612);
        Arrays.stream(this.challenge.playlists).parallel().forEach(playlist -> {

            final LinkedHashSet<String> submission;

            try {
                if (playlist.tracks.length == 0) {
                    titleOnly.incrementAndGet();
                    submission = titleOnly(playlist.name.trim(), playlist.pid, RESULT_SIZE);
                } else if (playlist.tracks.length == 1) {
                    first.incrementAndGet();
                    submission = firstTrack(playlist);
                } else {
                    Track lastTrack = playlist.tracks[playlist.tracks.length - 1];

                    if (lastTrack.pos == playlist.tracks.length - 1 && playlist.tracks[0].pos == 0) {

                        if (100 == playlist.tracks.length) {
                            first100.incrementAndGet();
                            submission = shingle(playlist, RESULT_SIZE);
                        } else {
                            firstN.incrementAndGet();
                            submission = spanFirst(playlist, relaxMode);
                        }

                    } else {
                        random.incrementAndGet();
                        submission = tracksOnly(playlist, RESULT_SIZE);
                    }
                }
            } catch (IOException | ParseException e) {
                throw new RuntimeException(e);
            }

            export(submission, playlist, out.get());

            out.get().flush();
            submission.clear();
        });

        out.get().flush();
        out.get().close();

        // Sanity check
        if (first.get() == 1000 && titleOnly.get() == 1000 && random.get() == 2000 && firstN.get() == 5000 && first100.get() == 1000)
            System.out.println("Number of entries into the Category Paths is OK!");
        else
            throw new RuntimeException("titleOnly:" + titleOnly + " random:" + random + " firstN:" + firstN);
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    private LinkedHashSet<String> titleAND(String title, int pId, int howMany) throws ParseException, IOException {

        QueryParser queryParser = new QueryParser("name", icu());
        queryParser.setDefaultOperator(QueryParser.Operator.AND);

        Query query = queryParser.parse(QueryParserBase.escape(title));

        LinkedHashSet<String> submission = new LinkedHashSet<>(howMany);

        ScoreDoc[] hits = searcher.search(query, Integer.MAX_VALUE).scoreDocs;

        for (ScoreDoc hit : hits) {
            int docId = hit.doc;
            Document doc = searcher.doc(docId);
            if (Integer.parseInt(doc.get("id")) == pId) continue;

            String trackURIs = doc.get("track_uris");
            List<String> list = Arrays.asList(whiteSpace.split(trackURIs));
            boolean finish = insertTrackURIs(submission, Collections.emptySet(), list, howMany);
            if (finish) break;
        }

        if (howMany == RESULT_SIZE && submission.size() != RESULT_SIZE)
            System.out.println("titleAND " + submission.size() + " results found for title : " + title);

        return submission;

    }

    private LinkedHashSet<String> titleOR(String title, int pId, int howMany) throws IOException {

        LinkedHashSet<String> submission = new LinkedHashSet<>(howMany);
        ArrayList<String> terms = Emoji.analyze(title, icu());

        List<TermQuery> clauses = terms.stream().map(t -> new Term("name", t)).map(TermQuery::new).collect(Collectors.toList());
        int minShouldMatch = clauses.size();

        while (submission.size() < howMany) {

            // halting criteria
            if (minShouldMatch == 0) break;

            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            builder.setMinimumNumberShouldMatch(minShouldMatch--);


            for (TermQuery tq : clauses)
                builder.add(tq, BooleanClause.Occur.SHOULD);

            BooleanQuery bq = builder.build();

            final ScoreDoc[] hits;

            //when there is single term to match i.e., minShouldMatch=1 sort by follower frequency
            if (sortByFollower && bq.getMinimumNumberShouldMatch() == 1)
                hits = searcher.search(bq, Integer.MAX_VALUE, new Sort(SORT_FOLLOWERS, SortField.FIELD_SCORE)).scoreDocs;
            else
                hits = searcher.search(bq, Integer.MAX_VALUE, new Sort(SortField.FIELD_SCORE, SORT_FOLLOWERS)).scoreDocs;

            for (ScoreDoc hit : hits) {
                int docId = hit.doc;
                Document doc = searcher.doc(docId);
                if (Integer.parseInt(doc.get("id")) == pId) continue;

                if (bq.getMinimumNumberShouldMatch() == 1) {
                    System.out.println(doc.get("id") + "\t" + doc.get("num_followers") + "\t" + minShouldMatch);
                }

                String trackURIs = doc.get("track_uris");
                List<String> list = Arrays.asList(whiteSpace.split(trackURIs));
                boolean finish = insertTrackURIs(submission, Collections.emptySet(), list, howMany);

                if (finish) {
                    System.out.println("minShouldMatch: " + (bq.getMinimumNumberShouldMatch()) + "/" + clauses.size());
                    break;
                }
            }
        }


        if (howMany == RESULT_SIZE && submission.size() != RESULT_SIZE)
            System.out.println("titleOR " + submission.size() + " for title " + title);

        return submission;
    }

    /**
     * Predict tracks for a playlist given its title only
     */
    private LinkedHashSet<String> titleOnly(String title, int pId, int howMany) throws ParseException, IOException {

        LinkedHashSet<String> submission = titleAND(title, pId, howMany);

        if (submission.size() == howMany) return submission;

        final int queryLength = Emoji.analyze(title, icu()).size();

        final String newTitle;
        if (queryLength == 1 && title.contains("_")) {
            newTitle = title.replaceAll("_", " ").trim();
        } else newTitle = title.trim();

        LinkedHashSet<String> backUp = titleOR(newTitle, pId, howMany * 2);
        boolean done = insertTrackURIs(submission, Collections.emptySet(), backUp, howMany);

        if (!done) {
            System.out.println("backup " + submission.size() + " for title " + newTitle);
        }
        return submission;
    }

    /**
     * Predict tracks for a playlist given its tracks only. Works best with <b>random</b> tracks category 8 and category 10
     */
    private LinkedHashSet<String> tracksOnly(Playlist playlist, int howMany) throws IOException {

        final Track[] tracks = playlist.tracks;

        LinkedHashSet<String> seeds = new LinkedHashSet<>(tracks.length);
        LinkedHashSet<String> submission = new LinkedHashSet<>(howMany);
        ArrayList<TermQuery> clauses = clauses(TermQuery.class, tracks, seeds);

        if (seeds.size() != clauses.size())
            throw new RuntimeException("seeds.size and clauses.size do not match! " + seeds.size() + " " + clauses.size());

        int minShouldMatch = seeds.size();

        while (submission.size() < howMany) {

            // halting criteria
            if (minShouldMatch == 0) break;

            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            builder.setMinimumNumberShouldMatch(minShouldMatch--);


            for (TermQuery tq : clauses)
                builder.add(tq, BooleanClause.Occur.SHOULD);

            BooleanQuery bq = builder.build();


            ScoreDoc[] hits =
                    sortByFollower ?
                            searcher.search(bq, Integer.MAX_VALUE, new Sort(SORT_FOLLOWERS, SortField.FIELD_SCORE)).scoreDocs
                            : searcher.search(bq, Integer.MAX_VALUE, new Sort(SortField.FIELD_SCORE, SORT_FOLLOWERS)).scoreDocs;


            for (ScoreDoc hit : hits) {
                int docId = hit.doc;
                Document doc = searcher.doc(docId);
                if (Integer.parseInt(doc.get("id")) == playlist.pid) continue;

                String trackURIs = doc.get("track_uris");
                String[] parts = whiteSpace.split(trackURIs);

                if (useOnlyLonger && (parts.length <= seeds.size()))
                    continue;

                if (parts.length <= seeds.size())
                    System.out.println("**** document length " + parts.length + " is less than or equal to query length " + seeds.size());

                List<String> list = Arrays.asList(parts);
                boolean finish = insertTrackURIs(submission, seeds, list, howMany);

                if (finish) {
                    System.out.println("minShouldMatch: " + (bq.getMinimumNumberShouldMatch()) + "/" + seeds.size());
                    break;
                }
            }
        }

        seeds.clear();

        if (howMany == RESULT_SIZE && submission.size() != RESULT_SIZE)
            System.out.println("warning result set size " + submission.size() + " for tracks " + tracks.length);

        return submission;
    }


    /**
     * Predict tracks for a playlist given its first N tracks, where N can equal 5, 10, 25, or 100.
     */
    private LinkedHashSet<String> spanFirst(Playlist playlist, SpanNearConfig.RelaxMode mode) throws IOException {

        final Track[] tracks = playlist.tracks;

        LinkedHashSet<String> seeds = new LinkedHashSet<>(tracks.length);
        LinkedHashSet<String> submission = new LinkedHashSet<>(RESULT_SIZE);
        ArrayList<SpanTermQuery> clauses = clauses(tracks, seeds);
        final SpanTermQuery[] clausesIn = clauses.toArray(new SpanTermQuery[clauses.size()]);

        if (seeds.size() != clauses.size())
            throw new RuntimeException("seeds.size and clauses.size do not match! " + seeds.size() + " " + clauses.size());


        final List<SpanNearConfig> configs = SpanNearConfig.configs(mode, clauses.size());

        int j = 0;

        while (submission.size() < RESULT_SIZE) {

            // halting criteria
            if (j == configs.size()) {
                break;
            }
            SpanNearConfig config = configs.get(j++);

            final SpanFirstQuery spanFirstQuery = new SpanFirstQuery(new SpanNearQuery(clausesIn, config.slop, config.inOrder), config.end);

            ScoreDoc[] hits = searcher.search(spanFirstQuery, Integer.MAX_VALUE).scoreDocs;

            for (ScoreDoc hit : hits) {
                int docId = hit.doc;
                Document doc = searcher.doc(docId);
                if (Integer.parseInt(doc.get("id")) == playlist.pid) continue;

                String trackURIs = doc.get("track_uris");

                if (config.inOrder && config.slop == 0 && config.end == clauses.size()) {
                    System.out.println("============ " + config + " for tracks " + clauses.size());
                    System.out.println("trackURIs " + trackURIs);
                    System.out.println("seeds " + seeds);
                }

                List<String> list = Arrays.asList(whiteSpace.split(trackURIs));
                boolean finish = insertTrackURIs(submission, seeds, list, RESULT_SIZE);

                if (finish) {
                    System.out.println("progress: " + (j - 1) + "/" + configs.size() + "\t" + config + " for tracks " + clausesIn.length);
                    break;
                }

            }
        }


        /*
         * If SpanFirst strategy returns less than 500, use tracksOnly for filler purposes
         */
        if (submission.size() != RESULT_SIZE) {
            System.out.println("SpanFirst strategy returns " + submission.size() + " for tracks " + clauses.size());

            LinkedHashSet<String> backUp = shingle(playlist, RESULT_SIZE * 2);
            boolean done = insertTrackURIs(submission, seeds, backUp, RESULT_SIZE);

            if (!done) {
                System.out.println("warning after tracksOnly backup result set size " + submission.size() + " for tracks " + tracks.length);

                backUp = tracksOnly(playlist, RESULT_SIZE * 2);
                insertTrackURIs(submission, seeds, backUp, RESULT_SIZE);
            }

        }

        seeds.clear();
        clauses.clear();
        return submission;
    }

    /**
     * Predict tracks for a playlist given its title and the first track
     */
    private LinkedHashSet<String> firstTrack(Playlist playlist) throws IOException {

        final Track[] tracks = playlist.tracks;

        if (tracks.length != 1)
            throw new RuntimeException("tracks length is not 1!");

        LinkedHashSet<String> seeds = new LinkedHashSet<>(tracks.length);
        LinkedHashSet<String> submission = new LinkedHashSet<>(RESULT_SIZE);
        SpanTermQuery spanTermQuery = new SpanTermQuery(new Term("track_uris", tracks[0].track_uri));
        seeds.add(tracks[0].track_uri);

        if (seeds.size() != 1)
            throw new RuntimeException("seeds.size is not 1!");

        int end = 1;

        while (submission.size() < RESULT_SIZE) {

            // halting criteria
            if (end == 50) {
                break;
            }

            final SpanFirstQuery spanFirstQuery = new SpanFirstQuery(spanTermQuery, end++);
            ScoreDoc[] hits = searcher.search(spanFirstQuery, Integer.MAX_VALUE).scoreDocs;


            for (ScoreDoc hit : hits) {
                int docId = hit.doc;
                Document doc = searcher.doc(docId);
                if (Integer.parseInt(doc.get("id")) == playlist.pid) continue;

                String trackURIs = doc.get("track_uris");

                List<String> list = Arrays.asList(whiteSpace.split(trackURIs));
                boolean finish = insertTrackURIs(submission, seeds, list, RESULT_SIZE);

                if (finish) {
                    System.out.println("progress: " + (end - 1) + "/" + 50 + " for tracks 1");
                    break;
                }

            }
        }


        /*
         * If SpanFirst strategy returns less than 500, use tracksOnly for filler purposes
         */
        if (submission.size() != RESULT_SIZE) {
            System.out.println("SpanFirst strategy returns " + submission.size() + " for tracks 1");

            LinkedHashSet<String> backUp = tracksOnly(playlist, RESULT_SIZE * 2);
            boolean done = insertTrackURIs(submission, seeds, backUp, RESULT_SIZE);

            if (!done) {
                System.out.println("warning after tracksOnly backup result set size " + submission.size() + " for tracks 1");
            }
        }

        seeds.clear();
        return submission;
    }

    /**
     * longest common prefix : http://richardstartin.uk/new-methods-in-java-9-math-fma-and-arrays-mismatch/
     */
    private LinkedHashSet<String> longestCommonPrefix(Playlist playlist, int howMany) throws IOException {

        final Track[] tracks = playlist.tracks;

        if (tracks.length < 1)
            throw new RuntimeException("tracks length is less than 1! " + tracks.length);

        LinkedHashSet<String> seeds = new LinkedHashSet<>(tracks.length);
        Arrays.stream(playlist.tracks).map(track -> track.track_uri).forEach(seeds::add);
        LinkedHashSet<String> submission = new LinkedHashSet<>(howMany);

        String[] seedArray = seeds.toArray(new String[seeds.size()]);

        SpanTermQuery spanTermQuery = new SpanTermQuery(new Term("track_uris", tracks[0].track_uri));

        if (!seedArray[0].equals(tracks[0].track_uri))
            throw new RuntimeException("seedArray[0] and  tracks[0].track_uri) does not match!");

        final SpanFirstQuery spanFirstQuery = new SpanFirstQuery(spanTermQuery, 1);
        ScoreDoc[] hits = searcher.search(spanFirstQuery, Integer.MAX_VALUE).scoreDocs;

        PriorityQueue<StringIntPair> priorityQueue = new PriorityQueue<>(howMany) {
            @Override
            protected boolean lessThan(StringIntPair a, StringIntPair b) {
                return Integer.compare(a.integer, b.integer) < 0;
            }
        };

        for (ScoreDoc hit : hits) {
            int docId = hit.doc;
            Document doc = searcher.doc(docId);
            if (Integer.parseInt(doc.get("id")) == playlist.pid) continue;

            String trackURIs = doc.get("track_uris");

            String[] parts = whiteSpace.split(trackURIs);

            int index = Arrays.mismatch(parts, seedArray);

            if (index == 0) throw new RuntimeException("can't happen Arrays.mismatch SpanFirst must guarantee 1!");

            if (index == -1) index = Integer.MAX_VALUE;

            priorityQueue.insertWithOverflow(new StringIntPair(trackURIs, index));

        }

        List<StringIntPair> reverse = Helper.reverse(priorityQueue);

        for (StringIntPair pair : reverse) {
            //System.out.println(pair.integer + "\t" + pair.string);
            boolean done = insertTrackURIs(submission, seeds, Arrays.asList(whiteSpace.split(pair.string)), howMany);
            if (done) {
                break;
            }
        }

        /*
         * If longest common prefix (LCP) strategy returns less than 500, use tracksOnly for filler purposes
         */
        if (submission.size() != howMany) {
            System.out.println("LCP returns " + submission.size() + " for tracks " + playlist.tracks.length);

            LinkedHashSet<String> backUp = tracksOnly(playlist, howMany * 2);
            boolean done = insertTrackURIs(submission, seeds, backUp, howMany);

            if (!done) {
                System.out.println("LCP warning after tracksOnly backup size " + submission.size());
            }
        }

        priorityQueue.clear();
        seeds.clear();
        reverse.clear();
        return submission;
    }

    static Analyzer shingleQuery() throws IOException {
        return CustomAnalyzer.builder()
                .withTokenizer("whitespace")
                .addTokenFilter(ShingleFilterFactory.class,
                        "minShingleSize", "2",
                        "maxShingleSize", "15",
                        "outputUnigrams", "false",
                        "outputUnigramsIfNoShingles", "false")
                .addTokenFilter(FlattenGraphFilterFactory.class)
                .build();
    }

    /**
     * Predict tracks for a playlist given its first N tracks. Works best with <b>N=100</b> tracks category 9
     */
    private LinkedHashSet<String> shingle(Playlist playlist, int howMany) throws IOException {

        final Track[] tracks = playlist.tracks;

        LinkedHashSet<String> seeds = new LinkedHashSet<>(tracks.length);
        LinkedHashSet<String> submission = new LinkedHashSet<>(howMany);

        QueryParser queryParser = new QueryParser(ShingleFilter.DEFAULT_TOKEN_TYPE, shingleQuery());
        queryParser.setDefaultOperator(QueryParser.Operator.OR);
        queryParser.setSplitOnWhitespace(false);
        queryParser.setAutoGeneratePhraseQueries(false);
        queryParser.setEnableGraphQueries(false);

        StringBuilder builder = new StringBuilder();
        for (Track track : tracks) {
            // skip duplicate tracks in the playlist. Only consider the first occurrence of the track.
            if (seeds.contains(track.track_uri)) continue;

            seeds.add(track.track_uri);
            builder.append(track.track_uri).append(' ');
        }

        Query query;

        try {
            query = queryParser.parse(QueryParserBase.escape(builder.toString().trim()));
        } catch (ParseException p) {
            throw new RuntimeException(seeds.size() + " " + playlist.tracks.length + " " + BooleanQuery.getMaxClauseCount());
        }

        ScoreDoc[] hits = searcher.search(query, Integer.MAX_VALUE).scoreDocs;

        for (ScoreDoc hit : hits) {
            int docId = hit.doc;
            Document doc = searcher.doc(docId);
            if (Integer.parseInt(doc.get("id")) == playlist.pid) continue;

            String trackURIs = doc.get("track_uris");
            List<String> list = Arrays.asList(whiteSpace.split(trackURIs));
            boolean finish = insertTrackURIs(submission, seeds, list, howMany);
            if (finish) break;
        }

        /*
         * If longest common prefix (LCP) strategy returns less than 500, use tracksOnly for filler purposes
         */
        if (submission.size() != howMany) {
            System.out.println("shingle returns " + submission.size() + " for tracks " + playlist.tracks.length);

            LinkedHashSet<String> backUp = tracksOnly(playlist, howMany * 2);
            boolean done = insertTrackURIs(submission, seeds, backUp, howMany);

            if (!done) {
                System.out.println("shingle warning after tracksOnly backup size " + submission.size());
            }
        }

        seeds.clear();
        return submission;

    }

    /**
     * Predict tracks for a playlist given its first N tracks. Works best with <b>N=100</b> tracks category 9
     */
    private LinkedHashSet<String> shinglePureOR(Playlist playlist, int howMany) throws IOException {

        final Track[] tracks = playlist.tracks;

        LinkedHashSet<String> seeds = new LinkedHashSet<>(tracks.length);
        LinkedHashSet<String> submission = new LinkedHashSet<>(howMany);


        StringBuilder stringBuilder = new StringBuilder();
        for (Track track : tracks) {
            // skip duplicate tracks in the playlist. Only consider the first occurrence of the track.
            if (seeds.contains(track.track_uri)) continue;

            seeds.add(track.track_uri);
            stringBuilder.append(track.track_uri).append(' ');
        }

        List<String> terms = Emoji.analyze(stringBuilder.toString().trim(), shingleQuery());

        List<TermQuery> clauses = terms.stream().map(t -> new Term(ShingleFilter.DEFAULT_TOKEN_TYPE, t)).map(TermQuery::new).collect(Collectors.toList());

        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        for (TermQuery tq : clauses)
            builder.add(tq, BooleanClause.Occur.SHOULD);

        BooleanQuery bq = builder.build();

        ScoreDoc[] hits = searcher.search(bq, Integer.MAX_VALUE).scoreDocs;

        for (ScoreDoc hit : hits) {
            int docId = hit.doc;
            Document doc = searcher.doc(docId);
            if (Integer.parseInt(doc.get("id")) == playlist.pid) continue;

            String trackURIs = doc.get("track_uris");
            List<String> list = Arrays.asList(whiteSpace.split(trackURIs));
            boolean finish = insertTrackURIs(submission, seeds, list, howMany);
            if (finish) break;
        }



        /*
         * If longest common prefix (LCP) strategy returns less than 500, use tracksOnly for filler purposes
         */
        if (submission.size() != howMany) {
            System.out.println("ORshingle returns " + submission.size() + " for tracks " + playlist.tracks.length);

            LinkedHashSet<String> backUp = tracksOnly(playlist, howMany * 2);
            boolean done = insertTrackURIs(submission, seeds, backUp, howMany);

            if (!done) {
                System.out.println("shingle warning after tracksOnly backup size " + submission.size());
            }
        }

        seeds.clear();
        return submission;

    }
}

