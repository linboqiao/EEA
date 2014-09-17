/**
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corp. 2007
 * All rights reserved.
 */
package edu.cmu.cs.lti.uima.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.NonEmptyStringList;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.cas.StringList;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;

import com.google.common.reflect.TypeToken;

import edu.cmu.cs.lti.script.type.ComponentAnnotation;

/**
 * @author Chris Welty
 * 
 *         Required JAR files:
 *         <ul>
 *         </ul>
 * 
 *         Version History:
 *         <ul>
 *         <li>0.1 [Apr 20, 2004]: Created.
 *         </ul>
 * 
 * @version: 0.1
 */
public class UimaConvenience extends BasicConvenience {

  public static String getDocId(CAS cas) {
    return getDocId(cas, true);
  }

  // public static String getDocId(TCAS tcas) {
  // return getDocId(tcas, true);
  // }
  //
  // public static String getDocId(TCAS tcas, boolean appendOffset) {
  // try {
  // return getDocId(tcas.getJCas(), appendOffset);
  // } catch (CASException e) {
  // return "doc" + docIdCount++;
  // }
  // }

  public static String getDocId(CAS cas, boolean appendOffset) {
    try {
      return getDocId(cas.getJCas(), appendOffset);
    } catch (CASException e) {
      return null;
    }
  }

  public static String getDocId(JCas aJCas) {
    return getDocId(aJCas, true);
  }

  /**
   * Add a single element to (the end of) an existing FSArray
   * 
   * @param jcas
   * @param array
   * @param ann
   * @return
   */
  public static FSArray updateArray(JCas jcas, FSArray array, FeatureStructure ann) {
    FSArray arr;
    if (array == null) {
      arr = new FSArray(jcas, 1);
    } else {
      arr = new FSArray(jcas, array.size() + 1);
      for (int i = 0; i < array.size(); i++)
        arr.set(i, array.get(i));
    }
    arr.set(arr.size() - 1, ann);
    return arr;
  }

  /**
   * Takes a Java list containing UIMA FeatureStructures, and creates an FSArray
   * 
   * @param list
   *          list containing Feature Structures (subclasses of TOP)
   * @param jcas
   *          jcas in which to create the FSArray
   * @return the FSArray
   */
  public static FSArray makeFsArray(List<? extends TOP> list, JCas jcas) {
    FSArray rtn = new FSArray(jcas, list.size());
    int i = 0;
    for (FeatureStructure item : list) {
      rtn.set(i, item);
      i++;
    }
    return rtn;
  }

  /**
   * Return Doc ID without including file path. (for backward-compatibility)
   */
  public static String getDocId(JCas aJCas, boolean appendOffset) {
    return getDocId(aJCas, appendOffset, false);
  }

  /**
   * @param aJCas
   * @param appendOffset
   * @param includeFullUri
   *          whether to include a converted form of filepath in document name.
   * @return
   */
  public static String getDocId(JCas aJCas, boolean appendOffset, boolean includeFullUri) {
    try {
      aJCas = getInitialView(aJCas);
    } catch (CASException e) {
      return null;
    }
    AnnotationIndex<Annotation> docIndex = aJCas.getJFSIndexRepository().getAnnotationIndex(
            SourceDocumentInformation.typeIndexID);
    FSIterator<Annotation> dociterator = docIndex.iterator();

    if (dociterator.isValid()) {
      SourceDocumentInformation meta = (SourceDocumentInformation) dociterator.get();
      String uri = meta.getUri();
      int offset = meta.getOffsetInSource();
      return getDocId(uri, appendOffset ? offset : 0, includeFullUri);
    } else
      return null;
  }

  public static String getDocId(String uri, int offset) {
    return getDocId(uri, offset, false);
  }

  public static String getDocId(String uri, int offset, boolean includeFullUri) {
    String id;
    if (includeFullUri) {
      id = uri.replace('/', '-').replace(':', '_');
    } else {
      id = removeFilePath(uri);
    }
    if (offset != 0)
      id = id + "-" + offset;
    return id;
  }

  public static String getDocumentLocation(JCas aJCas) {
    return getDocId(aJCas, false);
  }

  /**
   * Find the "real" _InitialView Can't simply get it by name as it may have been sofa mapped to
   * another view
   * 
   * @param jcas
   * @returns a view of the real, unmapped _InitialView
   * @throws CASException
   */

  private static JCas getInitialView(JCas jcas) throws CASException {
    FSIterator<SofaFS> sofaIter = jcas.getSofaIterator();
    JCas view = null;
    while (sofaIter.hasNext()) {
      SofaFS sofa = (SofaFS) sofaIter.next();
      view = jcas.getView(sofa);
      if (view.getViewName().equals("_InitialView")) {
        return view;
      }
    }
    return null;
  }

  /**
   * Returns the specified view from the specified JCas.
   * 
   * @param aJCas
   * @param viewName
   * @return
   */
  public static JCas getView(JCas aJCas, String viewName) {
    JCas view = null;
    try {
      view = aJCas.getView(viewName);
    } catch (CASException e) {
      System.err.println("Invalid view name: " + viewName);
      System.exit(1);
    }

    return view;
  }

  /**
   * @deprecated We no longer have this info in standard metadata
   * @param aJCas
   * @return
   */
  public static String getDocumentDate(JCas aJCas) {
    FSIndex docIndex = aJCas.getJFSIndexRepository().getAnnotationIndex(
            SourceDocumentInformation.typeIndexID);
    FSIterator dociterator = null;

    try {
      dociterator = docIndex.iterator();
    } catch (NullPointerException e) {
      return "";
    }

    if (dociterator.isValid()) {
      // SourceDocumentInformation meta = (SourceDocumentInformation)
      // dociterator
      // .get();
      // return meta.getDate();
      return "";
    }
    return "";
  }

  public static List<String> casStringListToList(StringList sl) {
    LinkedList<String> retval = new LinkedList<String>();
    while (sl instanceof NonEmptyStringList) {
      NonEmptyStringList nesl = (NonEmptyStringList) sl;
      retval.add(nesl.getHead());
      sl = nesl.getTail();
    }
    return retval;
  }

  public static boolean stringArrayEquals(StringArray s1, StringArray s2) {
    if (s1 == null || s2 == null)
      return s1 == s2;
    else if (s1.size() != s2.size())
      return false;
    for (int i = 0; i < s1.size(); i++) {
      String str1 = (String) s1.get(i);
      String str2 = (String) s2.get(i);
      if (!str1.equals(str2))
        return false;
    }
    return true;
  }

  /**
   * Check annotations for overlapping spans
   * 
   * @param a1
   * @param a2
   * @return True if a1 and a2 spans overlap, false otherwise.
   */
  public static boolean isOverlapping(Annotation a1, Annotation a2) {
    return !(a1.getBegin() >= a2.getEnd() || a2.getBegin() >= a1.getEnd());
  }

  /**
   * Checks whether a1 covers a2.
   * 
   * @param a1
   * @param a2
   * @return true if a1 covers a2; false otherwise.
   */
  public static boolean isCovered(Annotation a1, Annotation a2) {
    return (a1.getBegin() <= a2.getBegin() && a1.getEnd() >= a2.getEnd());
  }

  /**
   * Check whether or not a given CAS has a SofA defined. This method uses an efficient (i.e.,
   * exception free) approach that relies heavily in UIMA-internals. It should be changed when this
   * functionality is available inside UIMA.
   * 
   * @param cas
   *          The CAS which sofa might be defined
   * @param targetSofa
   *          the sofa name
   * @return true is targetSofa is defined
   */
  public static boolean hasSofa(CAS cas, String targetSofa) {
    FSIterator sofaIterator = cas.getSofaIterator();

    // int sofaIdFeatCode = ((CASImpl) cas).ll_getTypeSystem()
    // .ll_getCodeForFeature(
    // cas.getTypeSystem().getType(CAS.TYPE_NAME_SOFA)
    // .getFeatureByBaseName(
    // CAS.FEATURE_BASE_NAME_SOFAID));

    while (sofaIterator.isValid()) {
      SofaFS sofa = (SofaFS) sofaIterator.get();
      if (targetSofa.equals(cas.getView(sofa).getViewName()))
        // ((CASImpl) cas).getStringValue(sofa
        // .hashCode(), sofaIdFeatCode)))
        return true; // already exists

      sofaIterator.moveToNext();
    }

    return false;
  }

  public static boolean isInvalid(int begin, int end, JCas jcas) {
    return (begin >= end) || (begin < 0) || (end > jcas.getDocumentText().length());
  }

  public static boolean isInvalid(Annotation a, JCas jcas) {
    return (a.getBegin() >= a.getEnd() || (a.getBegin() < 0) || (a.getEnd() > jcas
            .getDocumentText().length()));
  }

  /**
   * Identifies the number of annotations of a given type in the specified JCas.
   * 
   * @param aJCas
   * @param clazz
   * @return
   * @throws CASRuntimeException
   */
  public static int numberOfAnnotations(final JCas aJCas, final Class clazz)
          throws CASRuntimeException {
    try {
      final int type = clazz.getField("type").getInt(clazz);
      return aJCas.getAnnotationIndex(type).size();
    } catch (SecurityException e) {
      throw new CASRuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new CASRuntimeException(e);
    } catch (NoSuchFieldException e) {
      throw new CASRuntimeException(e);
    }
  }

  /**
   * Moves an FSIterator so it is just before the given target span. That is, the next annotation
   * returned from i.next() (or i.get()) will be the first one in the CAS with
   * <code>begin >= targetSpan.begin</code> and <code>end &lt;= targetSpan.end</code>.
   * 
   * @param i
   *          an iterator
   * @param targetSpan
   *          annotation representing the target span to move the iterator before
   */
  public static void moveBeforeSpan(FSIterator i, AnnotationFS targetSpan) {
    i.moveTo(targetSpan);
    if (!i.isValid()) {
      i.moveToLast();
    }
    while (true) {
      if (!i.isValid()) {
        i.moveToFirst();
        return;
      }
      AnnotationFS annot = (AnnotationFS) i.get();
      if (annot.getBegin() < targetSpan.getBegin()
              || (annot.getBegin() == targetSpan.getBegin() && annot.getEnd() > targetSpan.getEnd())) {
        i.moveToNext();
        return;
      }
      i.moveToPrevious();
    }
  }

  /**
   * Returns a list of annotations of the specified type in the specified CAS. The difference of
   * this with JCasUtil.select is that this method will store everything into a list first, so if
   * modifying stuff, it won't raise ConcurrentModificationException
   * 
   * @param aJCas
   * @param clazz
   * @return a list of annotations of the specified type in the specified CAS
   */
  public static <T extends TOP> List<T> getAnnotationList(JCas aJCas, final Class<T> clazz) {
    try {
      // TODO: The integer field 'type' and 'typeIndexID' take the same
      // value as those of its parent
      // type. We need to fix the code below so it gets only the specified
      // annotation.
      final int type = clazz.getField("type").getInt(clazz);

      List<T> annotationList = new ArrayList<T>();
      Iterator<?> annotationIter = aJCas.getJFSIndexRepository().getAllIndexedFS(type);
      while (annotationIter.hasNext()) {
        T annotation = (T) annotationIter.next();
        annotationList.add(annotation);
      }
      return annotationList;
    } catch (SecurityException e) {
      throw new CASRuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new CASRuntimeException(e);
    } catch (NoSuchFieldException e) {
      throw new CASRuntimeException(e);
    }
  }

  /**
   * Returns a FSList with the newItem appended at the end
   * 
   * @param aJCas
   * @param oldFSList
   * @param newItem
   * @param clazz
   * @return
   */
  public static <T extends TOP> FSList appendFSList(JCas aJCas, FSList oldFSList, T newItem,
          Class<T> clazz) {
    List<T> newList;
    if (oldFSList != null) {
      newList = new ArrayList<T>(FSCollectionFactory.create(oldFSList, clazz));
    } else {
      newList = new ArrayList<T>();
    }
    newList.add(newItem);
    return FSCollectionFactory.createFSList(aJCas, newList);
  }

  public static <T extends TOP> FSList replaceFSList(JCas aJCas, FSList oldFSList, T oldItem,
          T newItem, Class<T> clazz) {
    List<T> newList = new ArrayList<T>();
    if (oldFSList != null) {
      List<T> oldList = new ArrayList<T>(FSCollectionFactory.create(oldFSList, clazz));
      for (T item : oldList) {
        if (!item.equals(oldItem)) {
          newList.add(item);
        }
      }
    }
    newList.add(newItem);
    return FSCollectionFactory.createFSList(aJCas, newList);
  }

  /**
   * Appends all element from a collection to the FSList
   * 
   * @param aJCas
   * @param oldFSList
   * @param newItems
   * @param clazz
   * @return
   */
  public static <T extends TOP> FSList appendAllFSList(JCas aJCas, FSList oldFSList,
          Collection<T> newItems, Class<T> clazz) {
    List<T> newList;

    if (oldFSList != null) {
      newList = new ArrayList<T>(FSCollectionFactory.create(oldFSList, clazz));
    } else {
      newList = new ArrayList<T>();
    }

    for (T item : newItems) {
      newList.add(item);
    }

    return FSCollectionFactory.createFSList(aJCas, newList);
  }

  public static <T extends TOP> FSList mergeFSList(JCas aJCas, FSList fsList1, FSList fsList2,
          Class<T> clazz) {
    if (fsList2 != null) {
      return appendAllFSList(aJCas, fsList1, FSCollectionFactory.create(fsList2, clazz), clazz);
    } else {
      return appendAllFSList(aJCas, fsList1, new ArrayList<T>(), clazz);
    }
  }

  /**
   * Returns a FSList with the specific item removed.
   * 
   * @param aJCas
   * @param oldFSList
   * @param itemToRemove
   * @param clazz
   * @return
   */
  public static <T extends TOP> FSList removeFromFSList(JCas aJCas, FSList oldFSList,
          T itemToRemove, Class<T> clazz) {
    List<T> newList;
    if (oldFSList != null) {
      newList = new ArrayList<T>();
      for (T item : FSCollectionFactory.create(oldFSList, clazz)) {
        if (!item.equals(itemToRemove))
          newList.add(item);
      }
      return FSCollectionFactory.createFSList(aJCas, newList);
    } else {
      return null;
    }
  }

  /**
   * Returns the annotation with the same range as the specified target annotation.
   * 
   * @param aJCas
   * @param annotationList
   * @param targetAnnotation
   * @return the annotation with the same range as the specified target annotation
   */
  public static <T, U extends Annotation> T getAnnotationWithSameRange(JCas aJCas,
          List<T> annotationList, U targetAnnotation) {
    if (annotationList == null || annotationList.isEmpty()) {
      return null;
    }

    int targetBegin = ((Annotation) targetAnnotation).getBegin();
    int targetEnd = ((Annotation) targetAnnotation).getEnd();

    for (T ann : annotationList) {
      int begin = ((Annotation) ann).getBegin();
      int end = ((Annotation) ann).getEnd();

      if (targetBegin == begin && targetEnd == end) {
        // The target annotation has the same region as this annotation.
        return ann;
      }
    }

    return null;
  }

  public static <T extends TOP, U extends Annotation> T getAnnotationWithMinRange(JCas aJCas,
          Class<T> clazz, U targetAnnotation) {
    List<T> annotationList = getAnnotationList(aJCas, clazz);
    return getAnnotationWithMinRange(aJCas, annotationList, targetAnnotation);
  }

  /**
   * Returns the annotation that surrounds the target annotation with the minimum range.
   * 
   * @param aJCas
   * @param annotationList
   * @param targetAnnotation
   * @return the annotation that surrounds the target annotation with the minimum range
   */
  public static <T, U extends Annotation> T getAnnotationWithMinRange(JCas aJCas,
          List<T> annotationList, U targetAnnotation) {
    if (annotationList == null || annotationList.isEmpty()) {
      return null;
    }

    int targetBegin = ((Annotation) targetAnnotation).getBegin();
    int targetEnd = ((Annotation) targetAnnotation).getEnd();

    int minRange = Integer.MAX_VALUE;
    T annotationWithMinRange = null;
    for (T ann : annotationList) {
      int begin = ((Annotation) ann).getBegin();
      int end = ((Annotation) ann).getEnd();

      if (targetEnd < begin || targetBegin > end) {
        // The target annotation is not in this annotation.
        continue;
      }

      int range = end - begin;
      if (range < minRange) {
        minRange = range;
        annotationWithMinRange = ann;
      }
    }

    return annotationWithMinRange;
  }

  /**
   * Returns the annotation of the specified type which is closest to the specified annotation
   * within the specified region.
   * 
   * @param aJCas
   * @param targetAnnotation
   * @param type
   * @param begin
   * @param end
   * @return the annotation of the specified type which is closest to the specified annotation
   *         within the specified region
   */
  public static <T, U extends Annotation> U getClosestAnnotation(JCas aJCas, T targetAnnotation,
          Class<U> type, int begin, int end) {
    List<U> candidates = JCasUtil.selectCovered(aJCas, type, begin, end);
    if (candidates == null) {
      return null;
    }

    U closestAnn = null;
    double minDistance = Double.MAX_VALUE;
    int targetBegin = ((Annotation) targetAnnotation).getBegin();
    for (U candidate : candidates) {
      int candidateBegin = candidate.getBegin();
      double distance = Math.abs(targetBegin - candidateBegin);
      if (distance < minDistance) {
        closestAnn = candidate;
        minDistance = distance;
      }
    }

    return closestAnn;
  }

  /**
   * Returns true if the specified annotation covers a valid region of text; false otherwise.
   * 
   * @param annotation
   * @return true if the specified annotation covers a valid region of text; false otherwise
   */
  public static <T extends Annotation> boolean coversText(T annotation) {
    int begin = annotation.getBegin();
    int end = annotation.getEnd();
    if (begin <= 0 || end <= 0) {
      return false;
    }

    return true;
  }

  /**
   * Select the first annotation covered
   * 
   * @param aJCas
   * @param anno
   * @param clazz
   * @return
   */
  public static <T extends Annotation> T selectCoveredFirst(JCas aJCas, Annotation anno,
          Class<T> clazz) {
    List<T> covers = JCasUtil.selectCovered(aJCas, clazz, anno);
    if (covers.isEmpty())
      return null;

    return JCasUtil.selectCovered(aJCas, clazz, anno).get(0);
  }

  /**
   * Select the first annotation covered
   * 
   * @param aJCas
   * @param anno
   * @param clazz
   * @return
   */
  public static <T extends Annotation> T selectCoveredFirst(JCas aJCas, int begin, int end,
          Class<T> clazz) {
    List<T> covers = JCasUtil.selectCovered(aJCas, clazz, begin, end);
    if (covers.isEmpty())
      return null;

    return covers.get(0);
  }

  public static <K extends Annotation, T extends Annotation> Map<K, Collection<T>> indexCoveredAndCovering(
          JCas aJCas, Class<K> keyType, Class<T> valueType) {
    Map<K, Collection<T>> index = new HashMap<K, Collection<T>>();

    Map<K, Collection<T>> covered = JCasUtil.indexCovered(aJCas, keyType, valueType);

    Map<K, Collection<T>> covering = JCasUtil.indexCovering(aJCas, keyType, valueType);

    index.putAll(covered);
    index.putAll(covering);

    return covered;
  }

  /**
   * Tests whether the specified two annotations are the same.
   * 
   * @param ann1
   * @param ann2
   * @return True if the specified two annotations are the same; false otherwise.
   */
  public static <T extends TOP> boolean isSameAnnotation(T ann1, T ann2) {
    if (ann1.getAddress() == ann2.getAddress()) {
      return true;
    }
    return false;
  }

  public static <T extends TOP> List<T> convertFSListToList(FSList fslist, Class<T> clazz) {
    return new ArrayList<T>(FSCollectionFactory.create(fslist, clazz));
  }

  public static void printProcessLog(JCas aJCas) {
    String fileName = getShortDocumentNameWithOffset(aJCas);
    System.out.println(String.format("Processing article: %s", fileName));
  }

  public static String getShortDocumentName(JCas aJCas) {
    SourceDocumentInformation srcDocInfo = JCasUtil.selectSingle(aJCas,
            SourceDocumentInformation.class);
    if (srcDocInfo == null) {
      return null;
    }
    return FilenameUtils.getBaseName(srcDocInfo.getUri());
  }

  public static String getShortDocumentNameWithOffset(JCas aJCas) {
    SourceDocumentInformation srcDocInfo = JCasUtil.selectSingle(aJCas,
            SourceDocumentInformation.class);
    if (srcDocInfo == null) {
      return null;
    }
    return FilenameUtils.getBaseName(srcDocInfo.getUri()) + "_" + srcDocInfo.getOffsetInSource();
  }
}
