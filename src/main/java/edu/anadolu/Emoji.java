package edu.anadolu;


import com.ibm.icu.lang.UScript;
import com.ibm.icu.lang.UScriptRun;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.icu.tokenattributes.ScriptAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.Query;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * Helper class for Emoji Presentation Sequences
 *
 * @see <a href="http://unicode.org/emoji/charts/emoji-variants.html">emoji</a>
 */
public class Emoji {


    static int analyze(String text) throws IOException {
        Analyzer analyzer = Indexer.icu();

        // The Analyzer class will construct the Tokenizer, TokenFilter(s), and CharFilter(s),
        //   and pass the resulting Reader to the Tokenizer.

        int i=0;

        try (Reader reader = new StringReader(text);
             TokenStream ts = analyzer.tokenStream("field", reader)) {

            ScriptAttribute script = ts.addAttribute(ScriptAttribute.class);
            CharTermAttribute term = ts.addAttribute(CharTermAttribute.class);
            TypeAttribute type = ts.addAttribute(TypeAttribute.class);
            ts.reset(); // Resets this stream to the beginning. (Required)
            while (ts.incrementToken()) {
                System.out.println(term.toString() + " " + script.getShortName() + " " + type.type());
                i++;
            }
            ts.end();   // Perform end-of-stream operations, e.g. set the final offset.
        }

        return i;
    }

    public static void main(String[] args) throws Exception {
        String text = "\uD83D \uDD25 \uD83D \uDD25 \uD83D \uDD25 \u261D ahmet config \uD83D\uDCA9 \uD83D\uDCA9\uD83D\uDCA9 help Its_lit \uD83D\uDC9A\uD83D\uDC9A\uD83D\uDC9A";
        analyze(text);
        printScriptRuns(text.toCharArray());

        System.out.println("\uD83D\uDC9A\uD83D\uDC9A\uD83D\uDC9A");


        QueryParser queryParser = new QueryParser("title", Indexer.icu());

        Query query = queryParser.parse("\uD83D\uDC9A\uD83D\uDC9A\uD83D\uDC9A");

        System.out.println(query);

        System.out.println(Searcher.whiteSpaceSplitter.split("\uD83D\uDC9A\uD83D\uDC9A\uD83D\uDC9A").length);

        System.out.println(analyze("\uD83D\uDC9A\uD83D\uDC9A\uD83D\uDC9A"));

        String title = "Its_lit";

        System.out.println(title.replaceAll("_", " "));
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
