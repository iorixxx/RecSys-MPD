package edu.anadolu;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.spans.SpanTermQuery;

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

        //  if (queryClass.equals(SpanTermQuery.class)) return spanTermQueryClauses(tracks, seeds);

        // queryClass.newInstance();
        return null;

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
}
