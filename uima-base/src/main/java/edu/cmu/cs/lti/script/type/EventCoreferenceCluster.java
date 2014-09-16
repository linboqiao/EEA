

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
public class EventCoreferenceCluster extends ComponentTOP {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(EventCoreferenceCluster.class);
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
  protected EventCoreferenceCluster() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public EventCoreferenceCluster(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public EventCoreferenceCluster(JCas jcas) {
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
  //* Feature: clusterType

  /** getter for clusterType - gets 
   * @generated
   * @return value of the feature 
   */
  public String getClusterType() {
    if (EventCoreferenceCluster_Type.featOkTst && ((EventCoreferenceCluster_Type)jcasType).casFeat_clusterType == null)
      jcasType.jcas.throwFeatMissing("clusterType", "edu.cmu.cs.lti.script.type.EventCoreferenceCluster");
    return jcasType.ll_cas.ll_getStringValue(addr, ((EventCoreferenceCluster_Type)jcasType).casFeatCode_clusterType);}
    
  /** setter for clusterType - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setClusterType(String v) {
    if (EventCoreferenceCluster_Type.featOkTst && ((EventCoreferenceCluster_Type)jcasType).casFeat_clusterType == null)
      jcasType.jcas.throwFeatMissing("clusterType", "edu.cmu.cs.lti.script.type.EventCoreferenceCluster");
    jcasType.ll_cas.ll_setStringValue(addr, ((EventCoreferenceCluster_Type)jcasType).casFeatCode_clusterType, v);}    
   
    
  //*--------------*
  //* Feature: parentEventMention

  /** getter for parentEventMention - gets 
   * @generated
   * @return value of the feature 
   */
  public EventMention getParentEventMention() {
    if (EventCoreferenceCluster_Type.featOkTst && ((EventCoreferenceCluster_Type)jcasType).casFeat_parentEventMention == null)
      jcasType.jcas.throwFeatMissing("parentEventMention", "edu.cmu.cs.lti.script.type.EventCoreferenceCluster");
    return (EventMention)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EventCoreferenceCluster_Type)jcasType).casFeatCode_parentEventMention)));}
    
  /** setter for parentEventMention - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setParentEventMention(EventMention v) {
    if (EventCoreferenceCluster_Type.featOkTst && ((EventCoreferenceCluster_Type)jcasType).casFeat_parentEventMention == null)
      jcasType.jcas.throwFeatMissing("parentEventMention", "edu.cmu.cs.lti.script.type.EventCoreferenceCluster");
    jcasType.ll_cas.ll_setRefValue(addr, ((EventCoreferenceCluster_Type)jcasType).casFeatCode_parentEventMention, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: childEventMentions

  /** getter for childEventMentions - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getChildEventMentions() {
    if (EventCoreferenceCluster_Type.featOkTst && ((EventCoreferenceCluster_Type)jcasType).casFeat_childEventMentions == null)
      jcasType.jcas.throwFeatMissing("childEventMentions", "edu.cmu.cs.lti.script.type.EventCoreferenceCluster");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EventCoreferenceCluster_Type)jcasType).casFeatCode_childEventMentions)));}
    
  /** setter for childEventMentions - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setChildEventMentions(FSList v) {
    if (EventCoreferenceCluster_Type.featOkTst && ((EventCoreferenceCluster_Type)jcasType).casFeat_childEventMentions == null)
      jcasType.jcas.throwFeatMissing("childEventMentions", "edu.cmu.cs.lti.script.type.EventCoreferenceCluster");
    jcasType.ll_cas.ll_setRefValue(addr, ((EventCoreferenceCluster_Type)jcasType).casFeatCode_childEventMentions, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    