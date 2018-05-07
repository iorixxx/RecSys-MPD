package edu.anadolu;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SpanNearConfig implements Comparable<SpanNearConfig>, Cloneable {

    int slop;
    int end;
    boolean inOrder;

    private SpanNearConfig(int slop, int end, boolean inOrder) {
        this.slop = slop;
        this.end = end;
        this.inOrder = inOrder;
    }

    private void setInOrder(boolean inOrder) {
        this.inOrder = inOrder;
    }

    /**
     * SpanNearConfig{slop=0, end=25, inOrder=true} for tracks 25
     */
    boolean tightest(int n) {
        return this.inOrder && n == this.end && 0 == this.slop;
    }

    @Override
    public String toString() {
        return "SpanNearConfig{" +
                "slop=" + slop +
                ", end=" + end +
                ", inOrder=" + inOrder +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpanNearConfig config = (SpanNearConfig) o;
        return slop == config.slop &&
                end == config.end &&
                inOrder == config.inOrder;
    }

    @Override
    public int hashCode() {
        return Objects.hash(slop, end, inOrder);
    }

    private static final ConcurrentHashMap<Integer, List<SpanNearConfig>> SPAN_CACHE = new ConcurrentHashMap<>();

    static String cacheKeys() {
        return new TreeSet<>(SPAN_CACHE.keySet()).toString();
    }

    static void warmSpanCache(RelaxMode mode) {

        int[] keys = new int[]{4, 5, 6, 7, 8, 9, 10, 18, 20, 21, 22, 23, 24, 25, 65, 66, 75, 80, 82, 83, 84, 85, 87, 88, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100};
        for (int i : keys)
            configs(mode, i);

        if (SPAN_CACHE.size() != keys.length)
            throw new RuntimeException("cache size " + SPAN_CACHE.size() + " is different from length of keys " + keys.length);
    }

    synchronized static List<SpanNearConfig> configs(RelaxMode mode, int n) {

        if (SPAN_CACHE.containsKey(n)) return SPAN_CACHE.get(n);

        final List<SpanNearConfig> configs;

        switch (mode) {
            case Mode1:
                configs = mode1(n);
                break;
            case Mode2:
                configs = mode2(n);
                break;
            case Mode3:
                configs = mode3(n);
                break;
            default:
                throw new AssertionError(SpanNearConfig.class);
        }

        SPAN_CACHE.put(n, configs);
        return configs;
    }

    public enum RelaxMode {
        Mode1,
        Mode2,
        Mode3
    }


    private static int highEnd(int n) {
        if (n < 6)
            return n * 2; // n=5 use 10
        else if (n < 26)
            return (int) (n * 1.5); // for n=10 and n=25 use 15 and 37
        else
            return (int) (n * 1.25); // for n=100 use 125
    }

    private static int highSlop(int n) {
        if (n < 6)
            return n * 2; // for n=5 use 10
        else if (n < 26)
            return (int) (n * 0.5); // for n=10 and n=25 use 5 and 12
        else
            return (int) (n * 0.25); // for n=100 use 25
    }

    private static List<SpanNearConfig> mode1(int n) {
        List<SpanNearConfig> list = new ArrayList<>();

        SpanNearConfig config = new SpanNearConfig(0, n, true);

        int i = 0;
        while (config.slop < highSlop(n) || config.end < highEnd(n)) {

            for (boolean inOrder : new boolean[]{true, false}) {
                config.setInOrder(inOrder);
                try {
                    list.add((SpanNearConfig) config.clone());
                } catch (CloneNotSupportedException e) {
                    throw new RuntimeException(e);
                }

            }

            if (i % 2 == 0)
                config.slop++;
            else
                config.end++;

            i++;
        }
        return Collections.unmodifiableList(list);
    }


    private static List<SpanNearConfig> mode2(int n) {
        List<SpanNearConfig> list = new ArrayList<>();

        for (int slop = 0; slop < highSlop(n); slop++) {
            for (int end = n; end < highEnd(n); end++) {
                for (boolean inOrder : new boolean[]{true, false}) {
                    list.add(new SpanNearConfig(slop, end, inOrder));
                }
            }
        }

        Collections.sort(list);
        return Collections.unmodifiableList(list);
    }

    private static List<SpanNearConfig> mode3(int n) {
        List<SpanNearConfig> list = new ArrayList<>();

        for (int slop = 0; slop < highSlop(n); slop++) {
            for (int end = slop + n; end < highEnd(n) + slop; end++) {
                list.add(new SpanNearConfig(slop, end, true));
                list.add(new SpanNearConfig(slop, end, false));
            }
        }


        return Collections.unmodifiableList(list);
    }

    public static void main(String[] args) {

        Collections.emptySet().clear();
        int n = 25;
        System.out.println("==== Mode1 " + mode1(n).size());
        for (SpanNearConfig config : mode1(n)) {
            System.out.println(config + (config.tightest(n) ? "***" : ""));
        }

        System.out.println("==== Mode2 " + mode2(n).size());
        for (SpanNearConfig config : mode2(n)) {
            System.out.println(config + (config.tightest(n) ? "***" : ""));
        }

        System.out.println("==== Mode3 " + mode3(n).size());
        for (SpanNearConfig config : mode3(n)) {
            System.out.println(config + (config.tightest(n) ? "***" : ""));
        }
    }

    @Override
    public int compareTo(SpanNearConfig o) {
        return (this.slop - o.slop) + (this.end - o.end);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}



