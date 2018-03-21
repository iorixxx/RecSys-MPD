package edu.anadolu;

import com.google.gson.Gson;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
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
import java.util.Map;
import java.util.stream.Stream;

/**
 * Indexer for the Million Playlist Dataset
 */
public class Indexer {

    private final Gson gson = new Gson();

    private Analyzer analyzer() throws IOException {

        Map<String, Analyzer> analyzerPerField = new HashMap<>();
        analyzerPerField.put("name", anlyzr());
        analyzerPerField.put("track_uris", new WhitespaceAnalyzer());

        return new PerFieldAnalyzerWrapper(anlyzr(), analyzerPerField);


    }

    private static Analyzer anlyzr() throws IOException {

        return CustomAnalyzer.builder()
                .withTokenizer("standard")
                .addTokenFilter("lowercase")
                .build();
    }


    int index(Path indexPath, Path mdp) throws IOException {

        System.out.println("Indexing to directory '" + indexPath + "'...");

        Directory dir = FSDirectory.open(indexPath);

        IndexWriterConfig iwc = new IndexWriterConfig(analyzer());

        iwc.setSimilarity(new BM25Similarity());
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        iwc.setUseCompoundFile(false);
        iwc.setMergeScheduler(new ConcurrentMergeScheduler());
        iwc.setRAMBufferSizeMB(256.0);

        final IndexWriter writer = new IndexWriter(dir, iwc);

        Stream<Path> jSons = Files.find(mdp,
                1, (Path p, BasicFileAttributes att) -> {

                    if (!att.isRegularFile()) return false;

                    Path name = p.getFileName();

                    return (name != null && name.toString().endsWith(".json"));

                });

        jSons.forEach(path -> {


            try (BufferedReader reader = Files.newBufferedReader(path)) {

                MPD data = gson.fromJson(reader, MPD.class);

                for (Playlist playlist : data.playlists) {

                    Document document = new Document();
                    document.add(new TextField("name", playlist.name, Field.Store.YES));
                    document.add(new StringField("id", Integer.toString(playlist.pid), Field.Store.YES));

                    StringBuilder builder = new StringBuilder();
                    for (Track track : playlist.tracks) {
                        builder.append(track.track_uri).append(' ');
                    }

                    document.add(new TextField("track_uris", builder.toString().trim(), Field.Store.YES));

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
        }

        dir.close();


        return numIndexed;
    }

}
