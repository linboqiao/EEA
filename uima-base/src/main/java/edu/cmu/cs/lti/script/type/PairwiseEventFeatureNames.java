

/* First created by JCasGen Mon Sep 15 15:04:26 EDT 2014 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.StringList;


/** 
 * Updated by JCasGen Tue Sep 16 00:38:30 EDT 2014
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/TypeSystem.xml
 * @generated */
public class PairwiseEventFeatureNames extends ComponentTOP {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(PairwiseEventFeatureNames.class);
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
  protected PairwiseEventFeatureNames() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public PairwiseEventFeatureNames(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public PairwiseEventFeatureNames(JCas jcas) {
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
  //* Feature: featureNames

  /** getter for featureNames - gets 
   * @generated
   * @return value of the feature 
   */
  public StringList getFeatureNames() {
    if (PairwiseEventFeatureNames_Type.featOkTst && ((PairwiseEventFeatureNames_Type)jcasType).casFeat_featureNames == null)
      jcasType.jcas.throwFeatMissing("featureNames", "edu.cmu.cs.lti.script.type.PairwiseEventFeatureNames");
    return (StringList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((PairwiseEventFeatureNames_Type)jcasType).casFeatCode_featureNames)));}
    
  /** setter for featureNames - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setFeatureNames(StringList v) {
    if (PairwiseEventFeatureNames_Type.featOkTst && ((PairwiseEventFeatureNames_Type)jcasType).casFeat_featureNames == null)
      jcasType.jcas.throwFeatMissing("featureNames", "edu.cmu.cs.lti.script.type.PairwiseEventFeatureNames");
    jcasType.ll_cas.ll_setRefValue(addr, ((PairwiseEventFeatureNames_Type)jcasType).casFeatCode_featureNames, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    