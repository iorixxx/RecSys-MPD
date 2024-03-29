package edu.anadolu;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.util.PriorityQueue;

import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static edu.anadolu.Searcher.RESULT_SIZE;
import static edu.anadolu.Searcher.whiteSpace;

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

    // TODO
    static Track resolveTrackFromURI(String trackURI) {
        return new Track();
    }

    // TODO
    static String actualLabel(String trackURI) {
        if (ThreadLocalRandom.current().nextBoolean())
            return "1";
        else return "0";

    }

    static synchronized void export(LinkedHashSet<String> submission, Playlist playlist, PrintWriter out) {

        for (String s : submission) {
            out.print(playlist.pid);
            out.print(",");
            out.print(s);
            out.println();
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
     * If certain algorithm collects less than MAX_RESULT_SIZE tracks,
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

    static <T> List<T> reverse(PriorityQueue<T> priorityQueue) {
        List<T> reverse = new ArrayList<>(priorityQueue.size());

        while (priorityQueue.size() != 0) {
            reverse.add(priorityQueue.pop());
        }
        Collections.reverse(reverse);
        return reverse;
    }
}
