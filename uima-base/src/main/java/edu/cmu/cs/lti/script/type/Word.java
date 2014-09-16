

/* First created by JCasGen Mon Sep 15 15:04:26 EDT 2014 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.FSList;


/** 
 * Updated by JCasGen Tue Sep 16 00:38:30 EDT 2014
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/TypeSystem.xml
 * @generated */
public class Word extends ComponentAnnotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Word.class);
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
  protected Word() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public Word(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public Word(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public Word(JCas jcas, int begin, int end) {
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
  //* Feature: lemma

  /** getter for lemma - gets 
   * @generated
   * @return value of the feature 
   */
  public String getLemma() {
    if (Word_Type.featOkTst && ((Word_Type)jcasType).casFeat_lemma == null)
      jcasType.jcas.throwFeatMissing("lemma", "edu.cmu.cs.lti.script.type.Word");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Word_Type)jcasType).casFeatCode_lemma);}
    
  /** setter for lemma - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setLemma(String v) {
    if (Word_Type.featOkTst && ((Word_Type)jcasType).casFeat_lemma == null)
      jcasType.jcas.throwFeatMissing("lemma", "edu.cmu.cs.lti.script.type.Word");
    jcasType.ll_cas.ll_setStringValue(addr, ((Word_Type)jcasType).casFeatCode_lemma, v);}    
   
    
  //*--------------*
  //* Feature: elliptical

  /** getter for elliptical - gets 
   * @generated
   * @return value of the feature 
   */
  public String getElliptical() {
    if (Word_Type.featOkTst && ((Word_Type)jcasType).casFeat_elliptical == null)
      jcasType.jcas.throwFeatMissing("elliptical", "edu.cmu.cs.lti.script.type.Word");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Word_Type)jcasType).casFeatCode_elliptical);}
    
  /** setter for elliptical - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setElliptical(String v) {
    if (Word_Type.featOkTst && ((Word_Type)jcasType).casFeat_elliptical == null)
      jcasType.jcas.throwFeatMissing("elliptical", "edu.cmu.cs.lti.script.type.Word");
    jcasType.ll_cas.ll_setStringValue(addr, ((Word_Type)jcasType).casFeatCode_elliptical, v);}    
   
    
  //*--------------*
  //* Feature: pos

  /** getter for pos - gets 
   * @generated
   * @return value of the feature 
   */
  public String getPos() {
    if (Word_Type.featOkTst && ((Word_Type)jcasType).casFeat_pos == null)
      jcasType.jcas.throwFeatMissing("pos", "edu.cmu.cs.lti.script.type.Word");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Word_Type)jcasType).casFeatCode_pos);}
    
  /** setter for pos - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setPos(String v) {
    if (Word_Type.featOkTst && ((Word_Type)jcasType).casFeat_pos == null)
      jcasType.jcas.throwFeatMissing("pos", "edu.cmu.cs.lti.script.type.Word");
    jcasType.ll_cas.ll_setStringValue(addr, ((Word_Type)jcasType).casFeatCode_pos, v);}    
   
    
  //*--------------*
  //* Feature: nerTag

  /** getter for nerTag - gets 
   * @generated
   * @return value of the feature 
   */
  public String getNerTag() {
    if (Word_Type.featOkTst && ((Word_Type)jcasType).casFeat_nerTag == null)
      jcasType.jcas.throwFeatMissing("nerTag", "edu.cmu.cs.lti.script.type.Word");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Word_Type)jcasType).casFeatCode_nerTag);}
    
  /** setter for nerTag - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setNerTag(String v) {
    if (Word_Type.featOkTst && ((Word_Type)jcasType).casFeat_nerTag == null)
      jcasType.jcas.throwFeatMissing("nerTag", "edu.cmu.cs.lti.script.type.Word");
    jcasType.ll_cas.ll_setStringValue(addr, ((Word_Type)jcasType).casFeatCode_nerTag, v);}    
   
    
  //*--------------*
  //* Feature: morpha

  /** getter for morpha - gets Unifying the morphological type and change it into a more based form
   * @generated
   * @return value of the feature 
   */
  public String getMorpha() {
    if (Word_Type.featOkTst && ((Word_Type)jcasType).casFeat_morpha == null)
      jcasType.jcas.throwFeatMissing("morpha", "edu.cmu.cs.lti.script.type.Word");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Word_Type)jcasType).casFeatCode_morpha);}
    
  /** setter for morpha - sets Unifying the morphological type and change it into a more based form 
   * @generated
   * @param v value to set into the feature 
   */
  public void setMorpha(String v) {
    if (Word_Type.featOkTst && ((Word_Type)jcasType).casFeat_morpha == null)
      jcasType.jcas.throwFeatMissing("morpha", "edu.cmu.cs.lti.script.type.Word");
    jcasType.ll_cas.ll_setStringValue(addr, ((Word_Type)jcasType).casFeatCode_morpha, v);}    
   
    
  //*--------------*
  //* Feature: headDependencyRelations

  /** getter for headDependencyRelations - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getHeadDependencyRelations() {
    if (Word_Type.featOkTst && ((Word_Type)jcasType).casFeat_headDependencyRelations == null)
      jcasType.jcas.throwFeatMissing("headDependencyRelations", "edu.cmu.cs.lti.script.type.Word");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Word_Type)jcasType).casFeatCode_headDependencyRelations)));}
    
  /** setter for headDependencyRelations - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setHeadDependencyRelations(FSList v) {
    if (Word_Type.featOkTst && ((Word_Type)jcasType).casFeat_headDependencyRelations == null)
      jcasType.jcas.throwFeatMissing("headDependencyRelations", "edu.cmu.cs.lti.script.type.Word");
    jcasType.ll_cas.ll_setRefValue(addr, ((Word_Type)jcasType).casFeatCode_headDependencyRelations, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: childDependencyRelations

  /** getter for childDependencyRelations - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getChildDependencyRelations() {
    if (Word_Type.featOkTst && ((Word_Type)jcasType).casFeat_childDependencyRelations == null)
      jcasType.jcas.throwFeatMissing("childDependencyRelations", "edu.cmu.cs.lti.script.type.Word");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Word_Type)jcasType).casFeatCode_childDependencyRelations)));}
    
  /** setter for childDependencyRelations - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setChildDependencyRelations(FSList v) {
    if (Word_Type.featOkTst && ((Word_Type)jcasType).casFeat_childDependencyRelations == null)
      jcasType.jcas.throwFeatMissing("childDependencyRelations", "edu.cmu.cs.lti.script.type.Word");
    jcasType.ll_cas.ll_setRefValue(addr, ((Word_Type)jcasType).casFeatCode_childDependencyRelations, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: isDependencyRoot

  /** getter for isDependencyRoot - gets 
   * @generated
   * @return value of the feature 
   */
  public boolean getIsDependencyRoot() {
    if (Word_Type.featOkTst && ((Word_Type)jcasType).casFeat_isDependencyRoot == null)
      jcasType.jcas.throwFeatMissing("isDependencyRoot", "edu.cmu.cs.lti.script.type.Word");
    return jcasType.ll_cas.ll_getBooleanValue(addr, ((Word_Type)jcasType).casFeatCode_isDependencyRoot);}
    
  /** setter for isDependencyRoot - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setIsDependencyRoot(boolean v) {
    if (Word_Type.featOkTst && ((Word_Type)jcasType).casFeat_isDependencyRoot == null)
      jcasType.jcas.throwFeatMissing("isDependencyRoot", "edu.cmu.cs.lti.script.type.Word");
    jcasType.ll_cas.ll_setBooleanValue(addr, ((Word_Type)jcasType).casFeatCode_isDependencyRoot, v);}    
   
    
  //*--------------*
  //* Feature: headSemanticRelations

  /** getter for headSemanticRelations - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getHeadSemanticRelations() {
    if (Word_Type.featOkTst && ((Word_Type)jcasType).casFeat_headSemanticRelations == null)
      jcasType.jcas.throwFeatMissing("headSemanticRelations", "edu.cmu.cs.lti.script.type.Word");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Word_Type)jcasType).casFeatCode_headSemanticRelations)));}
    
  /** setter for headSemanticRelations - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setHeadSemanticRelations(FSList v) {
    if (Word_Type.featOkTst && ((Word_Type)jcasType).casFeat_headSemanticRelations == null)
      jcasType.jcas.throwFeatMissing("headSemanticRelations", "edu.cmu.cs.lti.script.type.Word");
    jcasType.ll_cas.ll_setRefValue(addr, ((Word_Type)jcasType).casFeatCode_headSemanticRelations, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: childSemanticRelations

  /** getter for childSemanticRelations - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getChildSemanticRelations() {
    if (Word_Type.featOkTst && ((Word_Type)jcasType).casFeat_childSemanticRelations == null)
      jcasType.jcas.throwFeatMissing("childSemanticRelations", "edu.cmu.cs.lti.script.type.Word");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Word_Type)jcasType).casFeatCode_childSemanticRelations)));}
    
  /** setter for childSemanticRelations - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setChildSemanticRelations(FSList v) {
    if (Word_Type.featOkTst && ((Word_Type)jcasType).casFeat_childSemanticRelations == null)
      jcasType.jcas.throwFeatMissing("childSemanticRelations", "edu.cmu.cs.lti.script.type.Word");
    jcasType.ll_cas.ll_setRefValue(addr, ((Word_Type)jcasType).casFeatCode_childSemanticRelations, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    