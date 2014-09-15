package edu.cmu.cs.lti.uima.model;

import org.apache.uima.jcas.cas.TOP;

public interface AnnotationCondition{
  public Boolean check(TOP aAnnotation);
}
