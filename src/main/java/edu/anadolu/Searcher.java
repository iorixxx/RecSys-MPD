package edu.anadolu;

import com.google.gson.Gson;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.misc.HighFreqTerms;
import org.apache.lucene.misc.TermStats;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.*;
import org.apache.lucene.search.spans.SpanFirstQuery;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static edu.anadolu.Helper.*;
import static edu.anadolu.SpanNearConfig.cacheKeys;
import static org.apache.lucene.misc.HighFreqTerms.getHighFreqTerms;


/**
 * Create submission
 */
public class Searcher implements Closeable {

    private static final String TEAM_INFO = "team_info,Anadolu,main,aarslan2@anadolu.edu.tr";

//    private final LinkedHashMap<Integer, Integer> pageCount = new LinkedHashMap<>();

    static final int RESULT_SIZE = 500;

    static final Pattern whiteSpace = Pattern.compile("\\s+");
    private final MPD challenge;
    private final IndexReader reader;
    private final IndexSearcher searcher;
    private final SimilarityConfig similarityConfig;

    private final SortedSet<TermStats> highFreqTrackURIs;
    private final Filler filler;
    private final List<String> followerFreq;

    private final boolean useOnlyLonger;

    public Searcher(Path indexPath, Path challengePath, SimilarityConfig similarityConfig, Filler filler, boolean useOnlyLonger) throws Exception {
        if (!Files.exists(indexPath) || !Files.isDirectory(indexPath) || !Files.isReadable(indexPath)) {
            throw new IllegalArgumentException(indexPath + " does not exist or is not a directory.");
        }

        final Gson GSON = new Gson();
        this.reader = DirectoryReader.open(FSDirectory.open(indexPath));
        this.similarityConfig = similarityConfig;
        this.filler = filler;
        this.searcher = new IndexSearcher(reader);
        this.searcher.setSimilarity(this.similarityConfig.getSimilarity());
        this.useOnlyLonger = useOnlyLonger;

        try (BufferedReader reader = Files.newBufferedReader(challengePath)) {
            this.challenge = GSON.fromJson(reader, MPD.class);
        }
//
//        for (int i = 1; i <= 100; i++)
//            pageCount.put(i, 0);
//
//        pageCount.put(-1, 0);


        ClassLoader classLoader = getClass().getClassLoader();

        try (InputStream resource = classLoader.getResourceAsStream("follower_frequency.txt")) {
            List<String> lines =
                    new BufferedReader(new InputStreamReader(resource,
                            StandardCharsets.UTF_8)).lines().collect(Collectors.toList());

            List<String> followerFreq = new ArrayList<>(lines.size());
            for (String line : lines) {
                followerFreq.add(whiteSpace.split(line)[0]);
            }

            this.followerFreq = Collections.unmodifiableList(followerFreq);
            lines.clear();
        }

        Comparator<TermStats> comparator = new HighFreqTerms.DocFreqComparator();

        TermStats[] terms = getHighFreqTerms(reader, RESULT_SIZE * 2, "track_uris", comparator);

        System.out.println("Top-10 Track URIs sorted by playlist frequency");
        System.out.println("term \t totalTF \t docFreq");

        SortedSet<TermStats> highFreqTrackURIs = new TreeSet<>(Collections.reverseOrder(comparator));
        int i = 0;
        for (TermStats termStats : terms) {
            if (i++ < 10)
                System.out.printf(Locale.ROOT, "%s \t %d \t %d \n",
                        termStats.termtext.utf8ToString(), termStats.totalTermFreq, termStats.docFreq);
            highFreqTrackURIs.add(termStats);
        }
        this.highFreqTrackURIs = Collections.unmodifiableSortedSet(highFreqTrackURIs);

        i = 0;
        for (final TermStats termStats : this.highFreqTrackURIs) {
            if (!terms[i].equals(termStats))
                throw new RuntimeException(termStats.termtext.utf8ToString());
            if (!terms[i].termtext.utf8ToString().equals(termStats.termtext.utf8ToString()))
                throw new RuntimeException(termStats.termtext.utf8ToString());
            if (termStats.totalTermFreq != termStats.docFreq)
                throw new RuntimeException(termStats.termtext.utf8ToString());
            if (terms[i].totalTermFreq != terms[i].docFreq)
                throw new RuntimeException(termStats.termtext.utf8ToString());
            if (terms[i].docFreq != termStats.docFreq)
                throw new RuntimeException(termStats.termtext.utf8ToString());
            i++;
        }

    }

    private int toggle = 0;

    private void fill(LinkedHashSet<String> submission, Set<String> seeds) {
        if (Filler.Follower.equals(this.filler))
            fallBackToMostFollowedTracks(submission, seeds, this.followerFreq);
        else if (Filler.Blended.equals(this.filler))
            blended(submission, seeds, this.highFreqTrackURIs, this.followerFreq);
        else if (Filler.Hybrid.equals(this.filler)) {
            if (++toggle % 2 == 0) {
                fallBackToMostFollowedTracks(submission, seeds, this.followerFreq);
            } else {
                fallBackToMostFreqTracks(submission, seeds, this.highFreqTrackURIs);
            }
        } else if (Filler.Playlist.equals(this.filler))
            fallBackToMostFreqTracks(submission, seeds, this.highFreqTrackURIs);
        else
            fallBackToMostFreqTracks(submission, seeds, this.highFreqTrackURIs);

        if (submission.size() != RESULT_SIZE)
            throw new RuntimeException("after filler operation submission size is not equal to 500! size=" + submission.size());
    }


    public void search(Format format, Path resultPath, SpanNearConfig.RelaxMode relaxMode) throws IOException, ParseException {

        PrintWriter out = new PrintWriter(Files.newBufferedWriter(resultPath, StandardCharsets.US_ASCII));
        out.println(TEAM_INFO);

        int titleOnly = 0;
        int firstN = 0;
        int random = 0;
        int first = 0;

        for (Playlist playlist : this.challenge.playlists) {

            final LinkedHashSet<String> submission;
            if (playlist.tracks.length == 0) {
                titleOnly++;
                submission = titleOnly(playlist.name.trim(), playlist.pid, RESULT_SIZE);
            } else if (playlist.tracks.length == 1) {
                first++;
                submission = firstTrack(playlist);
            } else {
                Track lastTrack = playlist.tracks[playlist.tracks.length - 1];

                if (lastTrack.pos == playlist.tracks.length - 1 && playlist.tracks[0].pos == 0) {
                    firstN++;
                    submission = spanFirst(playlist, relaxMode);
                } else {
                    random++;
                    submission = tracksOnly(playlist, RESULT_SIZE);
                }
            }

            if (submission.size() < RESULT_SIZE) {
                Set<String> seeds = playlist.tracks.length == 0 ? Collections.emptySet() : Arrays.stream(playlist.tracks).map(track -> track.track_uri).collect(Collectors.toSet());
                fill(submission, seeds);
                seeds.clear();
            }

            if (submission.size() != RESULT_SIZE)
                throw new RuntimeException("we are about to persist the submission however submission size is not equal to 500! pid=" + playlist.pid + " size=" + submission.size());

            export(submission, playlist.pid, format, out, similarityConfig);

            out.flush();
            submission.clear();
        }

        out.flush();
        out.close();

        // Sanity check
        if (first == 1000 && titleOnly == 1000 && random == 2000 && firstN == 6000)
            System.out.println("Number of entries into the Category Paths is OK!");
        else
            throw new RuntimeException("titleOnly:" + titleOnly + " random:" + random + " firstN:" + firstN);

        System.out.println("cacheKeys: " + cacheKeys());
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    /**
     * Predict tracks for a playlist given its title only
     */
    private LinkedHashSet<String> titleOnly(String title, int pId, int howMany) throws ParseException, IOException {

        QueryParser queryParser = new QueryParser("name", Indexer.icu());
        queryParser.setDefaultOperator(QueryParser.Operator.AND);

        Query query = queryParser.parse(QueryParserBase.escape(title));

        LinkedHashSet<String> submission = new LinkedHashSet<>(howMany);

        ScoreDoc[] hits = searcher.search(query, Integer.MAX_VALUE).scoreDocs;

        /*
         * Try with OR operator, relaxed mode.
         */
        if (hits.length == 0) {

            final int queryLength = Emoji.analyze(title);

            // one term query: fuzzy match
            if (queryLength == 1 && !title.contains("_")) {

                System.out.println("==oneTermQuery : " + title);

            } else {

                queryParser = new QueryParser("name", Indexer.icu());
                queryParser.setDefaultOperator(QueryParser.Operator.OR);

                if (queryLength == 1 && title.contains("_")) {
                    query = queryParser.parse(QueryParserBase.escape(title.replaceAll("_", " ")));
                } else {
                    query = queryParser.parse(QueryParserBase.escape(title));
                }


                hits = searcher.search(query, Integer.MAX_VALUE).scoreDocs;

                if (hits.length == 0) {
                    System.out.println("Zero result found for title : " + title);
                    return new LinkedHashSet<>();
                }
            }

        }


        for (ScoreDoc hit : hits) {
            int docId = hit.doc;
            Document doc = searcher.doc(docId);

            // if given pId found in the index, skip it: to use 1M index for all kind of tasks (original test set, or the one we created

            if (Integer.parseInt(doc.get("id")) == pId) continue;

            String trackURIs = doc.get("track_uris");
            List<String> list = Arrays.asList(whiteSpace.split(trackURIs));
            boolean finish = insertTrackURIs(submission, Collections.emptySet(), list, RESULT_SIZE);
            if (finish) break;
        }


        if (howMany == RESULT_SIZE && submission.size() != RESULT_SIZE)
            System.out.println(submission.size() + " results found for title : " + title);

        return submission;
    }

    /**
     * Predict tracks for a playlist given its tracks only. Works best with <b>random</b> tracks category 8 and category 10
     */
    private LinkedHashSet<String> tracksOnly(Playlist playlist, int howMany) throws IOException {

        final Track[] tracks = playlist.tracks;
        final int pId = playlist.pid;

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


            ScoreDoc[] hits = searcher.search(bq, Integer.MAX_VALUE).scoreDocs;


            for (ScoreDoc hit : hits) {
                int docId = hit.doc;
                Document doc = searcher.doc(docId);
                if (Integer.parseInt(doc.get("id")) == pId) continue;

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
            System.out.println("warning result set for " + pId + " size " + submission.size() + " for tracks " + tracks.length);

        return submission;
    }


    /**
     * Predict tracks for a playlist given its first N tracks, where N can equal 5, 10, 25, or 100.
     */
    private LinkedHashSet<String> spanFirst(Playlist playlist, SpanNearConfig.RelaxMode mode) throws IOException {

        final Track[] tracks = playlist.tracks;
        final int pId = playlist.pid;

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
                System.out.println("halting j " + j);
                break;
            }
            SpanNearConfig config = configs.get(j++);

            if (config.tightest(clauses.size()))
                System.out.println("tightest pid: " + pId + " for tracks " + tracks.length);

            final SpanFirstQuery spanFirstQuery = new SpanFirstQuery(new SpanNearQuery(clausesIn, config.slop, config.inOrder), config.end);

            ScoreDoc[] hits = searcher.search(spanFirstQuery, Integer.MAX_VALUE).scoreDocs;

            for (ScoreDoc hit : hits) {
                int docId = hit.doc;
                Document doc = searcher.doc(docId);
                if (Integer.parseInt(doc.get("id")) == pId) continue;

                String trackURIs = doc.get("track_uris");

                if (config.inOrder && config.slop == 0 && config.end == clauses.size()) {
                    System.out.println("============ " + config + " for tracks " + clauses.size());
                    System.out.println("trackURIs " + trackURIs);
                    System.out.println("seeds " + seeds);
                }

                if (config.slop == config.end) {
                    System.out.println("------------ " + config + " for tracks " + clauses.size());
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

            LinkedHashSet<String> backUp = tracksOnly(playlist, RESULT_SIZE * 2);
            boolean done = insertTrackURIs(submission, seeds, backUp, RESULT_SIZE);

            if (!done) {
                System.out.println("warning after tracksOnly backup result set for " + pId + " size " + submission.size() + " for tracks " + tracks.length);
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
        final int pId = playlist.pid;

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
            if (end == 66) {
                break;
            }

            final SpanFirstQuery spanFirstQuery = new SpanFirstQuery(spanTermQuery, end++);
            ScoreDoc[] hits = searcher.search(spanFirstQuery, Integer.MAX_VALUE).scoreDocs;


            for (ScoreDoc hit : hits) {
                int docId = hit.doc;
                Document doc = searcher.doc(docId);
                if (Integer.parseInt(doc.get("id")) == pId) continue;

                String trackURIs = doc.get("track_uris");

                List<String> list = Arrays.asList(whiteSpace.split(trackURIs));
                boolean finish = insertTrackURIs(submission, seeds, list, RESULT_SIZE);

                if (finish) {
                    System.out.println("progress: " + (end - 1) + "/" + 66 + " for tracks 1");
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
                System.out.println("warning after tracksOnly backup result set for " + pId + " size " + submission.size() + " for tracks 1");
            }
        }

        seeds.clear();
        return submission;
    }
}

