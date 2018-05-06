package edu.anadolu;


import org.apache.lucene.util.English;
import org.apache.lucene.util.PriorityQueue;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.anadolu.Searcher.whiteSpace;

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

    @Test
    public void mismatch() {

        String kURIs = "spotify:track:4y1LsJpmMti1PfRQV9AWWe spotify:track:7uHO4AmKtyGa5v5fsElGoC spotify:track:5ChkMS8OtdzJeqyybCc9R5 spotify:track:2tUBqZG2AbRi7Q0BIrVrEj spotify:track:2FMcDUopGfjBh3xMsrm78S spotify:track:4aKIs5t9TqP59btlCGPrgw spotify:track:5RsUlxLto4NZbhJpqJbHfN spotify:track:4bHsxqR3GMrXTxEPLuK5ue spotify:track:1oYZm4FC6H2SPwZuHeHQlg spotify:track:0ikz6tENMONtK6qGkOrU3c spotify:track:2HHtWyy5CgaQbC7XSoOb0e spotify:track:3MrRksHupTVEQ7YbA0FsZK spotify:track:5PIB8aoUJoCRh1O5TEGdOj spotify:track:1TfqLAPs4K3s2rJMoCokcS spotify:track:4McRlwqJQIERlJFiJEgbP0 spotify:track:2374M0fQpWi3dLnB54qaLX spotify:track:7jTKLKs3NDmDnJHan5M2A0 spotify:track:2IvetNzSZMH5gwjInoyr18 spotify:track:2f95gIKDeiDyk25KFkiL4m spotify:track:6iX1f3r7oUJnMbGgQ2gx1j spotify:track:4s0QpaeB5a3C9cxNGqPXxk spotify:track:709JP07qJbz95uMOLHYlWu spotify:track:3BGbqEDio3ocx1v4egIYr6 spotify:track:2RtnoUsvJg3wMgMeEhG6fr spotify:track:7fRvtXvJMpGfTLdF0M09a1 spotify:track:4moXVLw4YS7tVNmjs8v60A spotify:track:35k31HZI4z9PbBOioaI4dZ spotify:track:0RMo4bixbZYfzuJ4AYBLu9 spotify:track:3FdHgoJbH3DXNtGLh56pFu spotify:track:5a3QwvHiJO4fKWJqsPkJvB spotify:track:3koCCeSaVUyrRo3N2gHrd8 spotify:track:7tGOxq2rtohwKQml0YVF9d spotify:track:7b4yGtR0rujvLi5EBfKKJ2 spotify:track:4oDZ5L8izBals6jKBJDBcX spotify:track:3vV3cr2TpPqFk07zxYUbla spotify:track:7aHRctaQ7vjxVTVmY8OhAA spotify:track:5WwqdeavrQrbeAMDxGawse spotify:track:4uLU6hMCjMI75M1A2tKUQC spotify:track:4BFMQ15vXr626UOoZL8bUI spotify:track:3VZmChrnVW8JK6ano4gSED spotify:track:3XEtw7t4V5sfd2vtkp0ql7 spotify:track:3TjlMH27nWbY3veJ8fHdaD spotify:track:49eGLnL0ygM6XdU54lh5rA spotify:track:1ot6jEe4w4hYnsOPjd3xKQ spotify:track:2BeFYfKuc0AZGhbeVY78ep spotify:track:0qxYx4F3vm1AOnfux6dDxP spotify:track:7INi4pMPG4IE0Smx5y4KVf spotify:track:4YR6Dextuoc3I8nJ0XgzKI spotify:track:31H6au3jhblhr6MMJiXnCq spotify:track:5zA8vzDGqPl2AzZkEYQGKh spotify:track:5g3ZD7PmrEQlQZKDW91yGG spotify:track:7wQ9alB79WZb0F5gFLbxSh spotify:track:1OOtq8tRnDM8kG2gqUPjAj spotify:track:5tdKaKLnC4SgtDZ6RlWeal spotify:track:1hx5XSk2uGFGBuUIROLGHN spotify:track:1mCsF9Tw4AkIZOjvZbZZdT spotify:track:7o9uu2GDtVDr9nsR7ZRN73 spotify:track:0N5Du3CNQilIBTxkwDQadv spotify:track:57Y3UccJEJqT8w8RWkUAz0 spotify:track:5wiu4PUC6CLNNbC48vqGOb spotify:track:4NbAG8EAR5ZPr19hu1Qm54 spotify:track:1xPSDf8z4dH46gkvlLtvDO spotify:track:2Wb9ejnmy27DUTUe9YF5Ew spotify:track:40dJCw4xU6Bd5ie9rfagNo spotify:track:5PM96PMKMfD1lLX2lryUsG spotify:track:4prr1evklt7SgyknM0WKbp spotify:track:3UBXkWznXqXF7Cg3y1UKsI spotify:track:58mFu3oIpBa0HLNeJIxsw3 spotify:track:2dO88mixgzqmgy56wFUVSY spotify:track:0HEmnAUT8PHznIAAmVXqFJ spotify:track:2m0M7YqCy4lXfedh18qd8N spotify:track:49zeNqC8dkKofXLqG9kFTr spotify:track:2WfaOiMkCvy7F5fcp2zZ8L spotify:track:4aWn4NHlELpOehxsBaQeoe spotify:track:39lSeqnyjZJejRuaREfyLL spotify:track:4ebcE2SmkG7nplvzFAWRu7 spotify:track:1F43XlPBiwAUUIhrUGzylO spotify:track:0gFB5H3pHN13ERt2FyMuWi spotify:track:310epXrlbXmfGcD1qSdgVV spotify:track:0wQy2OO7jKjm0OOmA7gv3f spotify:track:4buDeg67vos7KP1yHrS9wl spotify:track:1vYew137WJ69SFp9oez3xm spotify:track:2b9lp5A6CqSzwOrBfAFhof spotify:track:5C0LFQARavkPpn7JgA4sLk spotify:track:7o2CTH4ctstm8TNelqjb51 spotify:track:3Cx4yrFaX8CeHwBMReOWXI spotify:track:2R7858bg0GHuBBxjTyOL7N spotify:track:4sscDOZCkbLSlDqcCgUJnX spotify:track:2VNfJpwdEQBLyXajaa6LWT spotify:track:5lA3pwMkBdd24StM90QrNR spotify:track:43btz2xjMKpcmjkuRsvxyg spotify:track:1lx8ddGT5wCD6W2xmLeRKG spotify:track:1ZPlNanZsJSPK5h9YZZFbZ spotify:track:37BTh5g05cxBIRYMbw8g2T spotify:track:5bHktj4UtEORf7uNWTtxnA spotify:track:3L7RtEcu1Hw3OXrpnthngx spotify:track:0DnA2DyUwy0W4xMYtRxocL spotify:track:2wSAWEYUHkt92X4SBAPqZE spotify:track:7mcW98cnE6Osce4ae7t4oZ spotify:track:4ByEFOBuLXpCqvO1kw8Wdm spotify:track:0GTK6TesV108Jj5D3MHsYb spotify:track:05wIrZSwuaVWhcv5FfqeH0 spotify:track:1UBQ5GK8JaQjm5VbkBZY66 spotify:track:6jBCehpNMkwFVF3dz4nLIW spotify:track:1gzIbdFnGJ226LTl0Cn2SX spotify:track:0cGG2EouYCEEC3xfa0tDFV spotify:track:2wXtG9bMf5iu9LSAHEJvaJ spotify:track:5tVA6TkbaAH9QMITTQRrNv spotify:track:5lWFrW5T3JtxVCLDb7etPu spotify:track:7Di7t9yGoxdZRLAt5a4pi0 spotify:track:2BPfKiV9U0CR1dpUgeUwuH spotify:track:2vEQ9zBiwbAVXzS2SOxodY spotify:track:5NLuC70kZQv8q34QyQa1DP spotify:track:41dDygR3r7e926oGUXfrLt spotify:track:5kz9GDBTX846OXwqWoyKzF spotify:track:4NnWuGQujzWUEg0uZokO5M spotify:track:7o7E1nrHWncYY7PY94gCiX spotify:track:1EXrFPfVNVsyb32yapebbM spotify:track:57ebBLITHpRgRKGrlbxMZS spotify:track:3wfujdbamR3Z46F4xav7LM spotify:track:7L2eFj0KFJDmHTPMUL4ZxR spotify:track:22tceep2vzeOKKX1jIjiLi spotify:track:3L9ClO1W5KmebIXTrlKShF spotify:track:0odIT9B9BvOCnXfS0e4lB5 spotify:track:3ZI3sK9ZJB3LOiZtCs9l3p spotify:track:0TT7wJiEYD5GAeJfSR1ETX spotify:track:682nhC3aQtY9HwrC2P2vz4 spotify:track:1ynmMEK1fkyiZ6Z6F3ThEt spotify:track:6qBSGvyUzqNQv8XtnzCr9n spotify:track:3v8vsQfMQio7ohYqFrEsaZ spotify:track:7iWk45E2wIpFc3YUWm3VVn spotify:track:2w4EpqGasrz9qdTwocx54t spotify:track:5uRsBdLhn91AYcsTCOGNOC spotify:track:7dQC53NiYOY9gKg3Qsu2Bs spotify:track:4W4wYHtsrgDiivRASVOINL spotify:track:4R11j2BjqkwcEmVxEzDQNG spotify:track:4XX1pFUkQOZTYp6Hb6a6Ae spotify:track:3MIVb9pn5cDSdK7yNDsMU1 spotify:track:5xl5582IihbEZAnfj0xyso spotify:track:7fmld8tYlaKIe8B1Rckc8P spotify:track:6ZWkoybEo5CHvxKEPkQ8mw spotify:track:0p2lB3tOc2DJGTUltBxjkv spotify:track:2tY1gxCKslfXLFpFofYmJQ spotify:track:54X78diSLoUDI3joC2bjMz spotify:track:6X4JeTWCuKEzKOEHXDtyBo spotify:track:2H7PHVdQ3mXqEHXcvclTB0 spotify:track:62LJFaYihsdVrrkgUOJC05 spotify:track:5jSz894ljfWE0IcHBSM39i spotify:track:7IoLw1bECmOSWcm2u9SmRH spotify:track:04MjaNej8XUHI8jbz2THez spotify:track:3nvuPQTw2zuFAVuLsC9IYQ";
        String seeds = "spotify:track:4y1LsJpmMti1PfRQV9AWWe spotify:track:7uHO4AmKtyGa5v5fsElGoC spotify:track:5ChkMS8OtdzJeqyybCc9R5 spotify:track:2tUBqZG2AbRi7Q0BIrVrEj spotify:track:2FMcDUopGfjBh3xMsrm78S";

        int i = Arrays.mismatch(whiteSpace.split(kURIs), whiteSpace.split(seeds));
        Assert.assertEquals(5, i);

        i = Arrays.mismatch(whiteSpace.split(kURIs), new String[]{"junk"});
        Assert.assertEquals(0, i);

        i = Arrays.mismatch(whiteSpace.split(kURIs), whiteSpace.split(kURIs));
        Assert.assertEquals(-1, i);

        i = Arrays.mismatch(whiteSpace.split(kURIs), new String[]{"spotify:track:4y1LsJpmMti1PfRQV9AWWe"});
        Assert.assertEquals(1, i);
    }

    @Test
    public void testPriorityQueue() {

        PriorityQueue<StringIntPair> priorityQueue = new PriorityQueue<>(10) {
            @Override
            protected boolean lessThan(StringIntPair a, StringIntPair b) {
                return Integer.compare(a.integer, b.integer) < 0;
            }
        };


        for (int i = 0; i < 1000; i++) {
            priorityQueue.insertWithOverflow(new StringIntPair(English.intToEnglish(i), i));
        }

        for (int i = 2000; i > 1000; i--) {
            priorityQueue.insertWithOverflow(new StringIntPair(English.intToEnglish(i), i));
        }


        System.out.println("pq size = " + priorityQueue.size());

        List<StringIntPair> reverse = Helper.reverse(priorityQueue);

        for (StringIntPair pair : reverse) {
            System.out.println(pair.integer + "\t" + pair.string);
        }


    }
}
