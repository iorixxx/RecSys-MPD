package edu.anadolu;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.misc.HighFreqTerms;
import org.apache.lucene.misc.TermStats;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.*;
import org.apache.lucene.search.spans.SpanFirstQuery;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static edu.anadolu.Helper.clauses;
import static org.apache.lucene.misc.HighFreqTerms.getHighFreqTerms;


/**
 * Create submission
 */
public class Searcher implements Closeable {

    private static final String TEAM_INFO = "team_info,Anadolu,main,aarslan2@anadolu.edu.tr";

    private final LinkedHashMap<Integer, Integer> pageCount = new LinkedHashMap<>();

    private static final int RESULT_SIZE = 500;

    static final Pattern whiteSpaceSplitter = Pattern.compile("\\s+");
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

        this.reader = DirectoryReader.open(FSDirectory.open(indexPath));
        this.similarityConfig = similarityConfig;
        this.filler = filler;
        this.searcher = new IndexSearcher(reader);
        this.searcher.setSimilarity(this.similarityConfig.getSimilarity());
        this.useOnlyLonger = useOnlyLonger;

        try (BufferedReader reader = Files.newBufferedReader(challengePath)) {
            this.challenge = MPD.GSON.fromJson(reader, MPD.class);
        }

        for (int i = 1; i <= 100; i++)
            pageCount.put(i, 0);

        pageCount.put(-1, 0);


        ClassLoader classLoader = getClass().getClassLoader();

        try (InputStream resource = classLoader.getResourceAsStream("follower_frequency.txt")) {
            List<String> lines =
                    new BufferedReader(new InputStreamReader(resource,
                            StandardCharsets.UTF_8)).lines().collect(Collectors.toList());

            List<String> followerFreq = new ArrayList<>(lines.size());
            for (String line : lines) {
                followerFreq.add(whiteSpaceSplitter.split(line)[0]);
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

    private void fill(LinkedHashSet<String> submission) {
        if (Filler.Follower.equals(this.filler))
            fallBackToMostFollowedTracks(submission);
        else if (Filler.Blended.equals(this.filler))
            blended(submission);
        else if (Filler.Hybrid.equals(this.filler)) {
            if (++toggle % 2 == 0) {
                fallBackToMostFollowedTracks(submission);
            } else {
                fallBackToMostFreqTracks(submission);
            }
        } else if (Filler.Playlist.equals(this.filler))
            fallBackToMostFreqTracks(submission);
        else
            fallBackToMostFreqTracks(submission);

        if (submission.size() != RESULT_SIZE)
            throw new RuntimeException("after filler operation submission size is not equal to 500! size=" + submission.size());
    }

    private void blended(LinkedHashSet<String> submission) {

        Iterator<String> first = this.followerFreq.iterator();
        Iterator<TermStats> second = this.highFreqTrackURIs.iterator();

        int toggle = 0;
        while (first.hasNext() || second.hasNext()) {
            if (++toggle % 2 == 0) {
                submission.add(first.next());
            } else {
                submission.add(second.next().termtext.utf8ToString());
            }

            if (submission.size() == RESULT_SIZE) break;
        }

        if (submission.size() != RESULT_SIZE)
            throw new RuntimeException("after filler operation submission size is not equal to 500! size=" + submission.size());

    }


    /**
     * Filler alternative: fill remaining tracks using most followed tracks
     */
    private void fallBackToMostFollowedTracks(LinkedHashSet<String> submission) {

        for (final String track : this.followerFreq) {
            submission.add(track);
            if (submission.size() == RESULT_SIZE) break;
        }

        if (submission.size() != RESULT_SIZE)
            throw new RuntimeException("after filler operation submission size is not equal to 500! size=" + submission.size());

    }


    /**
     * If certain algorithm collects less than RESULT_SIZE tracks,
     * then fill remaining tracks using most frequent tracks as a last resort.
     */
    private void fallBackToMostFreqTracks(LinkedHashSet<String> submission) {

        for (final TermStats termStats : this.highFreqTrackURIs) {

            final String track = termStats.termtext.utf8ToString();

            submission.add(track);
            if (submission.size() == RESULT_SIZE) break;
        }

        if (submission.size() != RESULT_SIZE)
            throw new RuntimeException("after filler operation submission size is not equal to 500! size=" + submission.size());

    }

    public void search(Format format, Path resultPath) throws IOException, ParseException {

        PrintWriter out = new PrintWriter(Files.newBufferedWriter(resultPath, StandardCharsets.US_ASCII));
        out.println(TEAM_INFO);


        for (Playlist playlist : this.challenge.playlists) {

            LinkedHashSet<String> submission;
            if (playlist.tracks.length == 0) {
                submission = titleOnly(playlist.name.trim(), playlist.pid);
            } else {
                Track lastTrack = playlist.tracks[playlist.tracks.length - 1];

                if (lastTrack.pos == playlist.tracks.length - 1 && playlist.tracks[0].pos == 0) {
                    submission = firstNTracks(playlist.tracks, playlist.pid, SpanNearConfig.RelaxMode.Mode1);
                } else {
                    submission = tracksOnly(playlist.tracks, playlist.pid);
                }
            }

            if (submission.size() < RESULT_SIZE)
                fill(submission);

            if (submission.size() != RESULT_SIZE)
                throw new RuntimeException("we are about to persist the submission however submission size is not equal to 500! pid=" + playlist.pid + " size=" + submission.size());

            Helper.export(submission, playlist.pid, format, out, similarityConfig);

            out.flush();
            submission.clear();
        }

        out.flush();
        out.close();
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    /**
     * Predict tracks for a playlist given its title only
     */
    private LinkedHashSet<String> titleOnly(String title, int pId) throws ParseException, IOException {

        QueryParser queryParser = new QueryParser("name", Indexer.icu());
        queryParser.setDefaultOperator(QueryParser.Operator.AND);

        Query query = queryParser.parse(QueryParserBase.escape(title));


        LinkedHashSet<String> submission = new LinkedHashSet<>(RESULT_SIZE);

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
            System.out.println(submission.size() + " results found for title : " + title);

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
                .filter(o -> o.getValue() > 0)
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .forEach(o -> System.out.println(o.getKey() + "\t" + o.getValue()));
    }

    /**
     * Predict tracks for a playlist given its tracks only. Works best with <b>random</b> tracks category 8 and category 10
     */
    private LinkedHashSet<String> tracksOnly(Track[] tracks, int pId) throws IOException {

        LinkedHashSet<String> seeds = new LinkedHashSet<>(tracks.length);

        //TODO Helper.termQueryClauses(tracks, seeds);
        ArrayList<TermQuery> clauses = clauses(TermQuery.class, tracks, seeds);

        LinkedHashSet<String> submission = new LinkedHashSet<>(RESULT_SIZE);

        int minShouldMatch = seeds.size();

        while (submission.size() < RESULT_SIZE) {

            // halting criteria
            if (minShouldMatch == 0) break;

            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            builder.setMinimumNumberShouldMatch(--minShouldMatch);


            for (TermQuery tq : clauses)
                builder.add(tq, BooleanClause.Occur.SHOULD);

            BooleanQuery bq = builder.build();


            ScoreDoc[] hits = searcher.search(bq, Integer.MAX_VALUE).scoreDocs;

            // if (hits.length == 0) {
            //    System.out.println("tracksOnly found zero result found for pId : " + pId);
            //   return new LinkedHashSet<>();
            // }


            boolean finish = false;

            for (int i = 0; i < hits.length; i++) {
                int docId = hits[i].doc;
                Document doc = searcher.doc(docId);
                if (Integer.parseInt(doc.get("id")) == pId) continue;

                String trackURIs = doc.get("track_uris");

                String[] parts = whiteSpaceSplitter.split(trackURIs);

                if (useOnlyLonger && (parts.length <= seeds.size()))
                    continue;

                if (parts.length <= seeds.size())
                    System.out.println("**** document length " + parts.length + " is less than or equal to query length " + seeds.size());


                for (String t : parts) {

                    if (seeds.contains(t)) continue;

                    if (submission.size() < RESULT_SIZE) {
                        submission.add(t);
                    } else {
                        finish = true;
                        break;
                    }

                }

                incrementPageCountMap(i);

                if (finish) {
                    System.out.println("minShouldMath: " + minShouldMatch + "/" + seeds.size());
                    break;
                }

            }

        }

        seeds.clear();

        if (submission.size() != RESULT_SIZE)
            System.out.println("warning result set for " + pId + " size " + submission.size());

        return submission;
    }


    /**
     * Predict tracks for a playlist given its first N tracks, where N can equal 1, 5, 10, 25, or 100.
     */
    private LinkedHashSet<String> firstNTracks(Track[] tracks, int pId, SpanNearConfig.RelaxMode mode) throws IOException {

        LinkedHashSet<String> seeds = new LinkedHashSet<>(tracks.length);
        LinkedHashSet<String> submission = new LinkedHashSet<>(RESULT_SIZE);

        //TODO Helper.spanTermQueryClauses(tracks, seeds);
        ArrayList<SpanTermQuery> clauses = clauses(SpanTermQuery.class, tracks, seeds);


        final SpanQuery[] clausesIn = clauses.toArray(new SpanQuery[clauses.size()]);

        List<SpanNearConfig> configs = SpanNearConfig.RelaxMode.Mode1.equals(mode) ? SpanNearConfig.mode1 : SpanNearConfig.mode2;

        int j = 0;

        while (submission.size() < RESULT_SIZE) {

            // halting criteria
            if (j == configs.size()) break;
            SpanNearConfig config = configs.get(j++);

            //TODO try to figure out n from tracks.length

//            final int n;
//            if (tracks.length < 6)
//                n = tracks.length + 2; // for n=1 and n=5 use 2 and 7
//            else if (tracks.length < 26)
//                n = (int) (tracks.length * 1.5); // for n=10 and n=25 use 15 and 37
//            else
//                n = (int) (tracks.length * 1.25); // for n=100 use 125


            final SpanFirstQuery spanFirstQuery = new SpanFirstQuery(clausesIn.length == 1 ? clausesIn[0] : new SpanNearQuery(clausesIn, config.slop, config.inOrder), config.end);


            ScoreDoc[] hits = searcher.search(spanFirstQuery, Integer.MAX_VALUE).scoreDocs;

            boolean finish = false;

            for (int i = 0; i < hits.length; i++) {
                int docId = hits[i].doc;
                Document doc = searcher.doc(docId);
                if (Integer.parseInt(doc.get("id")) == pId) continue;

                String trackURIs = doc.get("track_uris");

                if (config.inOrder && 0 == config.end && 0 == config.slop) {
                    System.out.println("trackURIs " + trackURIs);
                    System.out.println("seeds " + seeds);
                    System.out.println("=============");
                }

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

                if (finish) {
                    System.out.println(config);
                    break;
                }

            }
        }


        /*
         * If SpanFirst & SpanNear strategy returns less than 500, use tracksOnly for filler purposes
         */
        if (submission.size() != RESULT_SIZE) {
            System.out.println("warning result set for " + pId + " size " + submission.size() + " try relaxing/increasing first=" + tracks.length);

            LinkedHashSet<String> backUp = tracksOnly(tracks, pId);
            for (final String track : backUp) {
                submission.add(track);
                if (submission.size() == RESULT_SIZE) break;
            }

            if (submission.size() != RESULT_SIZE) {
                System.out.println("warning after tracksOnly backup result set for " + pId + " size " + submission.size());
            }

        }

        seeds.clear();
        clauses.clear();
        return submission;
    }
}

