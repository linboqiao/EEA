

/* First created by JCasGen Sat Sep 13 18:23:45 EDT 2014 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.FSList;


/** 
 * Updated by JCasGen Sun Sep 14 23:46:31 EDT 2014
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/TypeSystem.xml
 * @generated */
public class TokenAlignment extends ComponentTOP {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(TokenAlignment.class);
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
  protected TokenAlignment() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public TokenAlignment(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public TokenAlignment(JCas jcas) {
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
  //* Feature: targetViewName

  /** getter for targetViewName - gets 
   * @generated
   * @return value of the feature 
   */
  public String getTargetViewName() {
    if (TokenAlignment_Type.featOkTst && ((TokenAlignment_Type)jcasType).casFeat_targetViewName == null)
      jcasType.jcas.throwFeatMissing("targetViewName", "edu.cmu.cs.lti.script.type.TokenAlignment");
    return jcasType.ll_cas.ll_getStringValue(addr, ((TokenAlignment_Type)jcasType).casFeatCode_targetViewName);}
    
  /** setter for targetViewName - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setTargetViewName(String v) {
    if (TokenAlignment_Type.featOkTst && ((TokenAlignment_Type)jcasType).casFeat_targetViewName == null)
      jcasType.jcas.throwFeatMissing("targetViewName", "edu.cmu.cs.lti.script.type.TokenAlignment");
    jcasType.ll_cas.ll_setStringValue(addr, ((TokenAlignment_Type)jcasType).casFeatCode_targetViewName, v);}    
   
    
  //*--------------*
  //* Feature: sourceToken

  /** getter for sourceToken - gets 
   * @generated
   * @return value of the feature 
   */
  public StanfordCorenlpToken getSourceToken() {
    if (TokenAlignment_Type.featOkTst && ((TokenAlignment_Type)jcasType).casFeat_sourceToken == null)
      jcasType.jcas.throwFeatMissing("sourceToken", "edu.cmu.cs.lti.script.type.TokenAlignment");
    return (StanfordCorenlpToken)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((TokenAlignment_Type)jcasType).casFeatCode_sourceToken)));}
    
  /** setter for sourceToken - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setSourceToken(StanfordCorenlpToken v) {
    if (TokenAlignment_Type.featOkTst && ((TokenAlignment_Type)jcasType).casFeat_sourceToken == null)
      jcasType.jcas.throwFeatMissing("sourceToken", "edu.cmu.cs.lti.script.type.TokenAlignment");
    jcasType.ll_cas.ll_setRefValue(addr, ((TokenAlignment_Type)jcasType).casFeatCode_sourceToken, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: targetToken

  /** getter for targetToken - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getTargetToken() {
    if (TokenAlignment_Type.featOkTst && ((TokenAlignment_Type)jcasType).casFeat_targetToken == null)
      jcasType.jcas.throwFeatMissing("targetToken", "edu.cmu.cs.lti.script.type.TokenAlignment");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((TokenAlignment_Type)jcasType).casFeatCode_targetToken)));}
    
  /** setter for targetToken - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setTargetToken(FSList v) {
    if (TokenAlignment_Type.featOkTst && ((TokenAlignment_Type)jcasType).casFeat_targetToken == null)
      jcasType.jcas.throwFeatMissing("targetToken", "edu.cmu.cs.lti.script.type.TokenAlignment");
    jcasType.ll_cas.ll_setRefValue(addr, ((TokenAlignment_Type)jcasType).casFeatCode_targetToken, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: sourceViewName

  /** getter for sourceViewName - gets 
   * @generated
   * @return value of the feature 
   */
  public String getSourceViewName() {
    if (TokenAlignment_Type.featOkTst && ((TokenAlignment_Type)jcasType).casFeat_sourceViewName == null)
      jcasType.jcas.throwFeatMissing("sourceViewName", "edu.cmu.cs.lti.script.type.TokenAlignment");
    return jcasType.ll_cas.ll_getStringValue(addr, ((TokenAlignment_Type)jcasType).casFeatCode_sourceViewName);}
    
  /** setter for sourceViewName - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setSourceViewName(String v) {
    if (TokenAlignment_Type.featOkTst && ((TokenAlignment_Type)jcasType).casFeat_sourceViewName == null)
      jcasType.jcas.throwFeatMissing("sourceViewName", "edu.cmu.cs.lti.script.type.TokenAlignment");
    jcasType.ll_cas.ll_setStringValue(addr, ((TokenAlignment_Type)jcasType).casFeatCode_sourceViewName, v);}    
  }

    