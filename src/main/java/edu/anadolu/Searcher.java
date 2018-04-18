package edu.anadolu;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.misc.HighFreqTerms;
import org.apache.lucene.misc.TermStats;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.lucene.misc.HighFreqTerms.getHighFreqTerms;


/**
 * Create submission
 */
public class Searcher implements Closeable {

    private static final String TEAM_INFO = "team_info,main,Anadolu Team,aarslan2@anadolu.edu.tr";

    private final LinkedHashMap<Integer, Integer> pageCount = new LinkedHashMap<>();

    private static final int RESULT_SIZE = 500;

    private static final Pattern whiteSpaceSplitter = Pattern.compile("\\s+");
    private final MPD challenge;
    private final IndexReader reader;
    private final IndexSearcher searcher;
    private final SimilarityConfig similarityConfig;

    private final SortedSet<TermStats> highFreqTrackURIs;
    private final Filler filler;
    private final List<String> followerFreq;

    public Searcher(Path indexPath, Path challengePath, SimilarityConfig similarityConfig, Filler filler) throws Exception {
        if (!Files.exists(indexPath) || !Files.isDirectory(indexPath) || !Files.isReadable(indexPath)) {
            throw new IllegalArgumentException(indexPath + " does not exist or is not a directory.");
        }

        this.reader = DirectoryReader.open(FSDirectory.open(indexPath));
        this.similarityConfig = similarityConfig;
        this.filler = filler;
        this.searcher = new IndexSearcher(reader);
        this.searcher.setSimilarity(this.similarityConfig.getSimilarity());

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

    private void fill(LinkedHashSet<String> submission) {
        if (Filler.Follower.equals(this.filler))
            fallBackToMostFollowedTracks(submission);
        else if (Filler.Blended.equals(this.filler))
            blended(submission);
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
            ++toggle;

            if (toggle % 2 == 0) {
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
                submission = titleOnly(playlist.name, playlist.pid);
            } else {
                Track lastTrack = playlist.tracks[playlist.tracks.length - 1];

                if (lastTrack.pos == playlist.tracks.length - 1 && playlist.tracks[0].pos == 0) {
                    submission = firstNTracks(playlist.tracks, playlist.pid, SpanType.SpanNear);
                } else {
                    submission = tracksOnly(playlist.tracks, playlist.pid);
                }
            }

            if (submission.size() < RESULT_SIZE)
                fill(submission);

            if (submission.size() != RESULT_SIZE)
                throw new RuntimeException("we are about to persist the submission however submission size is not equal to 500! pid=" + playlist.pid + " size=" + submission.size());

            export(submission, playlist.pid, format, out);

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

            queryParser = new QueryParser("name", Indexer.icu());
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
     * Predict tracks for a playlist given its tracks only. Works best with random tracks category 8 and category 10
     */
    private LinkedHashSet<String> tracksOnly(Track[] tracks, int pId) throws ParseException, IOException {

        QueryParser queryParser = new QueryParser("track_uris", new WhitespaceAnalyzer());
        queryParser.setDefaultOperator(QueryParser.Operator.OR);

        HashSet<String> seeds = new HashSet<>(100);

        StringBuilder builder = new StringBuilder();
        for (Track track : tracks) {
            // skip duplicate tracks in the playlist. Only consider the first occurrence of the track.
            if (seeds.contains(track.track_uri)) continue;
            builder.append(track.track_uri).append(' ');
            seeds.add(track.track_uri);
        }
        Query query = queryParser.parse(QueryParserBase.escape(builder.toString().trim()));


        ScoreDoc[] hits = searcher.search(query, Integer.MAX_VALUE).scoreDocs;

        if (hits.length == 0) {
            System.out.println("tracksOnly found zero result found for pId : " + pId);
            return new LinkedHashSet<>();
        }

        LinkedHashSet<String> submission = new LinkedHashSet<>(RESULT_SIZE);

        boolean finish = false;

        for (int i = 0; i < hits.length; i++) {
            int docId = hits[i].doc;
            Document doc = searcher.doc(docId);
            if (Integer.parseInt(doc.get("id")) == pId) continue;

            String trackURIs = doc.get("track_uris");

            String[] parts = whiteSpaceSplitter.split(trackURIs);

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

            if (finish) break;

        }

        seeds.clear();

        if (submission.size() != RESULT_SIZE)
            System.out.println("warning result set for " + pId + " size " + submission.size());

        return submission;
    }

    private void export(LinkedHashSet<String> submission, int pid, Format format, PrintWriter out) {
        switch (format) {
            case RECSYS:
                out.print(pid);

                for (String s : submission) {
                    out.print(", ");
                    out.print(s);
                }

                out.println();
                break;
            case TREC:
                int i = 1;
                for (String s : submission) {
                    out.print(pid);
                    out.print("\tQ0\t");
                    out.print(s);
                    out.print("\t");
                    out.print(i);
                    out.print("\t");
                    out.print(i);
                    out.print("\t");
                    out.print(similarityConfig);
                    out.println();

                    i++;
                }
                break;
        }
    }

    enum SpanType {
        SpanOR,
        SpanNear,
    }

    /**
     * Predict tracks for a playlist given its title and the first N tracks. N here is 1, 5, 10, 25, 100
     */
    private LinkedHashSet<String> firstNTracks(Track[] tracks, int pId, SpanType type) throws ParseException, IOException {

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
        final SpanFirstQuery spanFirstQuery;

        if (SpanType.SpanOR.equals(type))
            spanFirstQuery = new SpanFirstQuery(clauses.size() == 1 ? clauses.get(0) : new SpanOrQuery(clauses.toArray(new SpanQuery[clauses.size()])), n);
        else
            spanFirstQuery = new SpanFirstQuery(clauses.size() == 1 ? clauses.get(0) : new SpanNearQuery(clauses.toArray(new SpanQuery[clauses.size()]), 0, true), n);


        ScoreDoc[] hits = searcher.search(spanFirstQuery, Integer.MAX_VALUE).scoreDocs;

        if (hits.length == 0) {
            System.out.println("SpanFirst found zero result found for pId : " + pId + " first " + tracks.length);
            return tracksOnly(tracks, pId);
        }

        LinkedHashSet<String> submission = new LinkedHashSet<>(RESULT_SIZE);

        boolean finish = false;

        for (int i = 0; i < hits.length; i++) {
            int docId = hits[i].doc;
            Document doc = searcher.doc(docId);
            if (Integer.parseInt(doc.get("id")) == pId) continue;

            String trackURIs = doc.get("track_uris");

            System.out.println("trackURIs " + trackURIs);
            System.out.println("seeds " + seeds);

            System.out.println("=============");

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

