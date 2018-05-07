package edu.anadolu;

import com.google.gson.Gson;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.icu.segmentation.ICUTokenizerFactory;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.shingle.ShingleFilterFactory;
import org.apache.lucene.document.*;
import org.apache.lucene.index.ConcurrentMergeScheduler;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Indexer for the Million Playlist Dataset
 */
public class Indexer {

    private static Analyzer analyzer() throws IOException {

        Map<String, Analyzer> analyzerPerField = new HashMap<>();
        analyzerPerField.put("name", icu());
        analyzerPerField.put("track_uris", new WhitespaceAnalyzer());
        analyzerPerField.put(ShingleFilter.DEFAULT_TOKEN_TYPE, shingle());

        return new PerFieldAnalyzerWrapper(new WhitespaceAnalyzer(), analyzerPerField);
    }

    static Analyzer icu() throws IOException {
        return CustomAnalyzer.builder()
                .withTokenizer(ICUTokenizerFactory.class)
                .addTokenFilter("lowercase")
                .build();
    }

    private static Analyzer shingle() throws IOException {
        return CustomAnalyzer.builder()
                .withTokenizer("whitespace")
                .addTokenFilter(ShingleFilterFactory.class,
                        "minShingleSize", "2",
                        "maxShingleSize", "25",
                        "outputUnigrams", "false",
                        "outputUnigramsIfNoShingles", "false"
                ).build();
    }

    static Stream<Path> jSons(Path mpdPath) throws IOException {
        return Files.find(mpdPath,
                1, (Path p, BasicFileAttributes att) -> {

                    if (!att.isRegularFile()) return false;

                    Path name = p.getFileName();

                    return (name != null && name.toString().endsWith(".json"));

                });
    }

    public int index(Path indexPath, Path mpdPath) throws IOException {

        final Gson GSON = new Gson();

        System.out.println("Indexing to directory '" + indexPath + "'...");

        Directory dir = FSDirectory.open(indexPath);

        IndexWriterConfig iwc = new IndexWriterConfig(analyzer());

        iwc.setSimilarity(new BM25Similarity());
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        iwc.setUseCompoundFile(false);
        iwc.setMergeScheduler(new ConcurrentMergeScheduler());
        iwc.setRAMBufferSizeMB(256.0);

        final IndexWriter writer = new IndexWriter(dir, iwc);

        Stream<Path> jSons = jSons(mpdPath);

        jSons.parallel().forEach(path -> {


            try (BufferedReader reader = Files.newBufferedReader(path)) {

                MPD data = GSON.fromJson(reader, MPD.class);

                for (Playlist playlist : data.playlists) {

                    Document document = new Document();
                    document.add(new TextField("name", playlist.name, Field.Store.YES));
                    document.add(new StringField("id", Integer.toString(playlist.pid), Field.Store.YES));
                    document.add(new StoredField("num_tracks", playlist.num_tracks));
                    document.add(new StoredField("num_followers", playlist.num_followers));
                    document.add(new NumericDocValuesField("num_followers", playlist.num_followers));

                    HashSet<String> seeds = new HashSet<>(100);
                    StringBuilder builder = new StringBuilder();
                    StringBuilder album = new StringBuilder();
                    StringBuilder artist = new StringBuilder();
                    for (Track track : playlist.tracks) {
                        if (seeds.contains(track.track_uri)) continue;
                        builder.append(track.track_uri).append(' ');
                        album.append(track.album_uri).append(' ');
                        artist.append(track.artist_uri).append(' ');
                        seeds.add(track.track_uri);
                    }

                    document.add(new TextField("track_uris", builder.toString().trim(), Field.Store.YES));
                    document.add(new TextField("album_uris", album.toString().trim(), Field.Store.YES));
                    document.add(new TextField("artist_uris", artist.toString().trim(), Field.Store.YES));
                    document.add(new TextField(ShingleFilter.DEFAULT_TOKEN_TYPE, builder.toString().trim(), Field.Store.NO));
                    seeds.clear();
                    writer.addDocument(document);
                }

            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        });

        final int numIndexed = writer.maxDoc();

        try {
            writer.commit();
            writer.forceMerge(1);
        } finally {
            writer.close();
            dir.close();
        }

        return numIndexed;
    }
}
