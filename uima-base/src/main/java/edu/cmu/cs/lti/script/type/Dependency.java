

/* First created by JCasGen Mon Sep 15 15:04:26 EDT 2014 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;



/** A generic dependency type, specific annotator dependency should inherite this. In principle, dependency nodes can be different as words, that's why it was annotated individually.
 * Updated by JCasGen Tue Sep 16 00:38:29 EDT 2014
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/TypeSystem.xml
 * @generated */
public class Dependency extends ComponentTOP {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Dependency.class);
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
  protected Dependency() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public Dependency(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public Dependency(JCas jcas) {
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
  //* Feature: head

  /** getter for head - gets 
   * @generated
   * @return value of the feature 
   */
  public Word getHead() {
    if (Dependency_Type.featOkTst && ((Dependency_Type)jcasType).casFeat_head == null)
      jcasType.jcas.throwFeatMissing("head", "edu.cmu.cs.lti.script.type.Dependency");
    return (Word)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Dependency_Type)jcasType).casFeatCode_head)));}
    
  /** setter for head - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setHead(Word v) {
    if (Dependency_Type.featOkTst && ((Dependency_Type)jcasType).casFeat_head == null)
      jcasType.jcas.throwFeatMissing("head", "edu.cmu.cs.lti.script.type.Dependency");
    jcasType.ll_cas.ll_setRefValue(addr, ((Dependency_Type)jcasType).casFeatCode_head, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: child

  /** getter for child - gets 
   * @generated
   * @return value of the feature 
   */
  public Word getChild() {
    if (Dependency_Type.featOkTst && ((Dependency_Type)jcasType).casFeat_child == null)
      jcasType.jcas.throwFeatMissing("child", "edu.cmu.cs.lti.script.type.Dependency");
    return (Word)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Dependency_Type)jcasType).casFeatCode_child)));}
    
  /** setter for child - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setChild(Word v) {
    if (Dependency_Type.featOkTst && ((Dependency_Type)jcasType).casFeat_child == null)
      jcasType.jcas.throwFeatMissing("child", "edu.cmu.cs.lti.script.type.Dependency");
    jcasType.ll_cas.ll_setRefValue(addr, ((Dependency_Type)jcasType).casFeatCode_child, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: dependencyType

  /** getter for dependencyType - gets 
   * @generated
   * @return value of the feature 
   */
  public String getDependencyType() {
    if (Dependency_Type.featOkTst && ((Dependency_Type)jcasType).casFeat_dependencyType == null)
      jcasType.jcas.throwFeatMissing("dependencyType", "edu.cmu.cs.lti.script.type.Dependency");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Dependency_Type)jcasType).casFeatCode_dependencyType);}
    
  /** setter for dependencyType - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setDependencyType(String v) {
    if (Dependency_Type.featOkTst && ((Dependency_Type)jcasType).casFeat_dependencyType == null)
      jcasType.jcas.throwFeatMissing("dependencyType", "edu.cmu.cs.lti.script.type.Dependency");
    jcasType.ll_cas.ll_setStringValue(addr, ((Dependency_Type)jcasType).casFeatCode_dependencyType, v);}    
   
    
  //*--------------*
  //* Feature: weight

  /** getter for weight - gets 
   * @generated
   * @return value of the feature 
   */
  public double getWeight() {
    if (Dependency_Type.featOkTst && ((Dependency_Type)jcasType).casFeat_weight == null)
      jcasType.jcas.throwFeatMissing("weight", "edu.cmu.cs.lti.script.type.Dependency");
    return jcasType.ll_cas.ll_getDoubleValue(addr, ((Dependency_Type)jcasType).casFeatCode_weight);}
    
  /** setter for weight - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setWeight(double v) {
    if (Dependency_Type.featOkTst && ((Dependency_Type)jcasType).casFeat_weight == null)
      jcasType.jcas.throwFeatMissing("weight", "edu.cmu.cs.lti.script.type.Dependency");
    jcasType.ll_cas.ll_setDoubleValue(addr, ((Dependency_Type)jcasType).casFeatCode_weight, v);}    
  }

    