

/* First created by JCasGen Mon Sep 15 15:04:26 EDT 2014 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.FSArray;


/** 
 * Updated by JCasGen Tue Sep 16 00:38:30 EDT 2014
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/TypeSystem.xml
 * @generated */
public class SemaforLayer extends ComponentAnnotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(SemaforLayer.class);
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
  protected SemaforLayer() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public SemaforLayer(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public SemaforLayer(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public SemaforLayer(JCas jcas, int begin, int end) {
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
  //* Feature: name

  /** getter for name - gets 
   * @generated
   * @return value of the feature 
   */
  public String getName() {
    if (SemaforLayer_Type.featOkTst && ((SemaforLayer_Type)jcasType).casFeat_name == null)
      jcasType.jcas.throwFeatMissing("name", "edu.cmu.cs.lti.script.type.SemaforLayer");
    return jcasType.ll_cas.ll_getStringValue(addr, ((SemaforLayer_Type)jcasType).casFeatCode_name);}
    
  /** setter for name - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setName(String v) {
    if (SemaforLayer_Type.featOkTst && ((SemaforLayer_Type)jcasType).casFeat_name == null)
      jcasType.jcas.throwFeatMissing("name", "edu.cmu.cs.lti.script.type.SemaforLayer");
    jcasType.ll_cas.ll_setStringValue(addr, ((SemaforLayer_Type)jcasType).casFeatCode_name, v);}    
   
    
  //*--------------*
  //* Feature: labels

  /** getter for labels - gets 
   * @generated
   * @return value of the feature 
   */
  public FSArray getLabels() {
    if (SemaforLayer_Type.featOkTst && ((SemaforLayer_Type)jcasType).casFeat_labels == null)
      jcasType.jcas.throwFeatMissing("labels", "edu.cmu.cs.lti.script.type.SemaforLayer");
    return (FSArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((SemaforLayer_Type)jcasType).casFeatCode_labels)));}
    
  /** setter for labels - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setLabels(FSArray v) {
    if (SemaforLayer_Type.featOkTst && ((SemaforLayer_Type)jcasType).casFeat_labels == null)
      jcasType.jcas.throwFeatMissing("labels", "edu.cmu.cs.lti.script.type.SemaforLayer");
    jcasType.ll_cas.ll_setRefValue(addr, ((SemaforLayer_Type)jcasType).casFeatCode_labels, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for labels - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public SemaforLabel getLabels(int i) {
    if (SemaforLayer_Type.featOkTst && ((SemaforLayer_Type)jcasType).casFeat_labels == null)
      jcasType.jcas.throwFeatMissing("labels", "edu.cmu.cs.lti.script.type.SemaforLayer");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((SemaforLayer_Type)jcasType).casFeatCode_labels), i);
    return (SemaforLabel)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((SemaforLayer_Type)jcasType).casFeatCode_labels), i)));}

  /** indexed setter for labels - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setLabels(int i, SemaforLabel v) { 
    if (SemaforLayer_Type.featOkTst && ((SemaforLayer_Type)jcasType).casFeat_labels == null)
      jcasType.jcas.throwFeatMissing("labels", "edu.cmu.cs.lti.script.type.SemaforLayer");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((SemaforLayer_Type)jcasType).casFeatCode_labels), i);
    jcasType.ll_cas.ll_setRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((SemaforLayer_Type)jcasType).casFeatCode_labels), i, jcasType.ll_cas.ll_getFSRef(v));}
   
    
  //*--------------*
  //* Feature: rank

  /** getter for rank - gets 
   * @generated
   * @return value of the feature 
   */
  public int getRank() {
    if (SemaforLayer_Type.featOkTst && ((SemaforLayer_Type)jcasType).casFeat_rank == null)
      jcasType.jcas.throwFeatMissing("rank", "edu.cmu.cs.lti.script.type.SemaforLayer");
    return jcasType.ll_cas.ll_getIntValue(addr, ((SemaforLayer_Type)jcasType).casFeatCode_rank);}
    
  /** setter for rank - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setRank(int v) {
    if (SemaforLayer_Type.featOkTst && ((SemaforLayer_Type)jcasType).casFeat_rank == null)
      jcasType.jcas.throwFeatMissing("rank", "edu.cmu.cs.lti.script.type.SemaforLayer");
    jcasType.ll_cas.ll_setIntValue(addr, ((SemaforLayer_Type)jcasType).casFeatCode_rank, v);}    
  }

    