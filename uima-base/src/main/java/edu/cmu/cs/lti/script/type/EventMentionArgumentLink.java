

/* First created by JCasGen Mon Sep 15 15:04:26 EDT 2014 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.StringList;


/** Link between an event mention to its argument (which is an entity mention)
 * Updated by JCasGen Tue Sep 16 00:38:29 EDT 2014
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/TypeSystem.xml
 * @generated */
public class EventMentionArgumentLink extends ComponentTOP {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(EventMentionArgumentLink.class);
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
  protected EventMentionArgumentLink() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public EventMentionArgumentLink(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public EventMentionArgumentLink(JCas jcas) {
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
  //* Feature: eventMention

  /** getter for eventMention - gets 
   * @generated
   * @return value of the feature 
   */
  public EventMention getEventMention() {
    if (EventMentionArgumentLink_Type.featOkTst && ((EventMentionArgumentLink_Type)jcasType).casFeat_eventMention == null)
      jcasType.jcas.throwFeatMissing("eventMention", "edu.cmu.cs.lti.script.type.EventMentionArgumentLink");
    return (EventMention)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EventMentionArgumentLink_Type)jcasType).casFeatCode_eventMention)));}
    
  /** setter for eventMention - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setEventMention(EventMention v) {
    if (EventMentionArgumentLink_Type.featOkTst && ((EventMentionArgumentLink_Type)jcasType).casFeat_eventMention == null)
      jcasType.jcas.throwFeatMissing("eventMention", "edu.cmu.cs.lti.script.type.EventMentionArgumentLink");
    jcasType.ll_cas.ll_setRefValue(addr, ((EventMentionArgumentLink_Type)jcasType).casFeatCode_eventMention, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: verbNetRoleName

  /** getter for verbNetRoleName - gets The thematic role name in VerbNet
   * @generated
   * @return value of the feature 
   */
  public String getVerbNetRoleName() {
    if (EventMentionArgumentLink_Type.featOkTst && ((EventMentionArgumentLink_Type)jcasType).casFeat_verbNetRoleName == null)
      jcasType.jcas.throwFeatMissing("verbNetRoleName", "edu.cmu.cs.lti.script.type.EventMentionArgumentLink");
    return jcasType.ll_cas.ll_getStringValue(addr, ((EventMentionArgumentLink_Type)jcasType).casFeatCode_verbNetRoleName);}
    
  /** setter for verbNetRoleName - sets The thematic role name in VerbNet 
   * @generated
   * @param v value to set into the feature 
   */
  public void setVerbNetRoleName(String v) {
    if (EventMentionArgumentLink_Type.featOkTst && ((EventMentionArgumentLink_Type)jcasType).casFeat_verbNetRoleName == null)
      jcasType.jcas.throwFeatMissing("verbNetRoleName", "edu.cmu.cs.lti.script.type.EventMentionArgumentLink");
    jcasType.ll_cas.ll_setStringValue(addr, ((EventMentionArgumentLink_Type)jcasType).casFeatCode_verbNetRoleName, v);}    
   
    
  //*--------------*
  //* Feature: frameElementName

  /** getter for frameElementName - gets The frame element name in frame net
   * @generated
   * @return value of the feature 
   */
  public String getFrameElementName() {
    if (EventMentionArgumentLink_Type.featOkTst && ((EventMentionArgumentLink_Type)jcasType).casFeat_frameElementName == null)
      jcasType.jcas.throwFeatMissing("frameElementName", "edu.cmu.cs.lti.script.type.EventMentionArgumentLink");
    return jcasType.ll_cas.ll_getStringValue(addr, ((EventMentionArgumentLink_Type)jcasType).casFeatCode_frameElementName);}
    
  /** setter for frameElementName - sets The frame element name in frame net 
   * @generated
   * @param v value to set into the feature 
   */
  public void setFrameElementName(String v) {
    if (EventMentionArgumentLink_Type.featOkTst && ((EventMentionArgumentLink_Type)jcasType).casFeat_frameElementName == null)
      jcasType.jcas.throwFeatMissing("frameElementName", "edu.cmu.cs.lti.script.type.EventMentionArgumentLink");
    jcasType.ll_cas.ll_setStringValue(addr, ((EventMentionArgumentLink_Type)jcasType).casFeatCode_frameElementName, v);}    
   
    
  //*--------------*
  //* Feature: propbankRoleName

  /** getter for propbankRoleName - gets Argument role name in terms of PropBank
   * @generated
   * @return value of the feature 
   */
  public String getPropbankRoleName() {
    if (EventMentionArgumentLink_Type.featOkTst && ((EventMentionArgumentLink_Type)jcasType).casFeat_propbankRoleName == null)
      jcasType.jcas.throwFeatMissing("propbankRoleName", "edu.cmu.cs.lti.script.type.EventMentionArgumentLink");
    return jcasType.ll_cas.ll_getStringValue(addr, ((EventMentionArgumentLink_Type)jcasType).casFeatCode_propbankRoleName);}
    
  /** setter for propbankRoleName - sets Argument role name in terms of PropBank 
   * @generated
   * @param v value to set into the feature 
   */
  public void setPropbankRoleName(String v) {
    if (EventMentionArgumentLink_Type.featOkTst && ((EventMentionArgumentLink_Type)jcasType).casFeat_propbankRoleName == null)
      jcasType.jcas.throwFeatMissing("propbankRoleName", "edu.cmu.cs.lti.script.type.EventMentionArgumentLink");
    jcasType.ll_cas.ll_setStringValue(addr, ((EventMentionArgumentLink_Type)jcasType).casFeatCode_propbankRoleName, v);}    
   
    
  //*--------------*
  //* Feature: argumentRole

  /** getter for argumentRole - gets If you don't know what kind of argument it is, use this one, otherwise use one of the specific role name such as : "verbNet" "frameElement" or "probank".
   * @generated
   * @return value of the feature 
   */
  public String getArgumentRole() {
    if (EventMentionArgumentLink_Type.featOkTst && ((EventMentionArgumentLink_Type)jcasType).casFeat_argumentRole == null)
      jcasType.jcas.throwFeatMissing("argumentRole", "edu.cmu.cs.lti.script.type.EventMentionArgumentLink");
    return jcasType.ll_cas.ll_getStringValue(addr, ((EventMentionArgumentLink_Type)jcasType).casFeatCode_argumentRole);}
    
  /** setter for argumentRole - sets If you don't know what kind of argument it is, use this one, otherwise use one of the specific role name such as : "verbNet" "frameElement" or "probank". 
   * @generated
   * @param v value to set into the feature 
   */
  public void setArgumentRole(String v) {
    if (EventMentionArgumentLink_Type.featOkTst && ((EventMentionArgumentLink_Type)jcasType).casFeat_argumentRole == null)
      jcasType.jcas.throwFeatMissing("argumentRole", "edu.cmu.cs.lti.script.type.EventMentionArgumentLink");
    jcasType.ll_cas.ll_setStringValue(addr, ((EventMentionArgumentLink_Type)jcasType).casFeatCode_argumentRole, v);}    
   
    
  //*--------------*
  //* Feature: confidence

  /** getter for confidence - gets 
   * @generated
   * @return value of the feature 
   */
  public double getConfidence() {
    if (EventMentionArgumentLink_Type.featOkTst && ((EventMentionArgumentLink_Type)jcasType).casFeat_confidence == null)
      jcasType.jcas.throwFeatMissing("confidence", "edu.cmu.cs.lti.script.type.EventMentionArgumentLink");
    return jcasType.ll_cas.ll_getDoubleValue(addr, ((EventMentionArgumentLink_Type)jcasType).casFeatCode_confidence);}
    
  /** setter for confidence - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setConfidence(double v) {
    if (EventMentionArgumentLink_Type.featOkTst && ((EventMentionArgumentLink_Type)jcasType).casFeat_confidence == null)
      jcasType.jcas.throwFeatMissing("confidence", "edu.cmu.cs.lti.script.type.EventMentionArgumentLink");
    jcasType.ll_cas.ll_setDoubleValue(addr, ((EventMentionArgumentLink_Type)jcasType).casFeatCode_confidence, v);}    
   
    
  //*--------------*
  //* Feature: superFrameElementRoleNames

  /** getter for superFrameElementRoleNames - gets store the super frame element names based on the frame relation table, current format "frameRelationType#superFrameName#superFEname"
   * @generated
   * @return value of the feature 
   */
  public StringList getSuperFrameElementRoleNames() {
    if (EventMentionArgumentLink_Type.featOkTst && ((EventMentionArgumentLink_Type)jcasType).casFeat_superFrameElementRoleNames == null)
      jcasType.jcas.throwFeatMissing("superFrameElementRoleNames", "edu.cmu.cs.lti.script.type.EventMentionArgumentLink");
    return (StringList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EventMentionArgumentLink_Type)jcasType).casFeatCode_superFrameElementRoleNames)));}
    
  /** setter for superFrameElementRoleNames - sets store the super frame element names based on the frame relation table, current format "frameRelationType#superFrameName#superFEname" 
   * @generated
   * @param v value to set into the feature 
   */
  public void setSuperFrameElementRoleNames(StringList v) {
    if (EventMentionArgumentLink_Type.featOkTst && ((EventMentionArgumentLink_Type)jcasType).casFeat_superFrameElementRoleNames == null)
      jcasType.jcas.throwFeatMissing("superFrameElementRoleNames", "edu.cmu.cs.lti.script.type.EventMentionArgumentLink");
    jcasType.ll_cas.ll_setRefValue(addr, ((EventMentionArgumentLink_Type)jcasType).casFeatCode_superFrameElementRoleNames, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: argument

  /** getter for argument - gets The argument attached to this link, represented as an Entity Based Component
   * @generated
   * @return value of the feature 
   */
  public EntityMention getArgument() {
    if (EventMentionArgumentLink_Type.featOkTst && ((EventMentionArgumentLink_Type)jcasType).casFeat_argument == null)
      jcasType.jcas.throwFeatMissing("argument", "edu.cmu.cs.lti.script.type.EventMentionArgumentLink");
    return (EntityMention)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EventMentionArgumentLink_Type)jcasType).casFeatCode_argument)));}
    
  /** setter for argument - sets The argument attached to this link, represented as an Entity Based Component 
   * @generated
   * @param v value to set into the feature 
   */
  public void setArgument(EntityMention v) {
    if (EventMentionArgumentLink_Type.featOkTst && ((EventMentionArgumentLink_Type)jcasType).casFeat_argument == null)
      jcasType.jcas.throwFeatMissing("argument", "edu.cmu.cs.lti.script.type.EventMentionArgumentLink");
    jcasType.ll_cas.ll_setRefValue(addr, ((EventMentionArgumentLink_Type)jcasType).casFeatCode_argument, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    