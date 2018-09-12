/**
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corp. 2007
 * All rights reserved.
 */

package edu.cmu.cs.lti.uima.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.EmptyFSList;
import org.apache.uima.jcas.cas.EmptyStringList;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.jcas.cas.NonEmptyStringList;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.cas.StringList;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * A set of static methods to help using UIMA under Java 1.5. Particularly
 * useful with import static.
 * 
 * @author duboue
 * 
 */
public class UimaJava5 {

	@SuppressWarnings("unchecked")
	public static <T extends Annotation> Iterator<T> annotationsIterator(
			JCas aJCas, int type) {
		return (Iterator<T>) aJCas.getAnnotationIndex(type).iterator();
	}

	public abstract static class UimaJava5Iterable<T> implements Iterable<T> {
	  @Override
	  public String toString() {
	    return makeList(this).toString();
	  }
	}
	
	// @SuppressWarnings("unchecked")
	// public static <T extends Annotation> Iterable<T> annotations(final JCas
	// aJCas, final int type)
	// {
	// return new Iterable<T>() {
	//
	// public Iterator<T> iterator() {
	// return aJCas.getAnnotationIndex(type).iterator();
	// }
	// };
	// }

	/**
	 * Allows for foreach constructs over JCases as follow: <br/>
	 * 
	 * for(SubTypeOfAnnotation
	 * annot:UimaJava5.annotations(jcas,SubTypeOfAnnotation.class)){...}
	 */
	public static <T extends Annotation> Iterable<T> annotations(
			final JCas aJCas, final Class<T> clazz) throws CASRuntimeException {
		try {
			final int type = clazz.getField("type").getInt(clazz);
			return new UimaJava5Iterable<T>() {

				@SuppressWarnings("unchecked")
				public Iterator<T> iterator() {
					return (Iterator<T>) aJCas.getAnnotationIndex(type).iterator();
				}
			};
		} catch (SecurityException e) {
			throw new CASRuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new CASRuntimeException(e);
		} catch (NoSuchFieldException e) {
			throw new CASRuntimeException(e);
		}
	}
  
  /**
   * Allows for foreach constructs for subiterators over JCases as follows:
   * 
   * for(SubTypeOfAnnotation
   * annot:UimaJava5.subAnnotations(jcas,SubTypeOfAnnotation.class,containingAnnotation)){...}
   * 
   * This will iterate over all annotations whose span is contained in or exactly the same as
   * the span of containingAnnotation.  Note this does not use type priorities in the case of
   * identical spans - all annotations having the same span as containingAnnotation are included.
   */
  public static <T extends Annotation> Iterable<T> subAnnotations(
      final JCas aJCas, final Class<T> clazz, final Annotation containingAnnotation) throws CASRuntimeException {
    try {
      final int type = clazz.getField("type").getInt(clazz);
      return new UimaJava5Iterable<T>() {
        @SuppressWarnings("unchecked")
        public Iterator<T> iterator() {
          return new Iterator<T>() {
            private FSIterator fsIter;
            
            {
              fsIter = aJCas.getAnnotationIndex(type).iterator();
              UimaConvenience.moveBeforeSpan(fsIter, containingAnnotation);
            }
            
            public boolean hasNext() {
              //skip annotations whose end position is outside the span
              while (fsIter.isValid() && ((Annotation)fsIter.get()).getEnd() > containingAnnotation.getEnd()) {
                fsIter.moveToNext();
              }
              //if we're at the end, or if we reach an annotation whose begin position is after the end of
              //the containingAnnotation, we're done
              if (!fsIter.isValid()) {
                return false;
              }
              if (((Annotation)fsIter.get()).getBegin() > containingAnnotation.getEnd()) {
                return false;
              }
              return true;
            }
            
            public T next() {
              if (!hasNext()) {
                throw new NoSuchElementException();
              }
              return (T)fsIter.next();
            }

            public void remove() {
              throw new UnsupportedOperationException();              
            }
          };
        }        
      };
    } catch (SecurityException e) {
      throw new CASRuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new CASRuntimeException(e);
    } catch (NoSuchFieldException e) {
      throw new CASRuntimeException(e);
    }
  }  

	/**
	 * Allows for foreach constructs over JCases as follow: <br/>
	 * 
	 * for(SubTypeOfTOP annot:UimaJava5.indexedFs(jcas,SubTypeOfTOP.class)){...}
	 */
	public static <T extends TOP> Iterable<T> indexedFs(final JCas aJCas,
			final Class<T> clazz) throws CASRuntimeException {
		try {
			final int type = clazz.getField("type").getInt(clazz);
			return new UimaJava5Iterable<T>() {

				@SuppressWarnings("unchecked")
				public Iterator<T> iterator() {
					return (Iterator<T>)aJCas.getJFSIndexRepository().getAllIndexedFS(type);
				}
			};
		} catch (SecurityException e) {
			throw new CASRuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new CASRuntimeException(e);
		} catch (NoSuchFieldException e) {
			throw new CASRuntimeException(e);
		}
	}

	/**
	 * Gets an instance of a JCas type that's expected to be a singleton. If
	 * there are no instances of the specified type, null is returned. If there
	 * is exactly one instance, that instance is returned. If there is more than
	 * one instance, an IllegalStateException is thrown
	 * 
	 * @param aJCas
	 *            JCAS instance to retrieve the instance from
	 * @param clazz
	 *            JCas class to get an instance of
	 * @return the singleton instance of the specified class
	 * @throws IllegalStateException
	 *             if there is more than one instance of the specified class
	 */
	public static <T extends TOP> T getSingleton(JCas aJCas, Class<T> clazz) {
		try {
			int type = clazz.getField("type").getInt(clazz);
			Iterator fsIter = aJCas.getJFSIndexRepository().getAllIndexedFS(
					type);
			if (!fsIter.hasNext()) {
				return null;
			}

			@SuppressWarnings("unchecked")
			T instance = (T) fsIter.next();

			if (fsIter.hasNext()) {
				throw new IllegalStateException(
						"More than one instance of expected singleton type "
								+ clazz.getSimpleName());
			}
			return instance;
		} catch (SecurityException e) {
			throw new CASRuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new CASRuntimeException(e);
		} catch (NoSuchFieldException e) {
			throw new CASRuntimeException(e);
		}
	}

	/**
	 * Takes a Java list containing UIMA FeatureStructures, and creates an FS
	 * List in a CAS.
	 * 
	 * @param list
	 *            list containing Feature Structures (subclasses of TOP)
	 * @param jcas
	 *            jcas in which to create the FSList
	 * @return the FSList
	 */
	public static FSList makeFsList(List<? extends TOP> list, JCas jcas) {
		return makeFsList(list.iterator(), jcas);
	}

	private static FSList makeFsList(Iterator<? extends TOP> iterator, JCas jcas) {
		if (!iterator.hasNext()) {
			return new EmptyFSList(jcas);
		} else {
			TOP head = iterator.next();
			FSList tail = makeFsList(iterator, jcas);
			NonEmptyFSList list = new NonEmptyFSList(jcas);
			list.setHead(head);
			list.setTail(tail);
			return list;
		}
	}

	/**
	 * Return a Java List with the FeatureStructure objects from the given
	 * FSList.
	 * 
	 * @param fsList
	 *            the input FSList of FeatureStructures
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T extends TOP> List<T> makeList(FSList fsList) {
		List list = new ArrayList<T>();

		if (fsList != null)
			while (fsList instanceof NonEmptyFSList) {
				NonEmptyFSList item = (NonEmptyFSList) fsList;
				list.add((T) item.getHead());
				fsList = item.getTail();
			}

		return list;
	}

	/**
	 * Returns the number of elements in the given FSList.
	 * 
	 * @param fsList
	 *            the given FSList
	 * @return the number of elements in the given FSList
	 */
	public static int fsListSize(FSList fsList) {
		int size = 0;
		if (fsList != null)
			while (fsList instanceof NonEmptyFSList) {
				NonEmptyFSList item = (NonEmptyFSList) fsList;
				size++;
				fsList = item.getTail();
			}
		return size;
	}

	/**
	 * Allows for foreach constructs over FSLists as follow: <br/>
	 * 
	 * for(SubTypeOfTOP fs : UimaJava5.fsListIterable(fsList,
	 * SubTypeOfTop.class)){...}
	 */
	public static <T extends TOP> Iterable<T> fsListIterable(
			final FSList aFsList, Class<T> aExpectedElementClass)
			throws CASRuntimeException {
		return new UimaJava5Iterable<T>() {

			@SuppressWarnings("unchecked")
			public Iterator<T> iterator() {
				return new FsListIterator(aFsList);
			}
		};
	}

	static class FsListIterator<E> implements Iterator<E> {
		private FSList currentFsListNode;

		public FsListIterator(FSList aFsList) {
			currentFsListNode = aFsList;
		}

		public boolean hasNext() {
			return (currentFsListNode instanceof NonEmptyFSList);
		}

		@SuppressWarnings("unchecked")
		public E next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			NonEmptyFSList neList = (NonEmptyFSList) currentFsListNode;
			currentFsListNode = neList.getTail();
			return (E) neList.getHead();
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

	}

	/**
	 * Takes a Java list containing Strings, and creates a String List in a CAS.
	 * 
	 * @param list
	 *            list containing Strings
	 * @param jcas
	 *            jcas in which to create the StringList
	 * @return the StringList
	 */
	public static StringList makeStringList(List<String> list, JCas jcas) {
		return makeStringList(list.iterator(), jcas);
	}

	private static StringList makeStringList(Iterator<String> iterator,
			JCas jcas) {
		if (!iterator.hasNext()) {
			return new EmptyStringList(jcas);
		} else {
			String head = iterator.next();
			StringList tail = makeStringList(iterator, jcas);
			NonEmptyStringList list = new NonEmptyStringList(jcas);
			list.setHead(head);
			list.setTail(tail);
			return list;
		}
	}

	/**
	 * Return a Java List with the strings from the given StringList.
	 * 
	 * @param stringList
	 *            the input StringList
	 * @return
	 */
	public static List<String> makeList(StringList stringList) {
		List<String> list = new ArrayList<String>();

		if (stringList != null)
			while (stringList instanceof NonEmptyStringList) {
				NonEmptyStringList item = (NonEmptyStringList) stringList;
				list.add(item.getHead());
				stringList = item.getTail();
			}

		return list;
	}

	/**
	 * Utility method to convert iterables such as those returned by
	 * {@link #indexedFs(JCas, Class)}, {@link #annotations(JCas, Class)}, and
	 * {@link #fsListIterable(FSList, Class)} into lists. This is relatively
	 * slow since (unlike the aforementioned methods) it has to step through the
	 * whole index.
	 */
	public static <T> List<T> makeList(Iterable<T> i) {
		List<T> retval = new ArrayList<T>();
		for (T t : i)
			retval.add(t);
		return retval;
	}

  /**
   * Returns an Annotation of a specified type that has the same span as a given annotation.
   *  
   * @param jcas the JCas view to operate on
   * @param annotation an input annotation
   * @param clazz class of the output annotation
   * @return an Annotation of class <code>clazz</code> that has the same span as <code>annotation</code>, or
   *   null if no such Annotation exists.
   */
  @SuppressWarnings("unchecked")
  public static <T extends Annotation> T getCoterminousAnnotation(JCas jcas, Annotation annotation, Class<T> clazz) {
    int type;
    try {
      type = clazz.getField("type").getInt(clazz);
    }catch (SecurityException e) {
      throw new CASRuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new CASRuntimeException(e);
    } catch (NoSuchFieldException e) {
      throw new CASRuntimeException(e);
    }
    FSIterator iterator = jcas.getAnnotationIndex(type).iterator();
    UimaConvenience.moveBeforeSpan(iterator, annotation);
    if (iterator.hasNext()) {
      T result = (T)iterator.next();
      if (result.getBegin() == annotation.getBegin() && result.getEnd() == annotation.getEnd()) {
        return result;
      }
    }
    return null;
  }
  
  /**
   * Return a Java List with the strings from the given StringArray.
   * 
   * @param stringArray
   *            the input StringArray
   * @return
   */
  
  public static List<String> makeList(StringArray stringArray) {
    List<String> list = new ArrayList<String>();

    for (int i = 0; i < stringArray.size(); i++)
      list.add(stringArray.get(i));

    return list;
  }
}
