package edu.anadolu;


import com.ibm.icu.lang.UScript;
import com.ibm.icu.lang.UScriptRun;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.icu.tokenattributes.ScriptAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * Helper class for Emoji Presentation Sequences
 *
 * @see <a href="http://unicode.org/emoji/charts/emoji-variants.html">emoji</a>
 */
public class Emoji {


    static void analyze(String text) throws IOException {
        Analyzer analyzer = Indexer.icu();

        // The Analyzer class will construct the Tokenizer, TokenFilter(s), and CharFilter(s),
        //   and pass the resulting Reader to the Tokenizer.


        try (Reader reader = new StringReader(text);
             TokenStream ts = analyzer.tokenStream("field", reader)) {

            ScriptAttribute script = ts.addAttribute(ScriptAttribute.class);
            CharTermAttribute term = ts.addAttribute(CharTermAttribute.class);
            TypeAttribute type = ts.addAttribute(TypeAttribute.class);
            ts.reset(); // Resets this stream to the beginning. (Required)
            while (ts.incrementToken()) {
                System.out.println(term.toString() + " " + script.getShortName() + " " + type.type());
            }
            ts.end();   // Perform end-of-stream operations, e.g. set the final offset.
        }

    }

    public static void main(String[] args) throws IOException {
        String text = "\uD83D \uDD25 \uD83D \uDD25 \uD83D \uDD25 \u261D ahmet config \uD83D\uDCA9 \uD83D\uDCA9\uD83D\uDCA9 help \uD83D\uDC9A\uD83D\uDC9A\uD83D\uDC9A";
        analyze(text);
        printScriptRuns(text.toCharArray());
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
