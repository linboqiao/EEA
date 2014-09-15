

/* First created by JCasGen Sat Sep 13 18:23:45 EDT 2014 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;



/** 
 * Updated by JCasGen Sun Sep 14 23:46:31 EDT 2014
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/TypeSystem.xml
 * @generated */
public class Sentence extends ComponentAnnotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Sentence.class);
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
  protected Sentence() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public Sentence(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public Sentence(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public Sentence(JCas jcas, int begin, int end) {
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
  //* Feature: paragraphId

  /** getter for paragraphId - gets 
   * @generated
   * @return value of the feature 
   */
  public String getParagraphId() {
    if (Sentence_Type.featOkTst && ((Sentence_Type)jcasType).casFeat_paragraphId == null)
      jcasType.jcas.throwFeatMissing("paragraphId", "edu.cmu.cs.lti.script.type.Sentence");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Sentence_Type)jcasType).casFeatCode_paragraphId);}
    
  /** setter for paragraphId - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setParagraphId(String v) {
    if (Sentence_Type.featOkTst && ((Sentence_Type)jcasType).casFeat_paragraphId == null)
      jcasType.jcas.throwFeatMissing("paragraphId", "edu.cmu.cs.lti.script.type.Sentence");
    jcasType.ll_cas.ll_setStringValue(addr, ((Sentence_Type)jcasType).casFeatCode_paragraphId, v);}    
   
    
  //*--------------*
  //* Feature: alignmentScore

  /** getter for alignmentScore - gets 
   * @generated
   * @return value of the feature 
   */
  public double getAlignmentScore() {
    if (Sentence_Type.featOkTst && ((Sentence_Type)jcasType).casFeat_alignmentScore == null)
      jcasType.jcas.throwFeatMissing("alignmentScore", "edu.cmu.cs.lti.script.type.Sentence");
    return jcasType.ll_cas.ll_getDoubleValue(addr, ((Sentence_Type)jcasType).casFeatCode_alignmentScore);}
    
  /** setter for alignmentScore - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAlignmentScore(double v) {
    if (Sentence_Type.featOkTst && ((Sentence_Type)jcasType).casFeat_alignmentScore == null)
      jcasType.jcas.throwFeatMissing("alignmentScore", "edu.cmu.cs.lti.script.type.Sentence");
    jcasType.ll_cas.ll_setDoubleValue(addr, ((Sentence_Type)jcasType).casFeatCode_alignmentScore, v);}    
  }

    