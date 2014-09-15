

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
public class FanseToken extends Word {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(FanseToken.class);
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
  protected FanseToken() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public FanseToken(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public FanseToken(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public FanseToken(JCas jcas, int begin, int end) {
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
  //* Feature: coarsePos

  /** getter for coarsePos - gets 
   * @generated
   * @return value of the feature 
   */
  public String getCoarsePos() {
    if (FanseToken_Type.featOkTst && ((FanseToken_Type)jcasType).casFeat_coarsePos == null)
      jcasType.jcas.throwFeatMissing("coarsePos", "edu.cmu.cs.lti.script.type.FanseToken");
    return jcasType.ll_cas.ll_getStringValue(addr, ((FanseToken_Type)jcasType).casFeatCode_coarsePos);}
    
  /** setter for coarsePos - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setCoarsePos(String v) {
    if (FanseToken_Type.featOkTst && ((FanseToken_Type)jcasType).casFeat_coarsePos == null)
      jcasType.jcas.throwFeatMissing("coarsePos", "edu.cmu.cs.lti.script.type.FanseToken");
    jcasType.ll_cas.ll_setStringValue(addr, ((FanseToken_Type)jcasType).casFeatCode_coarsePos, v);}    
   
    
  //*--------------*
  //* Feature: pos

  /** getter for pos - gets 
   * @generated
   * @return value of the feature 
   */
  public String getPos() {
    if (FanseToken_Type.featOkTst && ((FanseToken_Type)jcasType).casFeat_pos == null)
      jcasType.jcas.throwFeatMissing("pos", "edu.cmu.cs.lti.script.type.FanseToken");
    return jcasType.ll_cas.ll_getStringValue(addr, ((FanseToken_Type)jcasType).casFeatCode_pos);}
    
  /** setter for pos - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setPos(String v) {
    if (FanseToken_Type.featOkTst && ((FanseToken_Type)jcasType).casFeat_pos == null)
      jcasType.jcas.throwFeatMissing("pos", "edu.cmu.cs.lti.script.type.FanseToken");
    jcasType.ll_cas.ll_setStringValue(addr, ((FanseToken_Type)jcasType).casFeatCode_pos, v);}    
   
    
  //*--------------*
  //* Feature: lexicalSense

  /** getter for lexicalSense - gets 
   * @generated
   * @return value of the feature 
   */
  public String getLexicalSense() {
    if (FanseToken_Type.featOkTst && ((FanseToken_Type)jcasType).casFeat_lexicalSense == null)
      jcasType.jcas.throwFeatMissing("lexicalSense", "edu.cmu.cs.lti.script.type.FanseToken");
    return jcasType.ll_cas.ll_getStringValue(addr, ((FanseToken_Type)jcasType).casFeatCode_lexicalSense);}
    
  /** setter for lexicalSense - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setLexicalSense(String v) {
    if (FanseToken_Type.featOkTst && ((FanseToken_Type)jcasType).casFeat_lexicalSense == null)
      jcasType.jcas.throwFeatMissing("lexicalSense", "edu.cmu.cs.lti.script.type.FanseToken");
    jcasType.ll_cas.ll_setStringValue(addr, ((FanseToken_Type)jcasType).casFeatCode_lexicalSense, v);}    
  }

    