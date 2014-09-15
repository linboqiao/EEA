

/* First created by JCasGen Sat Sep 13 18:23:45 EDT 2014 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;



/** Annotate numbers and their normalized form
 * Updated by JCasGen Sun Sep 14 23:46:31 EDT 2014
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/TypeSystem.xml
 * @generated */
public class NumberAnnotation extends ComponentAnnotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(NumberAnnotation.class);
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
  protected NumberAnnotation() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public NumberAnnotation(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public NumberAnnotation(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public NumberAnnotation(JCas jcas, int begin, int end) {
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
  //* Feature: normalizedString

  /** getter for normalizedString - gets The normalized result
   * @generated
   * @return value of the feature 
   */
  public String getNormalizedString() {
    if (NumberAnnotation_Type.featOkTst && ((NumberAnnotation_Type)jcasType).casFeat_normalizedString == null)
      jcasType.jcas.throwFeatMissing("normalizedString", "edu.cmu.cs.lti.script.type.NumberAnnotation");
    return jcasType.ll_cas.ll_getStringValue(addr, ((NumberAnnotation_Type)jcasType).casFeatCode_normalizedString);}
    
  /** setter for normalizedString - sets The normalized result 
   * @generated
   * @param v value to set into the feature 
   */
  public void setNormalizedString(String v) {
    if (NumberAnnotation_Type.featOkTst && ((NumberAnnotation_Type)jcasType).casFeat_normalizedString == null)
      jcasType.jcas.throwFeatMissing("normalizedString", "edu.cmu.cs.lti.script.type.NumberAnnotation");
    jcasType.ll_cas.ll_setStringValue(addr, ((NumberAnnotation_Type)jcasType).casFeatCode_normalizedString, v);}    
  }

    