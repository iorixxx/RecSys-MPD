package edu.anadolu;

import java.util.Objects;

public class StringIntPair {

    final String string;
    final Integer integer;

    public Integer integer() {
        return this.integer;
    }

    @Override
    public String toString() {
        return "StringIntPair{" +
                "string='" + string + '\'' +
                ", integer=" + integer +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StringIntPair that = (StringIntPair) o;
        return Objects.equals(string, that.string) &&
                Objects.equals(integer, that.integer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(string, integer);
    }


    StringIntPair(String string, Integer integer) {
        this.string = string;
        this.integer = integer;
    }

}
