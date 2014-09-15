

/* First created by JCasGen Sat Sep 13 18:23:45 EDT 2014 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;



/** 
 * Updated by JCasGen Sun Sep 14 23:46:31 EDT 2014
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/TypeSystem.xml
 * @generated */
public class PairwiseEventFeature extends AbstractFeature {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(PairwiseEventFeature.class);
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
  protected PairwiseEventFeature() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public PairwiseEventFeature(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public PairwiseEventFeature(JCas jcas) {
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
    if (PairwiseEventFeature_Type.featOkTst && ((PairwiseEventFeature_Type)jcasType).casFeat_featureType == null)
      jcasType.jcas.throwFeatMissing("featureType", "edu.cmu.cs.lti.script.type.PairwiseEventFeature");
    return jcasType.ll_cas.ll_getStringValue(addr, ((PairwiseEventFeature_Type)jcasType).casFeatCode_featureType);}
    
  /** setter for featureType - sets This can take two values now:
1. numeric
2. binary 
   * @generated
   * @param v value to set into the feature 
   */
  public void setFeatureType(String v) {
    if (PairwiseEventFeature_Type.featOkTst && ((PairwiseEventFeature_Type)jcasType).casFeat_featureType == null)
      jcasType.jcas.throwFeatMissing("featureType", "edu.cmu.cs.lti.script.type.PairwiseEventFeature");
    jcasType.ll_cas.ll_setStringValue(addr, ((PairwiseEventFeature_Type)jcasType).casFeatCode_featureType, v);}    
   
    
  //*--------------*
  //* Feature: defaultZero

  /** getter for defaultZero - gets Indicating the default value of this feature is 0
   * @generated
   * @return value of the feature 
   */
  public boolean getDefaultZero() {
    if (PairwiseEventFeature_Type.featOkTst && ((PairwiseEventFeature_Type)jcasType).casFeat_defaultZero == null)
      jcasType.jcas.throwFeatMissing("defaultZero", "edu.cmu.cs.lti.script.type.PairwiseEventFeature");
    return jcasType.ll_cas.ll_getBooleanValue(addr, ((PairwiseEventFeature_Type)jcasType).casFeatCode_defaultZero);}
    
  /** setter for defaultZero - sets Indicating the default value of this feature is 0 
   * @generated
   * @param v value to set into the feature 
   */
  public void setDefaultZero(boolean v) {
    if (PairwiseEventFeature_Type.featOkTst && ((PairwiseEventFeature_Type)jcasType).casFeat_defaultZero == null)
      jcasType.jcas.throwFeatMissing("defaultZero", "edu.cmu.cs.lti.script.type.PairwiseEventFeature");
    jcasType.ll_cas.ll_setBooleanValue(addr, ((PairwiseEventFeature_Type)jcasType).casFeatCode_defaultZero, v);}    
  }

    