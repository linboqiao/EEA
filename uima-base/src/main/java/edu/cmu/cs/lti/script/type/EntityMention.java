

/* First created by JCasGen Mon Sep 15 15:04:26 EDT 2014 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;



/** 
 * Updated by JCasGen Tue Sep 16 00:38:29 EDT 2014
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/TypeSystem.xml
 * @generated */
public class EntityMention extends ComponentAnnotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(EntityMention.class);
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
  protected EntityMention() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public EntityMention(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public EntityMention(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public EntityMention(JCas jcas, int begin, int end) {
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
  //* Feature: entityType

  /** getter for entityType - gets 
   * @generated
   * @return value of the feature 
   */
  public String getEntityType() {
    if (EntityMention_Type.featOkTst && ((EntityMention_Type)jcasType).casFeat_entityType == null)
      jcasType.jcas.throwFeatMissing("entityType", "edu.cmu.cs.lti.script.type.EntityMention");
    return jcasType.ll_cas.ll_getStringValue(addr, ((EntityMention_Type)jcasType).casFeatCode_entityType);}
    
  /** setter for entityType - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setEntityType(String v) {
    if (EntityMention_Type.featOkTst && ((EntityMention_Type)jcasType).casFeat_entityType == null)
      jcasType.jcas.throwFeatMissing("entityType", "edu.cmu.cs.lti.script.type.EntityMention");
    jcasType.ll_cas.ll_setStringValue(addr, ((EntityMention_Type)jcasType).casFeatCode_entityType, v);}    
   
    
  //*--------------*
  //* Feature: head

  /** getter for head - gets 
   * @generated
   * @return value of the feature 
   */
  public Word getHead() {
    if (EntityMention_Type.featOkTst && ((EntityMention_Type)jcasType).casFeat_head == null)
      jcasType.jcas.throwFeatMissing("head", "edu.cmu.cs.lti.script.type.EntityMention");
    return (Word)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EntityMention_Type)jcasType).casFeatCode_head)));}
    
  /** setter for head - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setHead(Word v) {
    if (EntityMention_Type.featOkTst && ((EntityMention_Type)jcasType).casFeat_head == null)
      jcasType.jcas.throwFeatMissing("head", "edu.cmu.cs.lti.script.type.EntityMention");
    jcasType.ll_cas.ll_setRefValue(addr, ((EntityMention_Type)jcasType).casFeatCode_head, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: referingEntity

  /** getter for referingEntity - gets 
   * @generated
   * @return value of the feature 
   */
  public Entity getReferingEntity() {
    if (EntityMention_Type.featOkTst && ((EntityMention_Type)jcasType).casFeat_referingEntity == null)
      jcasType.jcas.throwFeatMissing("referingEntity", "edu.cmu.cs.lti.script.type.EntityMention");
    return (Entity)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EntityMention_Type)jcasType).casFeatCode_referingEntity)));}
    
  /** setter for referingEntity - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setReferingEntity(Entity v) {
    if (EntityMention_Type.featOkTst && ((EntityMention_Type)jcasType).casFeat_referingEntity == null)
      jcasType.jcas.throwFeatMissing("referingEntity", "edu.cmu.cs.lti.script.type.EntityMention");
    jcasType.ll_cas.ll_setRefValue(addr, ((EntityMention_Type)jcasType).casFeatCode_referingEntity, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    