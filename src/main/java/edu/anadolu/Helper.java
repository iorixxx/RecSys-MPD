package edu.anadolu;

import org.apache.lucene.index.Term;
import org.apache.lucene.misc.TermStats;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanTermQuery;

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

    static void export(LinkedHashSet<String> submission, int pid, Format format, PrintWriter out, SimilarityConfig similarityConfig) {
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


    static void blended(LinkedHashSet<String> submission, SortedSet<TermStats> highFreqTrackURIs, List<String> followerFreq) {

        Iterator<String> first = followerFreq.iterator();
        Iterator<TermStats> second = highFreqTrackURIs.iterator();

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
    static void fallBackToMostFollowedTracks(LinkedHashSet<String> submission, List<String> followerFreq) {

        for (final String track : followerFreq) {
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
    static void fallBackToMostFreqTracks(LinkedHashSet<String> submission, SortedSet<TermStats> highFreqTrackURIs) {

        for (final TermStats termStats : highFreqTrackURIs) {

            final String track = termStats.termtext.utf8ToString();

            submission.add(track);
            if (submission.size() == RESULT_SIZE) break;
        }

        if (submission.size() != RESULT_SIZE)
            throw new RuntimeException("after filler operation submission size is not equal to 500! size=" + submission.size());

    }
}
