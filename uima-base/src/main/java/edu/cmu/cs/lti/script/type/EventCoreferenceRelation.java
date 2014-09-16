

/* First created by JCasGen Mon Sep 15 15:04:26 EDT 2014 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;



/** 
 * Updated by JCasGen Tue Sep 16 00:38:29 EDT 2014
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/TypeSystem.xml
 * @generated */
public class EventCoreferenceRelation extends ComponentTOP {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(EventCoreferenceRelation.class);
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
  protected EventCoreferenceRelation() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public EventCoreferenceRelation(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public EventCoreferenceRelation(JCas jcas) {
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
  //* Feature: relationType

  /** getter for relationType - gets 
   * @generated
   * @return value of the feature 
   */
  public String getRelationType() {
    if (EventCoreferenceRelation_Type.featOkTst && ((EventCoreferenceRelation_Type)jcasType).casFeat_relationType == null)
      jcasType.jcas.throwFeatMissing("relationType", "edu.cmu.cs.lti.script.type.EventCoreferenceRelation");
    return jcasType.ll_cas.ll_getStringValue(addr, ((EventCoreferenceRelation_Type)jcasType).casFeatCode_relationType);}
    
  /** setter for relationType - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setRelationType(String v) {
    if (EventCoreferenceRelation_Type.featOkTst && ((EventCoreferenceRelation_Type)jcasType).casFeat_relationType == null)
      jcasType.jcas.throwFeatMissing("relationType", "edu.cmu.cs.lti.script.type.EventCoreferenceRelation");
    jcasType.ll_cas.ll_setStringValue(addr, ((EventCoreferenceRelation_Type)jcasType).casFeatCode_relationType, v);}    
   
    
  //*--------------*
  //* Feature: fromEventMention

  /** getter for fromEventMention - gets 
   * @generated
   * @return value of the feature 
   */
  public EventMention getFromEventMention() {
    if (EventCoreferenceRelation_Type.featOkTst && ((EventCoreferenceRelation_Type)jcasType).casFeat_fromEventMention == null)
      jcasType.jcas.throwFeatMissing("fromEventMention", "edu.cmu.cs.lti.script.type.EventCoreferenceRelation");
    return (EventMention)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EventCoreferenceRelation_Type)jcasType).casFeatCode_fromEventMention)));}
    
  /** setter for fromEventMention - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setFromEventMention(EventMention v) {
    if (EventCoreferenceRelation_Type.featOkTst && ((EventCoreferenceRelation_Type)jcasType).casFeat_fromEventMention == null)
      jcasType.jcas.throwFeatMissing("fromEventMention", "edu.cmu.cs.lti.script.type.EventCoreferenceRelation");
    jcasType.ll_cas.ll_setRefValue(addr, ((EventCoreferenceRelation_Type)jcasType).casFeatCode_fromEventMention, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: toEventMention

  /** getter for toEventMention - gets 
   * @generated
   * @return value of the feature 
   */
  public EventMention getToEventMention() {
    if (EventCoreferenceRelation_Type.featOkTst && ((EventCoreferenceRelation_Type)jcasType).casFeat_toEventMention == null)
      jcasType.jcas.throwFeatMissing("toEventMention", "edu.cmu.cs.lti.script.type.EventCoreferenceRelation");
    return (EventMention)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EventCoreferenceRelation_Type)jcasType).casFeatCode_toEventMention)));}
    
  /** setter for toEventMention - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setToEventMention(EventMention v) {
    if (EventCoreferenceRelation_Type.featOkTst && ((EventCoreferenceRelation_Type)jcasType).casFeat_toEventMention == null)
      jcasType.jcas.throwFeatMissing("toEventMention", "edu.cmu.cs.lti.script.type.EventCoreferenceRelation");
    jcasType.ll_cas.ll_setRefValue(addr, ((EventCoreferenceRelation_Type)jcasType).casFeatCode_toEventMention, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: confidence

  /** getter for confidence - gets 
   * @generated
   * @return value of the feature 
   */
  public double getConfidence() {
    if (EventCoreferenceRelation_Type.featOkTst && ((EventCoreferenceRelation_Type)jcasType).casFeat_confidence == null)
      jcasType.jcas.throwFeatMissing("confidence", "edu.cmu.cs.lti.script.type.EventCoreferenceRelation");
    return jcasType.ll_cas.ll_getDoubleValue(addr, ((EventCoreferenceRelation_Type)jcasType).casFeatCode_confidence);}
    
  /** setter for confidence - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setConfidence(double v) {
    if (EventCoreferenceRelation_Type.featOkTst && ((EventCoreferenceRelation_Type)jcasType).casFeat_confidence == null)
      jcasType.jcas.throwFeatMissing("confidence", "edu.cmu.cs.lti.script.type.EventCoreferenceRelation");
    jcasType.ll_cas.ll_setDoubleValue(addr, ((EventCoreferenceRelation_Type)jcasType).casFeatCode_confidence, v);}    
  }

    