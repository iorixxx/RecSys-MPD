package edu.anadolu;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.spans.SpanTermQuery;

import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashSet;

/**
 * Helper class: Trying to refactor code in Searcher, which is hairy.
 */
class Helper {


    static <T extends Query> T addAttribute(Class<T> attClass) {

        return null;

    }

    static <T extends Query> ArrayList<T> clauses(Class<T> queryClass, Track[] tracks, LinkedHashSet<String> seeds) {

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

    static ArrayList<SpanTermQuery> spanTermQueryClauses(Track[] tracks, LinkedHashSet<String> seeds) {

        ArrayList<SpanTermQuery> clauses = new ArrayList<>(tracks.length);


        for (Track track : tracks) {

            // skip duplicate tracks in the playlist. Only consider the first occurrence of the track.
            if (seeds.contains(track.track_uri)) continue;

            seeds.add(track.track_uri);
            clauses.add(new SpanTermQuery(new Term("track_uris", track.track_uri)));
        }

        return clauses;
    }

    static ArrayList<TermQuery> termQueryClauses(Track[] tracks, LinkedHashSet<String> seeds) {

        ArrayList<TermQuery> clauses = new ArrayList<>(tracks.length);


        for (Track track : tracks) {

            // skip duplicate tracks in the playlist. Only consider the first occurrence of the track.
            if (seeds.contains(track.track_uri)) continue;

            seeds.add(track.track_uri);
            clauses.add(new TermQuery(new Term("track_uris", track.track_uri)));
        }

        return clauses;
    }


    static void export(LinkedHashSet<String> submission, int pid, Format format, PrintWriter out, SimilarityConfig similarityConfig) {
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
}
