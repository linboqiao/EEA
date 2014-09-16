

/* First created by JCasGen Mon Sep 15 15:04:26 EDT 2014 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;



/** 
 * Updated by JCasGen Tue Sep 16 00:38:29 EDT 2014
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/TypeSystem.xml
 * @generated */
public class EntityAlignment extends ComponentTOP {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(EntityAlignment.class);
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
  protected EntityAlignment() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public EntityAlignment(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public EntityAlignment(JCas jcas) {
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
  //* Feature: sourceViewName

  /** getter for sourceViewName - gets 
   * @generated
   * @return value of the feature 
   */
  public String getSourceViewName() {
    if (EntityAlignment_Type.featOkTst && ((EntityAlignment_Type)jcasType).casFeat_sourceViewName == null)
      jcasType.jcas.throwFeatMissing("sourceViewName", "edu.cmu.cs.lti.script.type.EntityAlignment");
    return jcasType.ll_cas.ll_getStringValue(addr, ((EntityAlignment_Type)jcasType).casFeatCode_sourceViewName);}
    
  /** setter for sourceViewName - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setSourceViewName(String v) {
    if (EntityAlignment_Type.featOkTst && ((EntityAlignment_Type)jcasType).casFeat_sourceViewName == null)
      jcasType.jcas.throwFeatMissing("sourceViewName", "edu.cmu.cs.lti.script.type.EntityAlignment");
    jcasType.ll_cas.ll_setStringValue(addr, ((EntityAlignment_Type)jcasType).casFeatCode_sourceViewName, v);}    
   
    
  //*--------------*
  //* Feature: targetViewName

  /** getter for targetViewName - gets 
   * @generated
   * @return value of the feature 
   */
  public String getTargetViewName() {
    if (EntityAlignment_Type.featOkTst && ((EntityAlignment_Type)jcasType).casFeat_targetViewName == null)
      jcasType.jcas.throwFeatMissing("targetViewName", "edu.cmu.cs.lti.script.type.EntityAlignment");
    return jcasType.ll_cas.ll_getStringValue(addr, ((EntityAlignment_Type)jcasType).casFeatCode_targetViewName);}
    
  /** setter for targetViewName - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setTargetViewName(String v) {
    if (EntityAlignment_Type.featOkTst && ((EntityAlignment_Type)jcasType).casFeat_targetViewName == null)
      jcasType.jcas.throwFeatMissing("targetViewName", "edu.cmu.cs.lti.script.type.EntityAlignment");
    jcasType.ll_cas.ll_setStringValue(addr, ((EntityAlignment_Type)jcasType).casFeatCode_targetViewName, v);}    
   
    
  //*--------------*
  //* Feature: sourceEntity

  /** getter for sourceEntity - gets 
   * @generated
   * @return value of the feature 
   */
  public Entity getSourceEntity() {
    if (EntityAlignment_Type.featOkTst && ((EntityAlignment_Type)jcasType).casFeat_sourceEntity == null)
      jcasType.jcas.throwFeatMissing("sourceEntity", "edu.cmu.cs.lti.script.type.EntityAlignment");
    return (Entity)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EntityAlignment_Type)jcasType).casFeatCode_sourceEntity)));}
    
  /** setter for sourceEntity - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setSourceEntity(Entity v) {
    if (EntityAlignment_Type.featOkTst && ((EntityAlignment_Type)jcasType).casFeat_sourceEntity == null)
      jcasType.jcas.throwFeatMissing("sourceEntity", "edu.cmu.cs.lti.script.type.EntityAlignment");
    jcasType.ll_cas.ll_setRefValue(addr, ((EntityAlignment_Type)jcasType).casFeatCode_sourceEntity, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: targetEntity

  /** getter for targetEntity - gets 
   * @generated
   * @return value of the feature 
   */
  public Entity getTargetEntity() {
    if (EntityAlignment_Type.featOkTst && ((EntityAlignment_Type)jcasType).casFeat_targetEntity == null)
      jcasType.jcas.throwFeatMissing("targetEntity", "edu.cmu.cs.lti.script.type.EntityAlignment");
    return (Entity)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EntityAlignment_Type)jcasType).casFeatCode_targetEntity)));}
    
  /** setter for targetEntity - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setTargetEntity(Entity v) {
    if (EntityAlignment_Type.featOkTst && ((EntityAlignment_Type)jcasType).casFeat_targetEntity == null)
      jcasType.jcas.throwFeatMissing("targetEntity", "edu.cmu.cs.lti.script.type.EntityAlignment");
    jcasType.ll_cas.ll_setRefValue(addr, ((EntityAlignment_Type)jcasType).casFeatCode_targetEntity, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    