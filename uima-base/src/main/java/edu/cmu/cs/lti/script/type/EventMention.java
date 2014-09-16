

/* First created by JCasGen Mon Sep 15 15:04:26 EDT 2014 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.FSList;


/** 
 * Updated by JCasGen Tue Sep 16 00:38:29 EDT 2014
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/TypeSystem.xml
 * @generated */
public class EventMention extends ComponentAnnotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(EventMention.class);
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
  protected EventMention() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public EventMention(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public EventMention(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public EventMention(JCas jcas, int begin, int end) {
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
  //* Feature: goldStandardEventMentionId

  /** getter for goldStandardEventMentionId - gets This feature stores only event mention IDs in the gold standard.
   * @generated
   * @return value of the feature 
   */
  public String getGoldStandardEventMentionId() {
    if (EventMention_Type.featOkTst && ((EventMention_Type)jcasType).casFeat_goldStandardEventMentionId == null)
      jcasType.jcas.throwFeatMissing("goldStandardEventMentionId", "edu.cmu.cs.lti.script.type.EventMention");
    return jcasType.ll_cas.ll_getStringValue(addr, ((EventMention_Type)jcasType).casFeatCode_goldStandardEventMentionId);}
    
  /** setter for goldStandardEventMentionId - sets This feature stores only event mention IDs in the gold standard. 
   * @generated
   * @param v value to set into the feature 
   */
  public void setGoldStandardEventMentionId(String v) {
    if (EventMention_Type.featOkTst && ((EventMention_Type)jcasType).casFeat_goldStandardEventMentionId == null)
      jcasType.jcas.throwFeatMissing("goldStandardEventMentionId", "edu.cmu.cs.lti.script.type.EventMention");
    jcasType.ll_cas.ll_setStringValue(addr, ((EventMention_Type)jcasType).casFeatCode_goldStandardEventMentionId, v);}    
   
    
  //*--------------*
  //* Feature: eventMentionIndex

  /** getter for eventMentionIndex - gets This feature stores the sequential event mention index.
   * @generated
   * @return value of the feature 
   */
  public int getEventMentionIndex() {
    if (EventMention_Type.featOkTst && ((EventMention_Type)jcasType).casFeat_eventMentionIndex == null)
      jcasType.jcas.throwFeatMissing("eventMentionIndex", "edu.cmu.cs.lti.script.type.EventMention");
    return jcasType.ll_cas.ll_getIntValue(addr, ((EventMention_Type)jcasType).casFeatCode_eventMentionIndex);}
    
  /** setter for eventMentionIndex - sets This feature stores the sequential event mention index. 
   * @generated
   * @param v value to set into the feature 
   */
  public void setEventMentionIndex(int v) {
    if (EventMention_Type.featOkTst && ((EventMention_Type)jcasType).casFeat_eventMentionIndex == null)
      jcasType.jcas.throwFeatMissing("eventMentionIndex", "edu.cmu.cs.lti.script.type.EventMention");
    jcasType.ll_cas.ll_setIntValue(addr, ((EventMention_Type)jcasType).casFeatCode_eventMentionIndex, v);}    
   
    
  //*--------------*
  //* Feature: eventType

  /** getter for eventType - gets 
   * @generated
   * @return value of the feature 
   */
  public String getEventType() {
    if (EventMention_Type.featOkTst && ((EventMention_Type)jcasType).casFeat_eventType == null)
      jcasType.jcas.throwFeatMissing("eventType", "edu.cmu.cs.lti.script.type.EventMention");
    return jcasType.ll_cas.ll_getStringValue(addr, ((EventMention_Type)jcasType).casFeatCode_eventType);}
    
  /** setter for eventType - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setEventType(String v) {
    if (EventMention_Type.featOkTst && ((EventMention_Type)jcasType).casFeat_eventType == null)
      jcasType.jcas.throwFeatMissing("eventType", "edu.cmu.cs.lti.script.type.EventMention");
    jcasType.ll_cas.ll_setStringValue(addr, ((EventMention_Type)jcasType).casFeatCode_eventType, v);}    
   
    
  //*--------------*
  //* Feature: epistemicStatus

  /** getter for epistemicStatus - gets 
   * @generated
   * @return value of the feature 
   */
  public String getEpistemicStatus() {
    if (EventMention_Type.featOkTst && ((EventMention_Type)jcasType).casFeat_epistemicStatus == null)
      jcasType.jcas.throwFeatMissing("epistemicStatus", "edu.cmu.cs.lti.script.type.EventMention");
    return jcasType.ll_cas.ll_getStringValue(addr, ((EventMention_Type)jcasType).casFeatCode_epistemicStatus);}
    
  /** setter for epistemicStatus - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setEpistemicStatus(String v) {
    if (EventMention_Type.featOkTst && ((EventMention_Type)jcasType).casFeat_epistemicStatus == null)
      jcasType.jcas.throwFeatMissing("epistemicStatus", "edu.cmu.cs.lti.script.type.EventMention");
    jcasType.ll_cas.ll_setStringValue(addr, ((EventMention_Type)jcasType).casFeatCode_epistemicStatus, v);}    
   
    
  //*--------------*
  //* Feature: eventCoreferenceClusters

  /** getter for eventCoreferenceClusters - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getEventCoreferenceClusters() {
    if (EventMention_Type.featOkTst && ((EventMention_Type)jcasType).casFeat_eventCoreferenceClusters == null)
      jcasType.jcas.throwFeatMissing("eventCoreferenceClusters", "edu.cmu.cs.lti.script.type.EventMention");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EventMention_Type)jcasType).casFeatCode_eventCoreferenceClusters)));}
    
  /** setter for eventCoreferenceClusters - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setEventCoreferenceClusters(FSList v) {
    if (EventMention_Type.featOkTst && ((EventMention_Type)jcasType).casFeat_eventCoreferenceClusters == null)
      jcasType.jcas.throwFeatMissing("eventCoreferenceClusters", "edu.cmu.cs.lti.script.type.EventMention");
    jcasType.ll_cas.ll_setRefValue(addr, ((EventMention_Type)jcasType).casFeatCode_eventCoreferenceClusters, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: agentLinks

  /** getter for agentLinks - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getAgentLinks() {
    if (EventMention_Type.featOkTst && ((EventMention_Type)jcasType).casFeat_agentLinks == null)
      jcasType.jcas.throwFeatMissing("agentLinks", "edu.cmu.cs.lti.script.type.EventMention");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EventMention_Type)jcasType).casFeatCode_agentLinks)));}
    
  /** setter for agentLinks - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAgentLinks(FSList v) {
    if (EventMention_Type.featOkTst && ((EventMention_Type)jcasType).casFeat_agentLinks == null)
      jcasType.jcas.throwFeatMissing("agentLinks", "edu.cmu.cs.lti.script.type.EventMention");
    jcasType.ll_cas.ll_setRefValue(addr, ((EventMention_Type)jcasType).casFeatCode_agentLinks, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: patientLinks

  /** getter for patientLinks - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getPatientLinks() {
    if (EventMention_Type.featOkTst && ((EventMention_Type)jcasType).casFeat_patientLinks == null)
      jcasType.jcas.throwFeatMissing("patientLinks", "edu.cmu.cs.lti.script.type.EventMention");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EventMention_Type)jcasType).casFeatCode_patientLinks)));}
    
  /** setter for patientLinks - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setPatientLinks(FSList v) {
    if (EventMention_Type.featOkTst && ((EventMention_Type)jcasType).casFeat_patientLinks == null)
      jcasType.jcas.throwFeatMissing("patientLinks", "edu.cmu.cs.lti.script.type.EventMention");
    jcasType.ll_cas.ll_setRefValue(addr, ((EventMention_Type)jcasType).casFeatCode_patientLinks, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: locationLinks

  /** getter for locationLinks - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getLocationLinks() {
    if (EventMention_Type.featOkTst && ((EventMention_Type)jcasType).casFeat_locationLinks == null)
      jcasType.jcas.throwFeatMissing("locationLinks", "edu.cmu.cs.lti.script.type.EventMention");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EventMention_Type)jcasType).casFeatCode_locationLinks)));}
    
  /** setter for locationLinks - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setLocationLinks(FSList v) {
    if (EventMention_Type.featOkTst && ((EventMention_Type)jcasType).casFeat_locationLinks == null)
      jcasType.jcas.throwFeatMissing("locationLinks", "edu.cmu.cs.lti.script.type.EventMention");
    jcasType.ll_cas.ll_setRefValue(addr, ((EventMention_Type)jcasType).casFeatCode_locationLinks, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: timeLinks

  /** getter for timeLinks - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getTimeLinks() {
    if (EventMention_Type.featOkTst && ((EventMention_Type)jcasType).casFeat_timeLinks == null)
      jcasType.jcas.throwFeatMissing("timeLinks", "edu.cmu.cs.lti.script.type.EventMention");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EventMention_Type)jcasType).casFeatCode_timeLinks)));}
    
  /** setter for timeLinks - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setTimeLinks(FSList v) {
    if (EventMention_Type.featOkTst && ((EventMention_Type)jcasType).casFeat_timeLinks == null)
      jcasType.jcas.throwFeatMissing("timeLinks", "edu.cmu.cs.lti.script.type.EventMention");
    jcasType.ll_cas.ll_setRefValue(addr, ((EventMention_Type)jcasType).casFeatCode_timeLinks, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: childEventRelations

  /** getter for childEventRelations - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getChildEventRelations() {
    if (EventMention_Type.featOkTst && ((EventMention_Type)jcasType).casFeat_childEventRelations == null)
      jcasType.jcas.throwFeatMissing("childEventRelations", "edu.cmu.cs.lti.script.type.EventMention");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EventMention_Type)jcasType).casFeatCode_childEventRelations)));}
    
  /** setter for childEventRelations - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setChildEventRelations(FSList v) {
    if (EventMention_Type.featOkTst && ((EventMention_Type)jcasType).casFeat_childEventRelations == null)
      jcasType.jcas.throwFeatMissing("childEventRelations", "edu.cmu.cs.lti.script.type.EventMention");
    jcasType.ll_cas.ll_setRefValue(addr, ((EventMention_Type)jcasType).casFeatCode_childEventRelations, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: headEventRelations

  /** getter for headEventRelations - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getHeadEventRelations() {
    if (EventMention_Type.featOkTst && ((EventMention_Type)jcasType).casFeat_headEventRelations == null)
      jcasType.jcas.throwFeatMissing("headEventRelations", "edu.cmu.cs.lti.script.type.EventMention");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EventMention_Type)jcasType).casFeatCode_headEventRelations)));}
    
  /** setter for headEventRelations - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setHeadEventRelations(FSList v) {
    if (EventMention_Type.featOkTst && ((EventMention_Type)jcasType).casFeat_headEventRelations == null)
      jcasType.jcas.throwFeatMissing("headEventRelations", "edu.cmu.cs.lti.script.type.EventMention");
    jcasType.ll_cas.ll_setRefValue(addr, ((EventMention_Type)jcasType).casFeatCode_headEventRelations, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: headWord

  /** getter for headWord - gets 
   * @generated
   * @return value of the feature 
   */
  public Word getHeadWord() {
    if (EventMention_Type.featOkTst && ((EventMention_Type)jcasType).casFeat_headWord == null)
      jcasType.jcas.throwFeatMissing("headWord", "edu.cmu.cs.lti.script.type.EventMention");
    return (Word)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EventMention_Type)jcasType).casFeatCode_headWord)));}
    
  /** setter for headWord - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setHeadWord(Word v) {
    if (EventMention_Type.featOkTst && ((EventMention_Type)jcasType).casFeat_headWord == null)
      jcasType.jcas.throwFeatMissing("headWord", "edu.cmu.cs.lti.script.type.EventMention");
    jcasType.ll_cas.ll_setRefValue(addr, ((EventMention_Type)jcasType).casFeatCode_headWord, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: singleEventFeatures

  /** getter for singleEventFeatures - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getSingleEventFeatures() {
    if (EventMention_Type.featOkTst && ((EventMention_Type)jcasType).casFeat_singleEventFeatures == null)
      jcasType.jcas.throwFeatMissing("singleEventFeatures", "edu.cmu.cs.lti.script.type.EventMention");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EventMention_Type)jcasType).casFeatCode_singleEventFeatures)));}
    
  /** setter for singleEventFeatures - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setSingleEventFeatures(FSList v) {
    if (EventMention_Type.featOkTst && ((EventMention_Type)jcasType).casFeat_singleEventFeatures == null)
      jcasType.jcas.throwFeatMissing("singleEventFeatures", "edu.cmu.cs.lti.script.type.EventMention");
    jcasType.ll_cas.ll_setRefValue(addr, ((EventMention_Type)jcasType).casFeatCode_singleEventFeatures, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: quantity

  /** getter for quantity - gets The NumberAnnotation indicate the quantity of the event mention, such as "three blasts", the quantity is "three", normalized as 3
   * @generated
   * @return value of the feature 
   */
  public NumberAnnotation getQuantity() {
    if (EventMention_Type.featOkTst && ((EventMention_Type)jcasType).casFeat_quantity == null)
      jcasType.jcas.throwFeatMissing("quantity", "edu.cmu.cs.lti.script.type.EventMention");
    return (NumberAnnotation)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EventMention_Type)jcasType).casFeatCode_quantity)));}
    
  /** setter for quantity - sets The NumberAnnotation indicate the quantity of the event mention, such as "three blasts", the quantity is "three", normalized as 3 
   * @generated
   * @param v value to set into the feature 
   */
  public void setQuantity(NumberAnnotation v) {
    if (EventMention_Type.featOkTst && ((EventMention_Type)jcasType).casFeat_quantity == null)
      jcasType.jcas.throwFeatMissing("quantity", "edu.cmu.cs.lti.script.type.EventMention");
    jcasType.ll_cas.ll_setRefValue(addr, ((EventMention_Type)jcasType).casFeatCode_quantity, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: arguments

  /** getter for arguments - gets The argument list of this event mention
   * @generated
   * @return value of the feature 
   */
  public FSList getArguments() {
    if (EventMention_Type.featOkTst && ((EventMention_Type)jcasType).casFeat_arguments == null)
      jcasType.jcas.throwFeatMissing("arguments", "edu.cmu.cs.lti.script.type.EventMention");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EventMention_Type)jcasType).casFeatCode_arguments)));}
    
  /** setter for arguments - sets The argument list of this event mention 
   * @generated
   * @param v value to set into the feature 
   */
  public void setArguments(FSList v) {
    if (EventMention_Type.featOkTst && ((EventMention_Type)jcasType).casFeat_arguments == null)
      jcasType.jcas.throwFeatMissing("arguments", "edu.cmu.cs.lti.script.type.EventMention");
    jcasType.ll_cas.ll_setRefValue(addr, ((EventMention_Type)jcasType).casFeatCode_arguments, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: mentionContext

  /** getter for mentionContext - gets The full event mention context
   * @generated
   * @return value of the feature 
   */
  public EventMentionContext getMentionContext() {
    if (EventMention_Type.featOkTst && ((EventMention_Type)jcasType).casFeat_mentionContext == null)
      jcasType.jcas.throwFeatMissing("mentionContext", "edu.cmu.cs.lti.script.type.EventMention");
    return (EventMentionContext)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EventMention_Type)jcasType).casFeatCode_mentionContext)));}
    
  /** setter for mentionContext - sets The full event mention context 
   * @generated
   * @param v value to set into the feature 
   */
  public void setMentionContext(EventMentionContext v) {
    if (EventMention_Type.featOkTst && ((EventMention_Type)jcasType).casFeat_mentionContext == null)
      jcasType.jcas.throwFeatMissing("mentionContext", "edu.cmu.cs.lti.script.type.EventMention");
    jcasType.ll_cas.ll_setRefValue(addr, ((EventMention_Type)jcasType).casFeatCode_mentionContext, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: referringEvent

  /** getter for referringEvent - gets The underlying event referring to
   * @generated
   * @return value of the feature 
   */
  public Event getReferringEvent() {
    if (EventMention_Type.featOkTst && ((EventMention_Type)jcasType).casFeat_referringEvent == null)
      jcasType.jcas.throwFeatMissing("referringEvent", "edu.cmu.cs.lti.script.type.EventMention");
    return (Event)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EventMention_Type)jcasType).casFeatCode_referringEvent)));}
    
  /** setter for referringEvent - sets The underlying event referring to 
   * @generated
   * @param v value to set into the feature 
   */
  public void setReferringEvent(Event v) {
    if (EventMention_Type.featOkTst && ((EventMention_Type)jcasType).casFeat_referringEvent == null)
      jcasType.jcas.throwFeatMissing("referringEvent", "edu.cmu.cs.lti.script.type.EventMention");
    jcasType.ll_cas.ll_setRefValue(addr, ((EventMention_Type)jcasType).casFeatCode_referringEvent, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: frameName

  /** getter for frameName - gets The frame name an event evoke (it is still questionable whether we should put this attribute here)
   * @generated
   * @return value of the feature 
   */
  public String getFrameName() {
    if (EventMention_Type.featOkTst && ((EventMention_Type)jcasType).casFeat_frameName == null)
      jcasType.jcas.throwFeatMissing("frameName", "edu.cmu.cs.lti.script.type.EventMention");
    return jcasType.ll_cas.ll_getStringValue(addr, ((EventMention_Type)jcasType).casFeatCode_frameName);}
    
  /** setter for frameName - sets The frame name an event evoke (it is still questionable whether we should put this attribute here) 
   * @generated
   * @param v value to set into the feature 
   */
  public void setFrameName(String v) {
    if (EventMention_Type.featOkTst && ((EventMention_Type)jcasType).casFeat_frameName == null)
      jcasType.jcas.throwFeatMissing("frameName", "edu.cmu.cs.lti.script.type.EventMention");
    jcasType.ll_cas.ll_setStringValue(addr, ((EventMention_Type)jcasType).casFeatCode_frameName, v);}    
  }

    