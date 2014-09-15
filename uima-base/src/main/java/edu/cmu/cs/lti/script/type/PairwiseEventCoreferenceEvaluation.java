

/* First created by JCasGen Sat Sep 13 18:23:45 EDT 2014 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.FSList;


/** The type storing the information on event coreference.
 * Updated by JCasGen Sun Sep 14 23:46:31 EDT 2014
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/TypeSystem.xml
 * @generated */
public class PairwiseEventCoreferenceEvaluation extends ComponentTOP {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(PairwiseEventCoreferenceEvaluation.class);
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
  protected PairwiseEventCoreferenceEvaluation() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public PairwiseEventCoreferenceEvaluation(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public PairwiseEventCoreferenceEvaluation(JCas jcas) {
    super(jcas);
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
  //* Feature: eventMentionI

  /** getter for eventMentionI - gets 
   * @generated
   * @return value of the feature 
   */
  public EventMention getEventMentionI() {
    if (PairwiseEventCoreferenceEvaluation_Type.featOkTst && ((PairwiseEventCoreferenceEvaluation_Type)jcasType).casFeat_eventMentionI == null)
      jcasType.jcas.throwFeatMissing("eventMentionI", "edu.cmu.cs.lti.script.type.PairwiseEventCoreferenceEvaluation");
    return (EventMention)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((PairwiseEventCoreferenceEvaluation_Type)jcasType).casFeatCode_eventMentionI)));}
    
  /** setter for eventMentionI - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setEventMentionI(EventMention v) {
    if (PairwiseEventCoreferenceEvaluation_Type.featOkTst && ((PairwiseEventCoreferenceEvaluation_Type)jcasType).casFeat_eventMentionI == null)
      jcasType.jcas.throwFeatMissing("eventMentionI", "edu.cmu.cs.lti.script.type.PairwiseEventCoreferenceEvaluation");
    jcasType.ll_cas.ll_setRefValue(addr, ((PairwiseEventCoreferenceEvaluation_Type)jcasType).casFeatCode_eventMentionI, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: eventMentionJ

  /** getter for eventMentionJ - gets 
   * @generated
   * @return value of the feature 
   */
  public EventMention getEventMentionJ() {
    if (PairwiseEventCoreferenceEvaluation_Type.featOkTst && ((PairwiseEventCoreferenceEvaluation_Type)jcasType).casFeat_eventMentionJ == null)
      jcasType.jcas.throwFeatMissing("eventMentionJ", "edu.cmu.cs.lti.script.type.PairwiseEventCoreferenceEvaluation");
    return (EventMention)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((PairwiseEventCoreferenceEvaluation_Type)jcasType).casFeatCode_eventMentionJ)));}
    
  /** setter for eventMentionJ - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setEventMentionJ(EventMention v) {
    if (PairwiseEventCoreferenceEvaluation_Type.featOkTst && ((PairwiseEventCoreferenceEvaluation_Type)jcasType).casFeat_eventMentionJ == null)
      jcasType.jcas.throwFeatMissing("eventMentionJ", "edu.cmu.cs.lti.script.type.PairwiseEventCoreferenceEvaluation");
    jcasType.ll_cas.ll_setRefValue(addr, ((PairwiseEventCoreferenceEvaluation_Type)jcasType).casFeatCode_eventMentionJ, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: eventCoreferenceRelationGoldStandard

  /** getter for eventCoreferenceRelationGoldStandard - gets Its value can be one of full, member, subevent, and no.
   * @generated
   * @return value of the feature 
   */
  public String getEventCoreferenceRelationGoldStandard() {
    if (PairwiseEventCoreferenceEvaluation_Type.featOkTst && ((PairwiseEventCoreferenceEvaluation_Type)jcasType).casFeat_eventCoreferenceRelationGoldStandard == null)
      jcasType.jcas.throwFeatMissing("eventCoreferenceRelationGoldStandard", "edu.cmu.cs.lti.script.type.PairwiseEventCoreferenceEvaluation");
    return jcasType.ll_cas.ll_getStringValue(addr, ((PairwiseEventCoreferenceEvaluation_Type)jcasType).casFeatCode_eventCoreferenceRelationGoldStandard);}
    
  /** setter for eventCoreferenceRelationGoldStandard - sets Its value can be one of full, member, subevent, and no. 
   * @generated
   * @param v value to set into the feature 
   */
  public void setEventCoreferenceRelationGoldStandard(String v) {
    if (PairwiseEventCoreferenceEvaluation_Type.featOkTst && ((PairwiseEventCoreferenceEvaluation_Type)jcasType).casFeat_eventCoreferenceRelationGoldStandard == null)
      jcasType.jcas.throwFeatMissing("eventCoreferenceRelationGoldStandard", "edu.cmu.cs.lti.script.type.PairwiseEventCoreferenceEvaluation");
    jcasType.ll_cas.ll_setStringValue(addr, ((PairwiseEventCoreferenceEvaluation_Type)jcasType).casFeatCode_eventCoreferenceRelationGoldStandard, v);}    
   
    
  //*--------------*
  //* Feature: eventCoreferenceRelationSystem

  /** getter for eventCoreferenceRelationSystem - gets Its value can be one of full, member, subevent, and no.
   * @generated
   * @return value of the feature 
   */
  public String getEventCoreferenceRelationSystem() {
    if (PairwiseEventCoreferenceEvaluation_Type.featOkTst && ((PairwiseEventCoreferenceEvaluation_Type)jcasType).casFeat_eventCoreferenceRelationSystem == null)
      jcasType.jcas.throwFeatMissing("eventCoreferenceRelationSystem", "edu.cmu.cs.lti.script.type.PairwiseEventCoreferenceEvaluation");
    return jcasType.ll_cas.ll_getStringValue(addr, ((PairwiseEventCoreferenceEvaluation_Type)jcasType).casFeatCode_eventCoreferenceRelationSystem);}
    
  /** setter for eventCoreferenceRelationSystem - sets Its value can be one of full, member, subevent, and no. 
   * @generated
   * @param v value to set into the feature 
   */
  public void setEventCoreferenceRelationSystem(String v) {
    if (PairwiseEventCoreferenceEvaluation_Type.featOkTst && ((PairwiseEventCoreferenceEvaluation_Type)jcasType).casFeat_eventCoreferenceRelationSystem == null)
      jcasType.jcas.throwFeatMissing("eventCoreferenceRelationSystem", "edu.cmu.cs.lti.script.type.PairwiseEventCoreferenceEvaluation");
    jcasType.ll_cas.ll_setStringValue(addr, ((PairwiseEventCoreferenceEvaluation_Type)jcasType).casFeatCode_eventCoreferenceRelationSystem, v);}    
   
    
  //*--------------*
  //* Feature: pairwiseEventFeatures

  /** getter for pairwiseEventFeatures - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getPairwiseEventFeatures() {
    if (PairwiseEventCoreferenceEvaluation_Type.featOkTst && ((PairwiseEventCoreferenceEvaluation_Type)jcasType).casFeat_pairwiseEventFeatures == null)
      jcasType.jcas.throwFeatMissing("pairwiseEventFeatures", "edu.cmu.cs.lti.script.type.PairwiseEventCoreferenceEvaluation");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((PairwiseEventCoreferenceEvaluation_Type)jcasType).casFeatCode_pairwiseEventFeatures)));}
    
  /** setter for pairwiseEventFeatures - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setPairwiseEventFeatures(FSList v) {
    if (PairwiseEventCoreferenceEvaluation_Type.featOkTst && ((PairwiseEventCoreferenceEvaluation_Type)jcasType).casFeat_pairwiseEventFeatures == null)
      jcasType.jcas.throwFeatMissing("pairwiseEventFeatures", "edu.cmu.cs.lti.script.type.PairwiseEventCoreferenceEvaluation");
    jcasType.ll_cas.ll_setRefValue(addr, ((PairwiseEventCoreferenceEvaluation_Type)jcasType).casFeatCode_pairwiseEventFeatures, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: isUnified

  /** getter for isUnified - gets 
   * @generated
   * @return value of the feature 
   */
  public boolean getIsUnified() {
    if (PairwiseEventCoreferenceEvaluation_Type.featOkTst && ((PairwiseEventCoreferenceEvaluation_Type)jcasType).casFeat_isUnified == null)
      jcasType.jcas.throwFeatMissing("isUnified", "edu.cmu.cs.lti.script.type.PairwiseEventCoreferenceEvaluation");
    return jcasType.ll_cas.ll_getBooleanValue(addr, ((PairwiseEventCoreferenceEvaluation_Type)jcasType).casFeatCode_isUnified);}
    
  /** setter for isUnified - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setIsUnified(boolean v) {
    if (PairwiseEventCoreferenceEvaluation_Type.featOkTst && ((PairwiseEventCoreferenceEvaluation_Type)jcasType).casFeat_isUnified == null)
      jcasType.jcas.throwFeatMissing("isUnified", "edu.cmu.cs.lti.script.type.PairwiseEventCoreferenceEvaluation");
    jcasType.ll_cas.ll_setBooleanValue(addr, ((PairwiseEventCoreferenceEvaluation_Type)jcasType).casFeatCode_isUnified, v);}    
   
    
  //*--------------*
  //* Feature: confidence

  /** getter for confidence - gets Confidence score for pairwise decision from the system
   * @generated
   * @return value of the feature 
   */
  public double getConfidence() {
    if (PairwiseEventCoreferenceEvaluation_Type.featOkTst && ((PairwiseEventCoreferenceEvaluation_Type)jcasType).casFeat_confidence == null)
      jcasType.jcas.throwFeatMissing("confidence", "edu.cmu.cs.lti.script.type.PairwiseEventCoreferenceEvaluation");
    return jcasType.ll_cas.ll_getDoubleValue(addr, ((PairwiseEventCoreferenceEvaluation_Type)jcasType).casFeatCode_confidence);}
    
  /** setter for confidence - sets Confidence score for pairwise decision from the system 
   * @generated
   * @param v value to set into the feature 
   */
  public void setConfidence(double v) {
    if (PairwiseEventCoreferenceEvaluation_Type.featOkTst && ((PairwiseEventCoreferenceEvaluation_Type)jcasType).casFeat_confidence == null)
      jcasType.jcas.throwFeatMissing("confidence", "edu.cmu.cs.lti.script.type.PairwiseEventCoreferenceEvaluation");
    jcasType.ll_cas.ll_setDoubleValue(addr, ((PairwiseEventCoreferenceEvaluation_Type)jcasType).casFeatCode_confidence, v);}    
  }

    