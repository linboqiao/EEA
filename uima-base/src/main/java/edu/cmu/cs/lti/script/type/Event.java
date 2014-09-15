

/* First created by JCasGen Sat Sep 13 18:23:45 EDT 2014 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.FSList;


/** An abstract annotation for event, which is a generalized concept that contains event mentions. It could be seen as we assign some attributes to the event mention clusters.
 * Updated by JCasGen Sun Sep 14 23:46:31 EDT 2014
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/TypeSystem.xml
 * @generated */
public class Event extends ComponentTOP {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Event.class);
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
  protected Event() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public Event(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public Event(JCas jcas) {
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
  //* Feature: eventType

  /** getter for eventType - gets Event type
   * @generated
   * @return value of the feature 
   */
  public String getEventType() {
    if (Event_Type.featOkTst && ((Event_Type)jcasType).casFeat_eventType == null)
      jcasType.jcas.throwFeatMissing("eventType", "edu.cmu.cs.lti.script.type.Event");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Event_Type)jcasType).casFeatCode_eventType);}
    
  /** setter for eventType - sets Event type 
   * @generated
   * @param v value to set into the feature 
   */
  public void setEventType(String v) {
    if (Event_Type.featOkTst && ((Event_Type)jcasType).casFeat_eventType == null)
      jcasType.jcas.throwFeatMissing("eventType", "edu.cmu.cs.lti.script.type.Event");
    jcasType.ll_cas.ll_setStringValue(addr, ((Event_Type)jcasType).casFeatCode_eventType, v);}    
   
    
  //*--------------*
  //* Feature: eventSubtype

  /** getter for eventSubtype - gets Event subtype
   * @generated
   * @return value of the feature 
   */
  public String getEventSubtype() {
    if (Event_Type.featOkTst && ((Event_Type)jcasType).casFeat_eventSubtype == null)
      jcasType.jcas.throwFeatMissing("eventSubtype", "edu.cmu.cs.lti.script.type.Event");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Event_Type)jcasType).casFeatCode_eventSubtype);}
    
  /** setter for eventSubtype - sets Event subtype 
   * @generated
   * @param v value to set into the feature 
   */
  public void setEventSubtype(String v) {
    if (Event_Type.featOkTst && ((Event_Type)jcasType).casFeat_eventSubtype == null)
      jcasType.jcas.throwFeatMissing("eventSubtype", "edu.cmu.cs.lti.script.type.Event");
    jcasType.ll_cas.ll_setStringValue(addr, ((Event_Type)jcasType).casFeatCode_eventSubtype, v);}    
   
    
  //*--------------*
  //* Feature: modality

  /** getter for modality - gets 
   * @generated
   * @return value of the feature 
   */
  public String getModality() {
    if (Event_Type.featOkTst && ((Event_Type)jcasType).casFeat_modality == null)
      jcasType.jcas.throwFeatMissing("modality", "edu.cmu.cs.lti.script.type.Event");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Event_Type)jcasType).casFeatCode_modality);}
    
  /** setter for modality - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setModality(String v) {
    if (Event_Type.featOkTst && ((Event_Type)jcasType).casFeat_modality == null)
      jcasType.jcas.throwFeatMissing("modality", "edu.cmu.cs.lti.script.type.Event");
    jcasType.ll_cas.ll_setStringValue(addr, ((Event_Type)jcasType).casFeatCode_modality, v);}    
   
    
  //*--------------*
  //* Feature: polarity

  /** getter for polarity - gets Polarity annotation for ACE
   * @generated
   * @return value of the feature 
   */
  public String getPolarity() {
    if (Event_Type.featOkTst && ((Event_Type)jcasType).casFeat_polarity == null)
      jcasType.jcas.throwFeatMissing("polarity", "edu.cmu.cs.lti.script.type.Event");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Event_Type)jcasType).casFeatCode_polarity);}
    
  /** setter for polarity - sets Polarity annotation for ACE 
   * @generated
   * @param v value to set into the feature 
   */
  public void setPolarity(String v) {
    if (Event_Type.featOkTst && ((Event_Type)jcasType).casFeat_polarity == null)
      jcasType.jcas.throwFeatMissing("polarity", "edu.cmu.cs.lti.script.type.Event");
    jcasType.ll_cas.ll_setStringValue(addr, ((Event_Type)jcasType).casFeatCode_polarity, v);}    
   
    
  //*--------------*
  //* Feature: genericity

  /** getter for genericity - gets 
   * @generated
   * @return value of the feature 
   */
  public String getGenericity() {
    if (Event_Type.featOkTst && ((Event_Type)jcasType).casFeat_genericity == null)
      jcasType.jcas.throwFeatMissing("genericity", "edu.cmu.cs.lti.script.type.Event");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Event_Type)jcasType).casFeatCode_genericity);}
    
  /** setter for genericity - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setGenericity(String v) {
    if (Event_Type.featOkTst && ((Event_Type)jcasType).casFeat_genericity == null)
      jcasType.jcas.throwFeatMissing("genericity", "edu.cmu.cs.lti.script.type.Event");
    jcasType.ll_cas.ll_setStringValue(addr, ((Event_Type)jcasType).casFeatCode_genericity, v);}    
   
    
  //*--------------*
  //* Feature: tense

  /** getter for tense - gets 
   * @generated
   * @return value of the feature 
   */
  public String getTense() {
    if (Event_Type.featOkTst && ((Event_Type)jcasType).casFeat_tense == null)
      jcasType.jcas.throwFeatMissing("tense", "edu.cmu.cs.lti.script.type.Event");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Event_Type)jcasType).casFeatCode_tense);}
    
  /** setter for tense - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setTense(String v) {
    if (Event_Type.featOkTst && ((Event_Type)jcasType).casFeat_tense == null)
      jcasType.jcas.throwFeatMissing("tense", "edu.cmu.cs.lti.script.type.Event");
    jcasType.ll_cas.ll_setStringValue(addr, ((Event_Type)jcasType).casFeatCode_tense, v);}    
   
    
  //*--------------*
  //* Feature: arguments

  /** getter for arguments - gets Arguments of the event, annotated from the ace golden standard
   * @generated
   * @return value of the feature 
   */
  public FSList getArguments() {
    if (Event_Type.featOkTst && ((Event_Type)jcasType).casFeat_arguments == null)
      jcasType.jcas.throwFeatMissing("arguments", "edu.cmu.cs.lti.script.type.Event");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Event_Type)jcasType).casFeatCode_arguments)));}
    
  /** setter for arguments - sets Arguments of the event, annotated from the ace golden standard 
   * @generated
   * @param v value to set into the feature 
   */
  public void setArguments(FSList v) {
    if (Event_Type.featOkTst && ((Event_Type)jcasType).casFeat_arguments == null)
      jcasType.jcas.throwFeatMissing("arguments", "edu.cmu.cs.lti.script.type.Event");
    jcasType.ll_cas.ll_setRefValue(addr, ((Event_Type)jcasType).casFeatCode_arguments, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: eventIndex

  /** getter for eventIndex - gets Event id assigned by our system
   * @generated
   * @return value of the feature 
   */
  public int getEventIndex() {
    if (Event_Type.featOkTst && ((Event_Type)jcasType).casFeat_eventIndex == null)
      jcasType.jcas.throwFeatMissing("eventIndex", "edu.cmu.cs.lti.script.type.Event");
    return jcasType.ll_cas.ll_getIntValue(addr, ((Event_Type)jcasType).casFeatCode_eventIndex);}
    
  /** setter for eventIndex - sets Event id assigned by our system 
   * @generated
   * @param v value to set into the feature 
   */
  public void setEventIndex(int v) {
    if (Event_Type.featOkTst && ((Event_Type)jcasType).casFeat_eventIndex == null)
      jcasType.jcas.throwFeatMissing("eventIndex", "edu.cmu.cs.lti.script.type.Event");
    jcasType.ll_cas.ll_setIntValue(addr, ((Event_Type)jcasType).casFeatCode_eventIndex, v);}    
   
    
  //*--------------*
  //* Feature: eventMentions

  /** getter for eventMentions - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getEventMentions() {
    if (Event_Type.featOkTst && ((Event_Type)jcasType).casFeat_eventMentions == null)
      jcasType.jcas.throwFeatMissing("eventMentions", "edu.cmu.cs.lti.script.type.Event");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Event_Type)jcasType).casFeatCode_eventMentions)));}
    
  /** setter for eventMentions - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setEventMentions(FSList v) {
    if (Event_Type.featOkTst && ((Event_Type)jcasType).casFeat_eventMentions == null)
      jcasType.jcas.throwFeatMissing("eventMentions", "edu.cmu.cs.lti.script.type.Event");
    jcasType.ll_cas.ll_setRefValue(addr, ((Event_Type)jcasType).casFeatCode_eventMentions, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: isEmpty

  /** getter for isEmpty - gets show that this event is dummy and empty, not used to store event mentions, which is used only as a placeholder
   * @generated
   * @return value of the feature 
   */
  public boolean getIsEmpty() {
    if (Event_Type.featOkTst && ((Event_Type)jcasType).casFeat_isEmpty == null)
      jcasType.jcas.throwFeatMissing("isEmpty", "edu.cmu.cs.lti.script.type.Event");
    return jcasType.ll_cas.ll_getBooleanValue(addr, ((Event_Type)jcasType).casFeatCode_isEmpty);}
    
  /** setter for isEmpty - sets show that this event is dummy and empty, not used to store event mentions, which is used only as a placeholder 
   * @generated
   * @param v value to set into the feature 
   */
  public void setIsEmpty(boolean v) {
    if (Event_Type.featOkTst && ((Event_Type)jcasType).casFeat_isEmpty == null)
      jcasType.jcas.throwFeatMissing("isEmpty", "edu.cmu.cs.lti.script.type.Event");
    jcasType.ll_cas.ll_setBooleanValue(addr, ((Event_Type)jcasType).casFeatCode_isEmpty, v);}    
  }

    