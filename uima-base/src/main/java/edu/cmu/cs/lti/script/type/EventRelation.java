

/* First created by JCasGen Mon Sep 15 15:04:26 EDT 2014 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;



/** Consider change this name
 * Updated by JCasGen Tue Sep 16 00:38:29 EDT 2014
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/TypeSystem.xml
 * @generated */
public class EventRelation extends ComponentTOP {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(EventRelation.class);
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
  protected EventRelation() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public EventRelation(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public EventRelation(JCas jcas) {
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
    if (EventRelation_Type.featOkTst && ((EventRelation_Type)jcasType).casFeat_relationType == null)
      jcasType.jcas.throwFeatMissing("relationType", "edu.cmu.cs.lti.script.type.EventRelation");
    return jcasType.ll_cas.ll_getStringValue(addr, ((EventRelation_Type)jcasType).casFeatCode_relationType);}
    
  /** setter for relationType - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setRelationType(String v) {
    if (EventRelation_Type.featOkTst && ((EventRelation_Type)jcasType).casFeat_relationType == null)
      jcasType.jcas.throwFeatMissing("relationType", "edu.cmu.cs.lti.script.type.EventRelation");
    jcasType.ll_cas.ll_setStringValue(addr, ((EventRelation_Type)jcasType).casFeatCode_relationType, v);}    
   
    
  //*--------------*
  //* Feature: head

  /** getter for head - gets 
   * @generated
   * @return value of the feature 
   */
  public EventMention getHead() {
    if (EventRelation_Type.featOkTst && ((EventRelation_Type)jcasType).casFeat_head == null)
      jcasType.jcas.throwFeatMissing("head", "edu.cmu.cs.lti.script.type.EventRelation");
    return (EventMention)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EventRelation_Type)jcasType).casFeatCode_head)));}
    
  /** setter for head - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setHead(EventMention v) {
    if (EventRelation_Type.featOkTst && ((EventRelation_Type)jcasType).casFeat_head == null)
      jcasType.jcas.throwFeatMissing("head", "edu.cmu.cs.lti.script.type.EventRelation");
    jcasType.ll_cas.ll_setRefValue(addr, ((EventRelation_Type)jcasType).casFeatCode_head, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: child

  /** getter for child - gets 
   * @generated
   * @return value of the feature 
   */
  public EventMention getChild() {
    if (EventRelation_Type.featOkTst && ((EventRelation_Type)jcasType).casFeat_child == null)
      jcasType.jcas.throwFeatMissing("child", "edu.cmu.cs.lti.script.type.EventRelation");
    return (EventMention)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EventRelation_Type)jcasType).casFeatCode_child)));}
    
  /** setter for child - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setChild(EventMention v) {
    if (EventRelation_Type.featOkTst && ((EventRelation_Type)jcasType).casFeat_child == null)
      jcasType.jcas.throwFeatMissing("child", "edu.cmu.cs.lti.script.type.EventRelation");
    jcasType.ll_cas.ll_setRefValue(addr, ((EventRelation_Type)jcasType).casFeatCode_child, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    