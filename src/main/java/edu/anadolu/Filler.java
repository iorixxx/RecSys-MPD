package edu.anadolu;

import com.google.gson.Gson;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.misc.HighFreqTerms;
import org.apache.lucene.misc.TermStats;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static edu.anadolu.Searcher.RESULT_SIZE;
import static edu.anadolu.Searcher.whiteSpace;
import static org.apache.lucene.misc.HighFreqTerms.getHighFreqTerms;

/**
 * Filler strategy for fall back mechanism
 */
public class Filler {

    public enum Type {
        Follower,
        Playlist,
        Blended,
        Hybrid
    }

    private final List<String> highFreqTrackURIs;
    private final List<String> followerFreq;

    private static final Pattern comma = Pattern.compile(",");

    private final MPD challenge;

    public Filler(Path indexPath, Path challengePath) {

        final Gson GSON = new Gson();

        try (BufferedReader reader = Files.newBufferedReader(challengePath)) {
            this.challenge = GSON.fromJson(reader, MPD.class);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Comparator<TermStats> comparator = new HighFreqTerms.DocFreqComparator();


        if (!Files.exists(indexPath) || !Files.isDirectory(indexPath) || !Files.isReadable(indexPath)) {
            throw new IllegalArgumentException(indexPath + " does not exist or is not a directory.");
        }

        try (FSDirectory directory = FSDirectory.open(indexPath); IndexReader reader = DirectoryReader.open(directory)) {

            TermStats[] terms = getHighFreqTerms(reader, RESULT_SIZE * 2, "track_uris", comparator);

            System.out.println("Top-10 Track URIs sorted by playlist frequency");
            System.out.println("term \t totalTF \t docFreq");


            List<String> highFreqTrackURIs = new ArrayList<>(RESULT_SIZE * 2);
            int i = 0;
            for (TermStats termStats : terms) {
                if (i++ < 10)
                    System.out.printf(Locale.ROOT, "%s \t %d \t %d \n",
                            termStats.termtext.utf8ToString(), termStats.totalTermFreq, termStats.docFreq);
                highFreqTrackURIs.add(termStats.termtext.utf8ToString());
            }
            this.highFreqTrackURIs = Collections.unmodifiableList(highFreqTrackURIs);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int toggle = 0;

    private void fill(Type filler, PrintWriter out, Set<String> seeds, int howMany) {
        if (Filler.Type.Follower.equals(filler))
            insertTrackURIs(out, seeds, this.followerFreq, howMany);
        else if (Filler.Type.Blended.equals(filler))
            blended(out, seeds, this.highFreqTrackURIs, this.followerFreq, howMany);
        else if (Filler.Type.Hybrid.equals(filler)) {
            if (++toggle % 2 == 0) {
                insertTrackURIs(out, seeds, this.followerFreq, howMany);
            } else {
                insertTrackURIs(out, seeds, this.highFreqTrackURIs, howMany);
            }
        } else if (Filler.Type.Playlist.equals(filler))
            insertTrackURIs(out, seeds, this.highFreqTrackURIs, howMany);
        else
            insertTrackURIs(out, seeds, this.highFreqTrackURIs, howMany);

    }

    private static void insertTrackURIs(PrintWriter out, Set<String> seeds, Iterable<String> iterable, int howMany) {
        int counter = 0;
        for (String t : iterable) {
            if (insertSingleTrack(out, seeds, t)) {
                counter++;
                if (howMany == counter) return;
            }
        }
        throw new RuntimeException("can't happen!");
    }

    private static void blended(PrintWriter out, Set<String> seeds, List<String> highFreqTrackURIs, List<String> followerFreq, int howMany) {

        Iterator<String> first = followerFreq.iterator();
        Iterator<String> second = highFreqTrackURIs.iterator();

        int toggle = 0;
        int counter = 0;
        while (first.hasNext() || second.hasNext()) {

            if (++toggle % 2 == 0) {
                if (insertSingleTrack(out, seeds, first.next()))
                    counter++;
            } else {
                if (insertSingleTrack(out, seeds, second.next())) {
                    counter++;
                }
            }
            if (howMany == counter) return;
        }

        throw new RuntimeException("can't happen!");

    }


    private static boolean insertSingleTrack(PrintWriter out, Set<String> seeds, String t) {
        if (seeds.contains(t)) return false;
        out.print(",");
        out.print(t);
        seeds.add(t);
        return true;
    }

    public void fillInTheBlanks(Path resultPath) {
        try {
            for (Type type : Type.values())
                fillInTheBlanks(resultPath, type);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private void fillInTheBlanks(Path resultPath, Type filler) throws IOException {

        int blankLines = 0;
        int completeLines = 0;

        int insertions = 0;

        String name = resultPath.getFileName().toString();

        int i = name.lastIndexOf('.');

        final String newFileName;
        if (i != -1) {
            newFileName = name.substring(0, i) + "_" + filler.toString() + name.substring(i);
        } else
            newFileName = name + filler.toString() + ".csv";


        final PrintWriter out = new PrintWriter(Files.newBufferedWriter(resultPath.toAbsolutePath().getParent().resolve(newFileName), StandardCharsets.US_ASCII));


        List<String> lines = Files.readAllLines(resultPath);

        for (String line : lines) {

            if (line.startsWith("team_info")) {
                out.println(line);
                continue;
            }

            final List<String> list = Arrays.stream(comma.split(line)).distinct().collect(Collectors.toList());

            if (list.size() == 1) blankLines++;

            if (list.size() == RESULT_SIZE + 1) {
                out.println(line);
                completeLines++;
                continue;
            }

            out.print(line);

            int id = Integer.parseInt(list.get(0));

            Set<String> seeds = Arrays.stream(challenge.playlists)
                    .filter(p -> p.pid == id)
                    .map(p -> p.tracks).flatMap(Arrays::stream)
                    .map(t -> t.track_uri)
                    .distinct()
                    .collect(Collectors.toSet());

            seeds.addAll(list);

            int howMany = RESULT_SIZE - list.size() + 1;
            insertions += howMany;
            fill(filler, out, seeds, howMany);
            out.println();
        }

        out.flush();
        out.close();

        System.out.println("Stats for " + filler.toString());
        System.out.println("completeLines " + completeLines);
        System.out.println("blankLines " + blankLines);
        System.out.println("filledTracks " + insertions);
    }

    /**
     * Generates filler submissions that consist of only fillers:
     * JustPIDs_Follower.csv JustPIDs_Hybrid.csv JustPIDs_Playlist.csv JustPIDs_Blended.csv
     * THis is find out which filler type is more effective.
     */
    public void generateFillerSubmission(Path path) {
        Path justPIDs = dumpJustPIDs(path.toAbsolutePath().getParent().resolve("JustPIDs.csv"));
        fillInTheBlanks(justPIDs);
    }

    private Path dumpJustPIDs(Path path) {

        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(path, StandardCharsets.US_ASCII))) {
            Arrays.stream(challenge.playlists).map(p -> p.pid).forEach(out::println);
            out.flush();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return path;
    }

    public static void main(String[] args) {
        System.out.println(Arrays.toString(comma.split("sdfsdf")));

        String name = "dph_irra.csv";

        Type filler = Type.Follower;

        int i = name.lastIndexOf('.');

        final String newFileName;
        if (i != -1) {
            newFileName = name.substring(0, i) + "_" + filler.toString() + name.substring(i);
        } else
            newFileName = name + filler.toString() + ".csv";

        System.out.println(newFileName);

        System.out.println(Paths.get(name));
        System.out.println(Paths.get(name).toAbsolutePath().getParent().resolve("test"));
    }

}
