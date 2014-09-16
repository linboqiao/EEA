

/* First created by JCasGen Mon Sep 15 15:04:26 EDT 2014 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;



/** 
 * Updated by JCasGen Tue Sep 16 00:38:30 EDT 2014
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/TypeSystem.xml
 * @generated */
public class SingleEventFeature extends AbstractFeature {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(SingleEventFeature.class);
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
  protected SingleEventFeature() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public SingleEventFeature(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public SingleEventFeature(JCas jcas) {
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
  //* Feature: featureType

  /** getter for featureType - gets This can take two values now:
1. numeric
2. binary
   * @generated
   * @return value of the feature 
   */
  public String getFeatureType() {
    if (SingleEventFeature_Type.featOkTst && ((SingleEventFeature_Type)jcasType).casFeat_featureType == null)
      jcasType.jcas.throwFeatMissing("featureType", "edu.cmu.cs.lti.script.type.SingleEventFeature");
    return jcasType.ll_cas.ll_getStringValue(addr, ((SingleEventFeature_Type)jcasType).casFeatCode_featureType);}
    
  /** setter for featureType - sets This can take two values now:
1. numeric
2. binary 
   * @generated
   * @param v value to set into the feature 
   */
  public void setFeatureType(String v) {
    if (SingleEventFeature_Type.featOkTst && ((SingleEventFeature_Type)jcasType).casFeat_featureType == null)
      jcasType.jcas.throwFeatMissing("featureType", "edu.cmu.cs.lti.script.type.SingleEventFeature");
    jcasType.ll_cas.ll_setStringValue(addr, ((SingleEventFeature_Type)jcasType).casFeatCode_featureType, v);}    
   
    
  //*--------------*
  //* Feature: defaultZero

  /** getter for defaultZero - gets 
   * @generated
   * @return value of the feature 
   */
  public boolean getDefaultZero() {
    if (SingleEventFeature_Type.featOkTst && ((SingleEventFeature_Type)jcasType).casFeat_defaultZero == null)
      jcasType.jcas.throwFeatMissing("defaultZero", "edu.cmu.cs.lti.script.type.SingleEventFeature");
    return jcasType.ll_cas.ll_getBooleanValue(addr, ((SingleEventFeature_Type)jcasType).casFeatCode_defaultZero);}
    
  /** setter for defaultZero - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setDefaultZero(boolean v) {
    if (SingleEventFeature_Type.featOkTst && ((SingleEventFeature_Type)jcasType).casFeat_defaultZero == null)
      jcasType.jcas.throwFeatMissing("defaultZero", "edu.cmu.cs.lti.script.type.SingleEventFeature");
    jcasType.ll_cas.ll_setBooleanValue(addr, ((SingleEventFeature_Type)jcasType).casFeatCode_defaultZero, v);}    
  }

    