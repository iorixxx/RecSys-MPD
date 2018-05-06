package edu.anadolu;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.util.PriorityQueue;

import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static edu.anadolu.Searcher.RESULT_SIZE;

/**
 * Helper class: Trying to refactor code in Searcher, which is hairy.
 */
class Helper {

    static ArrayList<SpanTermQuery> clauses(Track[] tracks, LinkedHashSet<String> seeds) {

        seeds.clear();
        ArrayList<SpanTermQuery> clauses = new ArrayList<>(tracks.length);


        for (Track track : tracks) {
            // skip duplicate tracks in the playlist. Only consider the first occurrence of the track.
            if (seeds.contains(track.track_uri)) continue;

            seeds.add(track.track_uri);
            clauses.add(new SpanTermQuery(new Term("track_uris", track.track_uri)));
        }


        return clauses;

    }


    static <T extends Query> ArrayList<T> clauses(Class<T> queryClass, Track[] tracks, LinkedHashSet<String> seeds) {

        seeds.clear();
        ArrayList<T> clauses = new ArrayList<>(tracks.length);


        try {

            Constructor<T> cons = queryClass.getConstructor(Term.class);

            for (Track track : tracks) {

                // skip duplicate tracks in the playlist. Only consider the first occurrence of the track.
                if (seeds.contains(track.track_uri)) continue;

                seeds.add(track.track_uri);
                clauses.add(cons.newInstance(new Term("track_uris", track.track_uri)));
            }

        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException exception) {
            throw new RuntimeException(exception);
        }

        return clauses;

    }

    static synchronized void export(LinkedHashSet<String> submission, int pid, Format format, PrintWriter out, SimilarityConfig similarityConfig) {
        switch (format) {
            case RECSYS:
                out.print(pid);

                for (String s : submission) {
                    out.print(",");
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


    static void blended(LinkedHashSet<String> submission, Set<String> seeds, List<String> highFreqTrackURIs, List<String> followerFreq) {

        Iterator<String> first = followerFreq.iterator();
        Iterator<String> second = highFreqTrackURIs.iterator();

        int toggle = 0;
        while (first.hasNext() || second.hasNext()) {
            final boolean done;
            if (++toggle % 2 == 0) {
                done = insertSingleTrack(submission, seeds, first.next(), RESULT_SIZE);
            } else {
                done = insertSingleTrack(submission, seeds, second.next(), RESULT_SIZE);
            }
            if (done) break;
        }

        if (submission.size() != RESULT_SIZE)
            throw new RuntimeException("after filler operation submission size is not equal to 500! size=" + submission.size());

    }


    /**
     * If certain algorithm collects less than RESULT_SIZE tracks,
     * then fill remaining tracks using most frequent tracks as a last resort.
     */
    static void fallBackTo(LinkedHashSet<String> submission, Set<String> seeds, List<String> followerFreq) {

        boolean done = insertTrackURIs(submission, seeds, followerFreq, RESULT_SIZE);

        if (!done)
            throw new RuntimeException("after filler operation submission size is not equal to 500! size=" + submission.size());
    }

    private static boolean insertSingleTrack(LinkedHashSet<String> submission, Set<String> seeds, String t, int howMany) {

        if (seeds.contains(t) || submission.contains(t)) return false;
        submission.add(t);
        return submission.size() == howMany;

    }

    static boolean insertTrackURIs(LinkedHashSet<String> submission, Set<String> seeds, Iterable<String> iterable, int howMany) {

        for (String t : iterable) {
            if (insertSingleTrack(submission, seeds, t, howMany)) {
                return true;
            }
        }
        return false;
    }


    static void incrementPageCountMap(LinkedHashMap<Integer, Integer> pageCount, int i) {
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

    static void printPageCountMap(LinkedHashMap<Integer, Integer> pageCount) {
        pageCount.entrySet().stream()
                .filter(o -> o.getValue() > 0)
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .forEach(o -> System.out.println(o.getKey() + "\t" + o.getValue()));
    }


    static <T> List<T> reverse(PriorityQueue<T> priorityQueue) {
        List<T> reverse = new ArrayList<>(priorityQueue.size());

        while (priorityQueue.size() != 0) {
            reverse.add(priorityQueue.pop());
        }
        Collections.reverse(reverse);
        return reverse;
    }
}
