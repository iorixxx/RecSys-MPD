package edu.anadolu;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.stream.Stream;

import static edu.anadolu.Indexer.jSons;
import static edu.anadolu.Searcher.whiteSpaceSplitter;

/**
 * Feature dumper: Feed features.txt file to LambdaMART
 */
public class Feature {

    public static void main(String[] args) throws IOException {

        if (args.length != 1) {
            System.out.println("only arguments is MPD path where json files reside");
            return;
        }

        Path mpdPath = Paths.get(args[0]);

        if (!Files.isDirectory(mpdPath) || !Files.isReadable(mpdPath)) {
            System.out.println(mpdPath + " is not directory or readable!");
            return;
        }

        dump(mpdPath);

    }


    private static void dump(Path mpdPath) throws IOException {

        final Gson GSON = new Gson();

        PrintWriter out = new PrintWriter(Files.newBufferedWriter(Paths.get("Features.txt"), StandardCharsets.US_ASCII));

        Stream<Path> jSons = jSons(mpdPath);

        jSons.forEach(path -> {

            try (BufferedReader reader = Files.newBufferedReader(path)) {

                MPD data = GSON.fromJson(reader, MPD.class);

                for (Playlist playlist : data.playlists) {

                    final String typeQ = "1" + " qid:" + playlist.pid + " 1:" + playlist.num_followers + " 2:" + playlist.num_tracks + " 3:" + whiteSpaceSplitter.split(playlist.name.trim()).length;

                    HashSet<String> seeds = new HashSet<>(100);

                    for (Track track : playlist.tracks) {
                        if (seeds.contains(track.track_uri)) continue;

                        seeds.add(track.track_uri);

                        out.print(typeQ);
                        out.print(" 4:" + track.pos + " 5:" + track.duration_ms + " # " + track.track_uri);
                        out.println();
                    }
                    seeds.clear();
                }

                out.flush();

            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        });

        out.flush();
        out.close();
    }
}
