

/* First created by JCasGen Sat Sep 13 18:23:45 EDT 2014 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.FSArray;


/** 
 * Updated by JCasGen Sun Sep 14 23:46:31 EDT 2014
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/TypeSystem.xml
 * @generated */
public class SemaforAnnotationSet extends ComponentAnnotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(SemaforAnnotationSet.class);
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
  protected SemaforAnnotationSet() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public SemaforAnnotationSet(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public SemaforAnnotationSet(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public SemaforAnnotationSet(JCas jcas, int begin, int end) {
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
  //* Feature: layers

  /** getter for layers - gets 
   * @generated
   * @return value of the feature 
   */
  public FSArray getLayers() {
    if (SemaforAnnotationSet_Type.featOkTst && ((SemaforAnnotationSet_Type)jcasType).casFeat_layers == null)
      jcasType.jcas.throwFeatMissing("layers", "edu.cmu.cs.lti.script.type.SemaforAnnotationSet");
    return (FSArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((SemaforAnnotationSet_Type)jcasType).casFeatCode_layers)));}
    
  /** setter for layers - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setLayers(FSArray v) {
    if (SemaforAnnotationSet_Type.featOkTst && ((SemaforAnnotationSet_Type)jcasType).casFeat_layers == null)
      jcasType.jcas.throwFeatMissing("layers", "edu.cmu.cs.lti.script.type.SemaforAnnotationSet");
    jcasType.ll_cas.ll_setRefValue(addr, ((SemaforAnnotationSet_Type)jcasType).casFeatCode_layers, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for layers - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public SemaforLayer getLayers(int i) {
    if (SemaforAnnotationSet_Type.featOkTst && ((SemaforAnnotationSet_Type)jcasType).casFeat_layers == null)
      jcasType.jcas.throwFeatMissing("layers", "edu.cmu.cs.lti.script.type.SemaforAnnotationSet");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((SemaforAnnotationSet_Type)jcasType).casFeatCode_layers), i);
    return (SemaforLayer)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((SemaforAnnotationSet_Type)jcasType).casFeatCode_layers), i)));}

  /** indexed setter for layers - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setLayers(int i, SemaforLayer v) { 
    if (SemaforAnnotationSet_Type.featOkTst && ((SemaforAnnotationSet_Type)jcasType).casFeat_layers == null)
      jcasType.jcas.throwFeatMissing("layers", "edu.cmu.cs.lti.script.type.SemaforAnnotationSet");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((SemaforAnnotationSet_Type)jcasType).casFeatCode_layers), i);
    jcasType.ll_cas.ll_setRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((SemaforAnnotationSet_Type)jcasType).casFeatCode_layers), i, jcasType.ll_cas.ll_getFSRef(v));}
   
    
  //*--------------*
  //* Feature: frameName

  /** getter for frameName - gets 
   * @generated
   * @return value of the feature 
   */
  public String getFrameName() {
    if (SemaforAnnotationSet_Type.featOkTst && ((SemaforAnnotationSet_Type)jcasType).casFeat_frameName == null)
      jcasType.jcas.throwFeatMissing("frameName", "edu.cmu.cs.lti.script.type.SemaforAnnotationSet");
    return jcasType.ll_cas.ll_getStringValue(addr, ((SemaforAnnotationSet_Type)jcasType).casFeatCode_frameName);}
    
  /** setter for frameName - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setFrameName(String v) {
    if (SemaforAnnotationSet_Type.featOkTst && ((SemaforAnnotationSet_Type)jcasType).casFeat_frameName == null)
      jcasType.jcas.throwFeatMissing("frameName", "edu.cmu.cs.lti.script.type.SemaforAnnotationSet");
    jcasType.ll_cas.ll_setStringValue(addr, ((SemaforAnnotationSet_Type)jcasType).casFeatCode_frameName, v);}    
  }

    