package edu.cmu.cs.lti.uima.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class Span implements Comparable<Span> {
  private int begin;

  private int end;

  public Span(int begin, int end) {
    if (begin > end) {
      throw new IllegalArgumentException("Begin index must be not larger than end index!");
    }

    this.begin = begin;
    this.end = end;
  }

  public int getBegin() {
    return begin;
  }

  public int getEnd() {
    return end;
  }

  public boolean isEmpty() {
    return (begin == end);
  }

  public boolean equals(Object object) {
    if (object instanceof Span) {
      Span that = (Span) object;
      return this.begin == that.begin && this.end == that.end;
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    return String.format("SPAN: [%d,%d]", begin, end);
  }

  public double checkOverlap(Span that) {
    if (that.begin >= end || that.end <= begin) {
      return 0.0; // no overlap
    } else {
      int[] points = { this.begin, this.end, that.begin, that.end };
      Arrays.sort(points);
      if (points[3] == points[0])
        return 1.0; // these are actually two points, not two spans
      else
        return (points[2] - points[1]) / (points[3] - points[0]);
    }
  }

  public boolean covers(Span that) {
    if (this.begin <= that.begin && this.end >= that.end) {
      return true;
    }
    return false;
  }

  public int hashCode() {
    return Arrays.hashCode(new int[] { this.begin, this.end });
  }

  public int getDistance(Span that) {
    if (checkOverlap(that) > 0) {
      return -1; // if overlap
    } else {
      return Math.max(that.begin - this.end, this.begin - that.end);
    }
  }

  public int compareTo(Span thatSpan) {
    if (this.begin < thatSpan.begin) {
      return 1;
    } else if (this.begin == thatSpan.begin) {
      if (this.end < thatSpan.end) {
        return 1;
      } else if (this.end == thatSpan.end) {
        return 0;
      } else {
        return -1;
      }
    } else {
      return -1;
    }
  }
}
