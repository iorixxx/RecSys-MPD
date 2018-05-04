package edu.anadolu;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.icu.segmentation.ICUTokenizerFactory;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
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

    static Analyzer analyzer() throws IOException {

        Map<String, Analyzer> analyzerPerField = new HashMap<>();
        analyzerPerField.put("name", icu());
        analyzerPerField.put("track_uris", new WhitespaceAnalyzer());

        return new PerFieldAnalyzerWrapper(icu(), analyzerPerField);
    }

    static Analyzer icu() throws IOException {
        return CustomAnalyzer.builder()
                .withTokenizer(ICUTokenizerFactory.class)
                .addTokenFilter("lowercase")
                .build();
    }

    public int index(Path indexPath, Path mpdPath) throws IOException {
        System.out.println("Indexing to directory '" + indexPath + "'...");

        Directory dir = FSDirectory.open(indexPath);

        IndexWriterConfig iwc = new IndexWriterConfig(analyzer());

        iwc.setSimilarity(new BM25Similarity());
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        iwc.setUseCompoundFile(false);
        iwc.setMergeScheduler(new ConcurrentMergeScheduler());
        iwc.setRAMBufferSizeMB(256.0);

        final IndexWriter writer = new IndexWriter(dir, iwc);

        Stream<Path> jSons = Files.find(mpdPath,
                1, (Path p, BasicFileAttributes att) -> {

                    if (!att.isRegularFile()) return false;

                    Path name = p.getFileName();

                    return (name != null && name.toString().endsWith(".json"));

                });

        jSons.parallel().forEach(path -> {


            try (BufferedReader reader = Files.newBufferedReader(path)) {

                MPD data = MPD.GSON.fromJson(reader, MPD.class);

                for (Playlist playlist : data.playlists) {

                    System.out.print("1" + " qid:" + playlist.pid + " 1:" + playlist.num_followers + " 2:" + playlist.num_tracks);

                    Document document = new Document();
                    document.add(new TextField("name", playlist.name, Field.Store.YES));
                    document.add(new StringField("id", Integer.toString(playlist.pid), Field.Store.YES));
                    document.add(new IntPoint("num_tracks", playlist.num_tracks));
                    document.add(new IntPoint("num_followers", playlist.num_followers));

                    HashSet<String> seeds = new HashSet<>(100);
                    StringBuilder builder = new StringBuilder();
                    for (Track track : playlist.tracks) {
                        if (seeds.contains(track.track_uri)) continue;
                        builder.append(track.track_uri).append(' ');
                        seeds.add(track.track_uri);

                        System.out.print(" 3:" + track.pos + " 4:" + track.duration_ms + " # " + track.track_uri);
                    }

                    document.add(new TextField("track_uris", builder.toString().trim(), Field.Store.YES));
                    seeds.clear();
                    writer.addDocument(document);
                }

            } catch (IOException ioe) {
                ioe.printStackTrace();
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
