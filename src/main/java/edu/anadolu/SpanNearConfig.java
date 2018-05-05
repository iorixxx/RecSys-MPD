package edu.anadolu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class SpanNearConfig implements Comparable<SpanNearConfig>, Cloneable {

    int slop = 0;
    int end = 0;
    boolean inOrder = true;

    private SpanNearConfig() {
        this(0, 0, true);
    }

    private SpanNearConfig(int slop, int end, boolean inOrder) {
        this.slop = slop;
        this.end = end;
        this.inOrder = inOrder;
    }

    private void setInOrder(boolean inOrder) {
        this.inOrder = inOrder;
    }

    boolean tightest() {
        return this.inOrder && 0 == this.end && 0 == this.slop;
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

    static final List<SpanNearConfig> mode1;
    static final List<SpanNearConfig> mode2;

    static {
        mode1 = mode1(5);
        mode2 = mode2(5);
    }

    enum RelaxMode {
        Mode1,
        Mode2,
    }

    static List<SpanNearConfig> mode1(int end) {
        List<SpanNearConfig> list = new ArrayList<>();

        SpanNearConfig config = new SpanNearConfig(0, end, true);

        int i = 0;
        while (config.slop < 20 || config.end < 20) {

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


    static List<SpanNearConfig> mode2(int n) {
        List<SpanNearConfig> list = new ArrayList<>();

        for (int slop = 0; slop < 20; slop++) {
            for (int end = n; end < 20; end++) {
                for (boolean inOrder : new boolean[]{true, false}) {
                    list.add(new SpanNearConfig(slop, end, inOrder));
                }
            }
        }

        Collections.sort(list);
        return Collections.unmodifiableList(list);
    }

    static List<SpanNearConfig> mode3(int end) {
        List<SpanNearConfig> list = new ArrayList<>();

        for (int slop = 0; slop < 20; slop++) {
            for (boolean inOrder : new boolean[]{true, false}) {
                list.add(new SpanNearConfig(slop, end, inOrder));
            }
        }

        Collections.sort(list);
        return Collections.unmodifiableList(list);
    }

    public static void main(String[] args) {


        System.out.println("==== Mode1 " + mode1.size());
        for (SpanNearConfig config : mode1) {
            System.out.println((config.tightest() ? "***" : "") + config);
        }

        System.out.println("==== Mode2 " + mode2.size());
        for (SpanNearConfig config : mode2) {
            System.out.println((config.tightest() ? "***" : "") + config);
        }

        System.out.println("==== Mode3");
        for (SpanNearConfig config : mode3(5)) {
            System.out.println((config.tightest() ? "***" : "") + config);
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



