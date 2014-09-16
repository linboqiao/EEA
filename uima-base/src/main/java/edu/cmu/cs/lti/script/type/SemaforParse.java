

/* First created by JCasGen Mon Sep 15 15:04:26 EDT 2014 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;


/** 
 * Updated by JCasGen Tue Sep 16 00:38:30 EDT 2014
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/TypeSystem.xml
 * @generated */
public class SemaforParse extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(SemaforParse.class);
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int type = typeIndexID;
  /** @generated
   * @return index of the type  
   */
  @Override
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * @generated */
  protected SemaforParse() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public SemaforParse(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public SemaforParse(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public SemaforParse(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
    readObject();
  }   

  /** 
   * <!-- begin-user-doc -->
   * Write your own initialization here
   * <!-- end-user-doc -->
   *
   * @generated modifiable 
   */
  private void readObject() {/*default - does nothing empty block */}
     
 
    
  //*--------------*
  //* Feature: annotationSets

  /** getter for annotationSets - gets 
   * @generated
   * @return value of the feature 
   */
  public FSArray getAnnotationSets() {
    if (SemaforParse_Type.featOkTst && ((SemaforParse_Type)jcasType).casFeat_annotationSets == null)
      jcasType.jcas.throwFeatMissing("annotationSets", "edu.cmu.cs.lti.script.type.SemaforParse");
    return (FSArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((SemaforParse_Type)jcasType).casFeatCode_annotationSets)));}
    
  /** setter for annotationSets - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAnnotationSets(FSArray v) {
    if (SemaforParse_Type.featOkTst && ((SemaforParse_Type)jcasType).casFeat_annotationSets == null)
      jcasType.jcas.throwFeatMissing("annotationSets", "edu.cmu.cs.lti.script.type.SemaforParse");
    jcasType.ll_cas.ll_setRefValue(addr, ((SemaforParse_Type)jcasType).casFeatCode_annotationSets, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for annotationSets - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public SemaforAnnotationSet getAnnotationSets(int i) {
    if (SemaforParse_Type.featOkTst && ((SemaforParse_Type)jcasType).casFeat_annotationSets == null)
      jcasType.jcas.throwFeatMissing("annotationSets", "edu.cmu.cs.lti.script.type.SemaforParse");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((SemaforParse_Type)jcasType).casFeatCode_annotationSets), i);
    return (SemaforAnnotationSet)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((SemaforParse_Type)jcasType).casFeatCode_annotationSets), i)));}

  /** indexed setter for annotationSets - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setAnnotationSets(int i, SemaforAnnotationSet v) { 
    if (SemaforParse_Type.featOkTst && ((SemaforParse_Type)jcasType).casFeat_annotationSets == null)
      jcasType.jcas.throwFeatMissing("annotationSets", "edu.cmu.cs.lti.script.type.SemaforParse");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((SemaforParse_Type)jcasType).casFeatCode_annotationSets), i);
    jcasType.ll_cas.ll_setRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((SemaforParse_Type)jcasType).casFeatCode_annotationSets), i, jcasType.ll_cas.ll_getFSRef(v));}
  }

    