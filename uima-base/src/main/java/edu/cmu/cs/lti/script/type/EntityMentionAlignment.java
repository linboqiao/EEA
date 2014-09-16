

/* First created by JCasGen Mon Sep 15 15:04:26 EDT 2014 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;



/** 
 * Updated by JCasGen Tue Sep 16 00:38:29 EDT 2014
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/TypeSystem.xml
 * @generated */
public class EntityMentionAlignment extends ComponentTOP {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(EntityMentionAlignment.class);
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
  protected EntityMentionAlignment() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public EntityMentionAlignment(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public EntityMentionAlignment(JCas jcas) {
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
    if (EntityMentionAlignment_Type.featOkTst && ((EntityMentionAlignment_Type)jcasType).casFeat_sourceViewName == null)
      jcasType.jcas.throwFeatMissing("sourceViewName", "edu.cmu.cs.lti.script.type.EntityMentionAlignment");
    return jcasType.ll_cas.ll_getStringValue(addr, ((EntityMentionAlignment_Type)jcasType).casFeatCode_sourceViewName);}
    
  /** setter for sourceViewName - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setSourceViewName(String v) {
    if (EntityMentionAlignment_Type.featOkTst && ((EntityMentionAlignment_Type)jcasType).casFeat_sourceViewName == null)
      jcasType.jcas.throwFeatMissing("sourceViewName", "edu.cmu.cs.lti.script.type.EntityMentionAlignment");
    jcasType.ll_cas.ll_setStringValue(addr, ((EntityMentionAlignment_Type)jcasType).casFeatCode_sourceViewName, v);}    
   
    
  //*--------------*
  //* Feature: targetViewName

  /** getter for targetViewName - gets 
   * @generated
   * @return value of the feature 
   */
  public String getTargetViewName() {
    if (EntityMentionAlignment_Type.featOkTst && ((EntityMentionAlignment_Type)jcasType).casFeat_targetViewName == null)
      jcasType.jcas.throwFeatMissing("targetViewName", "edu.cmu.cs.lti.script.type.EntityMentionAlignment");
    return jcasType.ll_cas.ll_getStringValue(addr, ((EntityMentionAlignment_Type)jcasType).casFeatCode_targetViewName);}
    
  /** setter for targetViewName - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setTargetViewName(String v) {
    if (EntityMentionAlignment_Type.featOkTst && ((EntityMentionAlignment_Type)jcasType).casFeat_targetViewName == null)
      jcasType.jcas.throwFeatMissing("targetViewName", "edu.cmu.cs.lti.script.type.EntityMentionAlignment");
    jcasType.ll_cas.ll_setStringValue(addr, ((EntityMentionAlignment_Type)jcasType).casFeatCode_targetViewName, v);}    
   
    
  //*--------------*
  //* Feature: sourceEntityMention

  /** getter for sourceEntityMention - gets 
   * @generated
   * @return value of the feature 
   */
  public StanfordEntityMention getSourceEntityMention() {
    if (EntityMentionAlignment_Type.featOkTst && ((EntityMentionAlignment_Type)jcasType).casFeat_sourceEntityMention == null)
      jcasType.jcas.throwFeatMissing("sourceEntityMention", "edu.cmu.cs.lti.script.type.EntityMentionAlignment");
    return (StanfordEntityMention)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EntityMentionAlignment_Type)jcasType).casFeatCode_sourceEntityMention)));}
    
  /** setter for sourceEntityMention - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setSourceEntityMention(StanfordEntityMention v) {
    if (EntityMentionAlignment_Type.featOkTst && ((EntityMentionAlignment_Type)jcasType).casFeat_sourceEntityMention == null)
      jcasType.jcas.throwFeatMissing("sourceEntityMention", "edu.cmu.cs.lti.script.type.EntityMentionAlignment");
    jcasType.ll_cas.ll_setRefValue(addr, ((EntityMentionAlignment_Type)jcasType).casFeatCode_sourceEntityMention, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: targetEntityMention

  /** getter for targetEntityMention - gets 
   * @generated
   * @return value of the feature 
   */
  public StanfordEntityMention getTargetEntityMention() {
    if (EntityMentionAlignment_Type.featOkTst && ((EntityMentionAlignment_Type)jcasType).casFeat_targetEntityMention == null)
      jcasType.jcas.throwFeatMissing("targetEntityMention", "edu.cmu.cs.lti.script.type.EntityMentionAlignment");
    return (StanfordEntityMention)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EntityMentionAlignment_Type)jcasType).casFeatCode_targetEntityMention)));}
    
  /** setter for targetEntityMention - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setTargetEntityMention(StanfordEntityMention v) {
    if (EntityMentionAlignment_Type.featOkTst && ((EntityMentionAlignment_Type)jcasType).casFeat_targetEntityMention == null)
      jcasType.jcas.throwFeatMissing("targetEntityMention", "edu.cmu.cs.lti.script.type.EntityMentionAlignment");
    jcasType.ll_cas.ll_setRefValue(addr, ((EntityMentionAlignment_Type)jcasType).casFeatCode_targetEntityMention, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    