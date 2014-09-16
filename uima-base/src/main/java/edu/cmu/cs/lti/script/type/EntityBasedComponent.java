

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
public class EntityBasedComponent extends ComponentAnnotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(EntityBasedComponent.class);
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
  protected EntityBasedComponent() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public EntityBasedComponent(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public EntityBasedComponent(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public EntityBasedComponent(JCas jcas, int begin, int end) {
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
  //* Feature: containingEntityMentions

  /** getter for containingEntityMentions - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getContainingEntityMentions() {
    if (EntityBasedComponent_Type.featOkTst && ((EntityBasedComponent_Type)jcasType).casFeat_containingEntityMentions == null)
      jcasType.jcas.throwFeatMissing("containingEntityMentions", "edu.cmu.cs.lti.script.type.EntityBasedComponent");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EntityBasedComponent_Type)jcasType).casFeatCode_containingEntityMentions)));}
    
  /** setter for containingEntityMentions - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setContainingEntityMentions(FSList v) {
    if (EntityBasedComponent_Type.featOkTst && ((EntityBasedComponent_Type)jcasType).casFeat_containingEntityMentions == null)
      jcasType.jcas.throwFeatMissing("containingEntityMentions", "edu.cmu.cs.lti.script.type.EntityBasedComponent");
    jcasType.ll_cas.ll_setRefValue(addr, ((EntityBasedComponent_Type)jcasType).casFeatCode_containingEntityMentions, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: componentLinks

  /** getter for componentLinks - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getComponentLinks() {
    if (EntityBasedComponent_Type.featOkTst && ((EntityBasedComponent_Type)jcasType).casFeat_componentLinks == null)
      jcasType.jcas.throwFeatMissing("componentLinks", "edu.cmu.cs.lti.script.type.EntityBasedComponent");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EntityBasedComponent_Type)jcasType).casFeatCode_componentLinks)));}
    
  /** setter for componentLinks - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setComponentLinks(FSList v) {
    if (EntityBasedComponent_Type.featOkTst && ((EntityBasedComponent_Type)jcasType).casFeat_componentLinks == null)
      jcasType.jcas.throwFeatMissing("componentLinks", "edu.cmu.cs.lti.script.type.EntityBasedComponent");
    jcasType.ll_cas.ll_setRefValue(addr, ((EntityBasedComponent_Type)jcasType).casFeatCode_componentLinks, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: headWord

  /** getter for headWord - gets 
   * @generated
   * @return value of the feature 
   */
  public Word getHeadWord() {
    if (EntityBasedComponent_Type.featOkTst && ((EntityBasedComponent_Type)jcasType).casFeat_headWord == null)
      jcasType.jcas.throwFeatMissing("headWord", "edu.cmu.cs.lti.script.type.EntityBasedComponent");
    return (Word)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EntityBasedComponent_Type)jcasType).casFeatCode_headWord)));}
    
  /** setter for headWord - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setHeadWord(Word v) {
    if (EntityBasedComponent_Type.featOkTst && ((EntityBasedComponent_Type)jcasType).casFeat_headWord == null)
      jcasType.jcas.throwFeatMissing("headWord", "edu.cmu.cs.lti.script.type.EntityBasedComponent");
    jcasType.ll_cas.ll_setRefValue(addr, ((EntityBasedComponent_Type)jcasType).casFeatCode_headWord, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: quantity

  /** getter for quantity - gets The associated quantity of this annotation
   * @generated
   * @return value of the feature 
   */
  public NumberAnnotation getQuantity() {
    if (EntityBasedComponent_Type.featOkTst && ((EntityBasedComponent_Type)jcasType).casFeat_quantity == null)
      jcasType.jcas.throwFeatMissing("quantity", "edu.cmu.cs.lti.script.type.EntityBasedComponent");
    return (NumberAnnotation)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EntityBasedComponent_Type)jcasType).casFeatCode_quantity)));}
    
  /** setter for quantity - sets The associated quantity of this annotation 
   * @generated
   * @param v value to set into the feature 
   */
  public void setQuantity(NumberAnnotation v) {
    if (EntityBasedComponent_Type.featOkTst && ((EntityBasedComponent_Type)jcasType).casFeat_quantity == null)
      jcasType.jcas.throwFeatMissing("quantity", "edu.cmu.cs.lti.script.type.EntityBasedComponent");
    jcasType.ll_cas.ll_setRefValue(addr, ((EntityBasedComponent_Type)jcasType).casFeatCode_quantity, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    