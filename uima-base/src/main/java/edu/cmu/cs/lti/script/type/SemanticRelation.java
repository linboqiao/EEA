

/* First created by JCasGen Sun Sep 14 18:06:22 EDT 2014 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;



/** Generic Semantic Relation
 * Updated by JCasGen Sun Sep 14 23:46:31 EDT 2014
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/TypeSystem.xml
 * @generated */
public class SemanticRelation extends ComponentTOP {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(SemanticRelation.class);
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
  protected SemanticRelation() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public SemanticRelation(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public SemanticRelation(JCas jcas) {
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
  //* Feature: semanticAnnotation

  /** getter for semanticAnnotation - gets 
   * @generated
   * @return value of the feature 
   */
  public String getSemanticAnnotation() {
    if (SemanticRelation_Type.featOkTst && ((SemanticRelation_Type)jcasType).casFeat_semanticAnnotation == null)
      jcasType.jcas.throwFeatMissing("semanticAnnotation", "edu.cmu.cs.lti.script.type.SemanticRelation");
    return jcasType.ll_cas.ll_getStringValue(addr, ((SemanticRelation_Type)jcasType).casFeatCode_semanticAnnotation);}
    
  /** setter for semanticAnnotation - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setSemanticAnnotation(String v) {
    if (SemanticRelation_Type.featOkTst && ((SemanticRelation_Type)jcasType).casFeat_semanticAnnotation == null)
      jcasType.jcas.throwFeatMissing("semanticAnnotation", "edu.cmu.cs.lti.script.type.SemanticRelation");
    jcasType.ll_cas.ll_setStringValue(addr, ((SemanticRelation_Type)jcasType).casFeatCode_semanticAnnotation, v);}    
   
    
  //*--------------*
  //* Feature: head

  /** getter for head - gets 
   * @generated
   * @return value of the feature 
   */
  public Word getHead() {
    if (SemanticRelation_Type.featOkTst && ((SemanticRelation_Type)jcasType).casFeat_head == null)
      jcasType.jcas.throwFeatMissing("head", "edu.cmu.cs.lti.script.type.SemanticRelation");
    return (Word)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((SemanticRelation_Type)jcasType).casFeatCode_head)));}
    
  /** setter for head - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setHead(Word v) {
    if (SemanticRelation_Type.featOkTst && ((SemanticRelation_Type)jcasType).casFeat_head == null)
      jcasType.jcas.throwFeatMissing("head", "edu.cmu.cs.lti.script.type.SemanticRelation");
    jcasType.ll_cas.ll_setRefValue(addr, ((SemanticRelation_Type)jcasType).casFeatCode_head, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: child

  /** getter for child - gets 
   * @generated
   * @return value of the feature 
   */
  public Word getChild() {
    if (SemanticRelation_Type.featOkTst && ((SemanticRelation_Type)jcasType).casFeat_child == null)
      jcasType.jcas.throwFeatMissing("child", "edu.cmu.cs.lti.script.type.SemanticRelation");
    return (Word)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((SemanticRelation_Type)jcasType).casFeatCode_child)));}
    
  /** setter for child - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setChild(Word v) {
    if (SemanticRelation_Type.featOkTst && ((SemanticRelation_Type)jcasType).casFeat_child == null)
      jcasType.jcas.throwFeatMissing("child", "edu.cmu.cs.lti.script.type.SemanticRelation");
    jcasType.ll_cas.ll_setRefValue(addr, ((SemanticRelation_Type)jcasType).casFeatCode_child, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    