

/* First created by JCasGen Sat Sep 13 18:23:45 EDT 2014 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.FSArray;


import org.apache.uima.jcas.cas.FSList;


/** 
 * Updated by JCasGen Sun Sep 14 23:46:31 EDT 2014
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/TypeSystem.xml
 * @generated */
public class StanfordTreeAnnotation extends ComponentAnnotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(StanfordTreeAnnotation.class);
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
  protected StanfordTreeAnnotation() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public StanfordTreeAnnotation(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public StanfordTreeAnnotation(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public StanfordTreeAnnotation(JCas jcas, int begin, int end) {
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
  //* Feature: pennTreeLabel

  /** getter for pennTreeLabel - gets 
   * @generated
   * @return value of the feature 
   */
  public String getPennTreeLabel() {
    if (StanfordTreeAnnotation_Type.featOkTst && ((StanfordTreeAnnotation_Type)jcasType).casFeat_pennTreeLabel == null)
      jcasType.jcas.throwFeatMissing("pennTreeLabel", "edu.cmu.cs.lti.script.type.StanfordTreeAnnotation");
    return jcasType.ll_cas.ll_getStringValue(addr, ((StanfordTreeAnnotation_Type)jcasType).casFeatCode_pennTreeLabel);}
    
  /** setter for pennTreeLabel - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setPennTreeLabel(String v) {
    if (StanfordTreeAnnotation_Type.featOkTst && ((StanfordTreeAnnotation_Type)jcasType).casFeat_pennTreeLabel == null)
      jcasType.jcas.throwFeatMissing("pennTreeLabel", "edu.cmu.cs.lti.script.type.StanfordTreeAnnotation");
    jcasType.ll_cas.ll_setStringValue(addr, ((StanfordTreeAnnotation_Type)jcasType).casFeatCode_pennTreeLabel, v);}    
   
    
  //*--------------*
  //* Feature: children

  /** getter for children - gets 
   * @generated
   * @return value of the feature 
   */
  public FSArray getChildren() {
    if (StanfordTreeAnnotation_Type.featOkTst && ((StanfordTreeAnnotation_Type)jcasType).casFeat_children == null)
      jcasType.jcas.throwFeatMissing("children", "edu.cmu.cs.lti.script.type.StanfordTreeAnnotation");
    return (FSArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((StanfordTreeAnnotation_Type)jcasType).casFeatCode_children)));}
    
  /** setter for children - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setChildren(FSArray v) {
    if (StanfordTreeAnnotation_Type.featOkTst && ((StanfordTreeAnnotation_Type)jcasType).casFeat_children == null)
      jcasType.jcas.throwFeatMissing("children", "edu.cmu.cs.lti.script.type.StanfordTreeAnnotation");
    jcasType.ll_cas.ll_setRefValue(addr, ((StanfordTreeAnnotation_Type)jcasType).casFeatCode_children, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for children - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public StanfordTreeAnnotation getChildren(int i) {
    if (StanfordTreeAnnotation_Type.featOkTst && ((StanfordTreeAnnotation_Type)jcasType).casFeat_children == null)
      jcasType.jcas.throwFeatMissing("children", "edu.cmu.cs.lti.script.type.StanfordTreeAnnotation");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((StanfordTreeAnnotation_Type)jcasType).casFeatCode_children), i);
    return (StanfordTreeAnnotation)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((StanfordTreeAnnotation_Type)jcasType).casFeatCode_children), i)));}

  /** indexed setter for children - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setChildren(int i, StanfordTreeAnnotation v) { 
    if (StanfordTreeAnnotation_Type.featOkTst && ((StanfordTreeAnnotation_Type)jcasType).casFeat_children == null)
      jcasType.jcas.throwFeatMissing("children", "edu.cmu.cs.lti.script.type.StanfordTreeAnnotation");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((StanfordTreeAnnotation_Type)jcasType).casFeatCode_children), i);
    jcasType.ll_cas.ll_setRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((StanfordTreeAnnotation_Type)jcasType).casFeatCode_children), i, jcasType.ll_cas.ll_getFSRef(v));}
   
    
  //*--------------*
  //* Feature: isLeaf

  /** getter for isLeaf - gets 
   * @generated
   * @return value of the feature 
   */
  public boolean getIsLeaf() {
    if (StanfordTreeAnnotation_Type.featOkTst && ((StanfordTreeAnnotation_Type)jcasType).casFeat_isLeaf == null)
      jcasType.jcas.throwFeatMissing("isLeaf", "edu.cmu.cs.lti.script.type.StanfordTreeAnnotation");
    return jcasType.ll_cas.ll_getBooleanValue(addr, ((StanfordTreeAnnotation_Type)jcasType).casFeatCode_isLeaf);}
    
  /** setter for isLeaf - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setIsLeaf(boolean v) {
    if (StanfordTreeAnnotation_Type.featOkTst && ((StanfordTreeAnnotation_Type)jcasType).casFeat_isLeaf == null)
      jcasType.jcas.throwFeatMissing("isLeaf", "edu.cmu.cs.lti.script.type.StanfordTreeAnnotation");
    jcasType.ll_cas.ll_setBooleanValue(addr, ((StanfordTreeAnnotation_Type)jcasType).casFeatCode_isLeaf, v);}    
   
    
  //*--------------*
  //* Feature: isRoot

  /** getter for isRoot - gets 
   * @generated
   * @return value of the feature 
   */
  public boolean getIsRoot() {
    if (StanfordTreeAnnotation_Type.featOkTst && ((StanfordTreeAnnotation_Type)jcasType).casFeat_isRoot == null)
      jcasType.jcas.throwFeatMissing("isRoot", "edu.cmu.cs.lti.script.type.StanfordTreeAnnotation");
    return jcasType.ll_cas.ll_getBooleanValue(addr, ((StanfordTreeAnnotation_Type)jcasType).casFeatCode_isRoot);}
    
  /** setter for isRoot - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setIsRoot(boolean v) {
    if (StanfordTreeAnnotation_Type.featOkTst && ((StanfordTreeAnnotation_Type)jcasType).casFeat_isRoot == null)
      jcasType.jcas.throwFeatMissing("isRoot", "edu.cmu.cs.lti.script.type.StanfordTreeAnnotation");
    jcasType.ll_cas.ll_setBooleanValue(addr, ((StanfordTreeAnnotation_Type)jcasType).casFeatCode_isRoot, v);}    
   
    
  //*--------------*
  //* Feature: parent

  /** getter for parent - gets 
   * @generated
   * @return value of the feature 
   */
  public StanfordTreeAnnotation getParent() {
    if (StanfordTreeAnnotation_Type.featOkTst && ((StanfordTreeAnnotation_Type)jcasType).casFeat_parent == null)
      jcasType.jcas.throwFeatMissing("parent", "edu.cmu.cs.lti.script.type.StanfordTreeAnnotation");
    return (StanfordTreeAnnotation)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((StanfordTreeAnnotation_Type)jcasType).casFeatCode_parent)));}
    
  /** setter for parent - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setParent(StanfordTreeAnnotation v) {
    if (StanfordTreeAnnotation_Type.featOkTst && ((StanfordTreeAnnotation_Type)jcasType).casFeat_parent == null)
      jcasType.jcas.throwFeatMissing("parent", "edu.cmu.cs.lti.script.type.StanfordTreeAnnotation");
    jcasType.ll_cas.ll_setRefValue(addr, ((StanfordTreeAnnotation_Type)jcasType).casFeatCode_parent, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: head

  /** getter for head - gets 
   * @generated
   * @return value of the feature 
   */
  public StanfordCorenlpToken getHead() {
    if (StanfordTreeAnnotation_Type.featOkTst && ((StanfordTreeAnnotation_Type)jcasType).casFeat_head == null)
      jcasType.jcas.throwFeatMissing("head", "edu.cmu.cs.lti.script.type.StanfordTreeAnnotation");
    return (StanfordCorenlpToken)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((StanfordTreeAnnotation_Type)jcasType).casFeatCode_head)));}
    
  /** setter for head - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setHead(StanfordCorenlpToken v) {
    if (StanfordTreeAnnotation_Type.featOkTst && ((StanfordTreeAnnotation_Type)jcasType).casFeat_head == null)
      jcasType.jcas.throwFeatMissing("head", "edu.cmu.cs.lti.script.type.StanfordTreeAnnotation");
    jcasType.ll_cas.ll_setRefValue(addr, ((StanfordTreeAnnotation_Type)jcasType).casFeatCode_head, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    