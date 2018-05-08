package edu.anadolu;


import com.ibm.icu.lang.UScript;
import com.ibm.icu.lang.UScriptRun;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;

/**
 * Helper class for Emoji Presentation Sequences
 *
 * @see <a href="http://unicode.org/emoji/charts/emoji-variants.html">emoji</a>
 */
public class Emoji {

    /**
     * This to prevent BooleanQuery$TooManyClauses: maxClauseCount is set to 1024 caused by shingle
     *
     * @return maxClauseCount
     */
    static int maxClauseCount() {

        StringBuilder builder = new StringBuilder();
        for (int i = 1; i <= 10; i++)
            builder.append(Integer.toString(i)).append(' ');

        int maxClauseCount = 0;

        try (Reader reader = new StringReader(builder.toString().trim());
             Analyzer analyzer = Indexer.shingle();
             TokenStream ts = analyzer.tokenStream("field", reader)) {

            CharTermAttribute term = ts.addAttribute(CharTermAttribute.class);

            ts.reset(); // Resets this stream to the beginning. (Required)
            while (ts.incrementToken()) {
                System.out.println(term.toString());
                maxClauseCount++;
            }
            ts.end();   // Perform end-of-stream operations, e.g. set the final offset.
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return BooleanQuery.getMaxClauseCount();
        }

        return maxClauseCount + 1;
    }


    static ArrayList<String> analyze(String text) {

        ArrayList<String> terms = new ArrayList<>();

        try (Reader reader = new StringReader(text);
             Analyzer analyzer = Indexer.icu();
             TokenStream ts = analyzer.tokenStream("field", reader)) {

            //ScriptAttribute script = ts.addAttribute(ScriptAttribute.class);
            CharTermAttribute term = ts.addAttribute(CharTermAttribute.class);
            //TypeAttribute type = ts.addAttribute(TypeAttribute.class);
            ts.reset(); // Resets this stream to the beginning. (Required)
            while (ts.incrementToken()) {
                terms.add(term.toString());
            }
            ts.end();   // Perform end-of-stream operations, e.g. set the final offset.

        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        return terms;
    }

    public static void main(String[] args) throws Exception {
        String text = "\uD83D \uDD25 \uD83D \uDD25 \uD83D \uDD25 \u261D ahmet config \uD83D\uDCA9 \uD83D\uDCA9\uD83D\uDCA9 help Its_lit \uD83D\uDC9A\uD83D\uDC9A\uD83D\uDC9A";
        analyze(text);
        printScriptRuns(text.toCharArray());

        System.out.println("\uD83D\uDC9A\uD83D\uDC9A\uD83D\uDC9A");


        QueryParser queryParser = new QueryParser("title", Indexer.icu());

        Query query = queryParser.parse("\uD83D\uDC9A\uD83D\uDC9A\uD83D\uDC9A");

        System.out.println(query);

        System.out.println(Searcher.whiteSpace.split("\uD83D\uDC9A\uD83D\uDC9A\uD83D\uDC9A").length);

        System.out.println(analyze("\uD83D\uDC9A\uD83D\uDC9A\uD83D\uDC9A"));

        String title = "Its_lit";

        System.out.println(title.replaceAll("_", " "));


        System.out.println(maxClauseCount());
    }


    static void printScriptRuns(char[] text) {
        UScriptRun scriptRun = new UScriptRun(text);

        while (scriptRun.next()) {
            int start = scriptRun.getScriptStart();
            int limit = scriptRun.getScriptLimit();
            int script = scriptRun.getScriptCode();

            System.out.println("Script \"" + UScript.getName(script) + "\" from " +
                    start + " to " + limit + ".");
        }
    }
}
