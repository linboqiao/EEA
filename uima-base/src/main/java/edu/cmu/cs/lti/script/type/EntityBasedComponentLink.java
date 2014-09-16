

/* First created by JCasGen Mon Sep 15 15:04:26 EDT 2014 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;



/** 
 * Updated by JCasGen Tue Sep 16 00:38:29 EDT 2014
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/TypeSystem.xml
 * @generated */
public class EntityBasedComponentLink extends ComponentTOP {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(EntityBasedComponentLink.class);
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
  protected EntityBasedComponentLink() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public EntityBasedComponentLink(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public EntityBasedComponentLink(JCas jcas) {
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
    if (EntityBasedComponentLink_Type.featOkTst && ((EntityBasedComponentLink_Type)jcasType).casFeat_eventMention == null)
      jcasType.jcas.throwFeatMissing("eventMention", "edu.cmu.cs.lti.script.type.EntityBasedComponentLink");
    return (EventMention)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EntityBasedComponentLink_Type)jcasType).casFeatCode_eventMention)));}
    
  /** setter for eventMention - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setEventMention(EventMention v) {
    if (EntityBasedComponentLink_Type.featOkTst && ((EntityBasedComponentLink_Type)jcasType).casFeat_eventMention == null)
      jcasType.jcas.throwFeatMissing("eventMention", "edu.cmu.cs.lti.script.type.EntityBasedComponentLink");
    jcasType.ll_cas.ll_setRefValue(addr, ((EntityBasedComponentLink_Type)jcasType).casFeatCode_eventMention, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: argument

  /** getter for argument - gets 
   * @generated
   * @return value of the feature 
   */
  public EntityBasedComponent getArgument() {
    if (EntityBasedComponentLink_Type.featOkTst && ((EntityBasedComponentLink_Type)jcasType).casFeat_argument == null)
      jcasType.jcas.throwFeatMissing("argument", "edu.cmu.cs.lti.script.type.EntityBasedComponentLink");
    return (EntityBasedComponent)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EntityBasedComponentLink_Type)jcasType).casFeatCode_argument)));}
    
  /** setter for argument - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setArgument(EntityBasedComponent v) {
    if (EntityBasedComponentLink_Type.featOkTst && ((EntityBasedComponentLink_Type)jcasType).casFeat_argument == null)
      jcasType.jcas.throwFeatMissing("argument", "edu.cmu.cs.lti.script.type.EntityBasedComponentLink");
    jcasType.ll_cas.ll_setRefValue(addr, ((EntityBasedComponentLink_Type)jcasType).casFeatCode_argument, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    