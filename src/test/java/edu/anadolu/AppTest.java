package edu.anadolu;


import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Unit test for submission backed by LinkedHashSet<String>
 */
public class AppTest {

    /**
     * We use LinkedHashSet<String></String> data structure to model the submission.
     * Submission must consider the first inclusion/addition if duplicate keys are inserted later on
     */
    @Test
    public void testSubmission() {

        LinkedHashSet<String> submission = new LinkedHashSet<>();

        submission.add("first");
        submission.add("second");
        submission.add("third");

        // duplicates
        submission.add("first");
        submission.add("second");

        Assert.assertEquals(3, submission.size());
        String[] expected = new String[]{"first", "second", "third"};
        int i = 0;
        for (String s : submission) {
            Assert.assertEquals(expected[i++], s);
        }
    }

    /**
     * Set eliminates duplicates for sure but how about the insertion order?
     */
    @Test
    public void testSubmissionOrder() {

        LinkedHashSet<String> submission = new LinkedHashSet<>();

        for (int i = 0; i < 500; i++)
            submission.add(Integer.toBinaryString(i));

        List<Integer> shuffle = IntStream.range(0, 500).boxed().collect(Collectors.toList());

        Collections.shuffle(shuffle);

        for (int i : shuffle)
            submission.add(Integer.toBinaryString(i));

        Assert.assertEquals(500, submission.size());

        int i = 0;
        for (String s : submission) {
            Assert.assertEquals(Integer.toBinaryString(i++), s);
        }
    }

    /**
     * Set eliminates duplicates for sure but how about the insertion order?
     */
    @Test
    public void testSubmissionOrder2() {

        LinkedHashSet<String> submission = new LinkedHashSet<>();

        List<Integer> shuffle = IntStream.range(0, 500).boxed().collect(Collectors.toList());

        Collections.shuffle(shuffle);

        for (int i : shuffle)
            submission.add(Integer.toHexString(i));

        for (int i = 0; i < 500; i++)
            submission.add(Integer.toHexString(i));

        Assert.assertEquals(500, submission.size());

        int i = 0;
        for (String s : submission) {
            Assert.assertEquals(Integer.toHexString(shuffle.get(i++)), s);
        }
    }
}
