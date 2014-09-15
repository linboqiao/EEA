

/* First created by JCasGen Sat Sep 13 18:23:45 EDT 2014 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.Annotation;


/** 
 * Updated by JCasGen Sun Sep 14 23:46:31 EDT 2014
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/TypeSystem.xml
 * @generated */
public class ComponentAnnotation extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(ComponentAnnotation.class);
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
  protected ComponentAnnotation() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public ComponentAnnotation(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public ComponentAnnotation(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public ComponentAnnotation(JCas jcas, int begin, int end) {
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
  //* Feature: componentId

  /** getter for componentId - gets This type can be used for various purposes, including differentiating system annotation from gold standard annotation.
   * @generated
   * @return value of the feature 
   */
  public String getComponentId() {
    if (ComponentAnnotation_Type.featOkTst && ((ComponentAnnotation_Type)jcasType).casFeat_componentId == null)
      jcasType.jcas.throwFeatMissing("componentId", "edu.cmu.cs.lti.script.type.ComponentAnnotation");
    return jcasType.ll_cas.ll_getStringValue(addr, ((ComponentAnnotation_Type)jcasType).casFeatCode_componentId);}
    
  /** setter for componentId - sets This type can be used for various purposes, including differentiating system annotation from gold standard annotation. 
   * @generated
   * @param v value to set into the feature 
   */
  public void setComponentId(String v) {
    if (ComponentAnnotation_Type.featOkTst && ((ComponentAnnotation_Type)jcasType).casFeat_componentId == null)
      jcasType.jcas.throwFeatMissing("componentId", "edu.cmu.cs.lti.script.type.ComponentAnnotation");
    jcasType.ll_cas.ll_setStringValue(addr, ((ComponentAnnotation_Type)jcasType).casFeatCode_componentId, v);}    
   
    
  //*--------------*
  //* Feature: id

  /** getter for id - gets ID assigned to this annotation
   * @generated
   * @return value of the feature 
   */
  public String getId() {
    if (ComponentAnnotation_Type.featOkTst && ((ComponentAnnotation_Type)jcasType).casFeat_id == null)
      jcasType.jcas.throwFeatMissing("id", "edu.cmu.cs.lti.script.type.ComponentAnnotation");
    return jcasType.ll_cas.ll_getStringValue(addr, ((ComponentAnnotation_Type)jcasType).casFeatCode_id);}
    
  /** setter for id - sets ID assigned to this annotation 
   * @generated
   * @param v value to set into the feature 
   */
  public void setId(String v) {
    if (ComponentAnnotation_Type.featOkTst && ((ComponentAnnotation_Type)jcasType).casFeat_id == null)
      jcasType.jcas.throwFeatMissing("id", "edu.cmu.cs.lti.script.type.ComponentAnnotation");
    jcasType.ll_cas.ll_setStringValue(addr, ((ComponentAnnotation_Type)jcasType).casFeatCode_id, v);}    
  }

    