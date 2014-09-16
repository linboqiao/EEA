

/* First created by JCasGen Mon Sep 15 15:04:26 EDT 2014 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;



/** 
 * Updated by JCasGen Tue Sep 16 00:38:29 EDT 2014
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/TypeSystem.xml
 * @generated */
public class OpennlpChunk extends ComponentAnnotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(OpennlpChunk.class);
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
  protected OpennlpChunk() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public OpennlpChunk(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public OpennlpChunk(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public OpennlpChunk(JCas jcas, int begin, int end) {
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
  //* Feature: tag

  /** getter for tag - gets 
   * @generated
   * @return value of the feature 
   */
  public String getTag() {
    if (OpennlpChunk_Type.featOkTst && ((OpennlpChunk_Type)jcasType).casFeat_tag == null)
      jcasType.jcas.throwFeatMissing("tag", "edu.cmu.cs.lti.script.type.OpennlpChunk");
    return jcasType.ll_cas.ll_getStringValue(addr, ((OpennlpChunk_Type)jcasType).casFeatCode_tag);}
    
  /** setter for tag - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setTag(String v) {
    if (OpennlpChunk_Type.featOkTst && ((OpennlpChunk_Type)jcasType).casFeat_tag == null)
      jcasType.jcas.throwFeatMissing("tag", "edu.cmu.cs.lti.script.type.OpennlpChunk");
    jcasType.ll_cas.ll_setStringValue(addr, ((OpennlpChunk_Type)jcasType).casFeatCode_tag, v);}    
  }

    