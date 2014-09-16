

/* First created by JCasGen Mon Sep 15 15:04:26 EDT 2014 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.FSList;


/** Intend to be use for script based analysis. Still tentative
 * Updated by JCasGen Tue Sep 16 00:38:30 EDT 2014
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/TypeSystem.xml
 * @generated */
public class Script extends ComponentAnnotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Script.class);
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
  protected Script() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public Script(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public Script(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public Script(JCas jcas, int begin, int end) {
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
  //* Feature: goal

  /** getter for goal - gets 
   * @generated
   * @return value of the feature 
   */
  public Event getGoal() {
    if (Script_Type.featOkTst && ((Script_Type)jcasType).casFeat_goal == null)
      jcasType.jcas.throwFeatMissing("goal", "edu.cmu.cs.lti.script.type.Script");
    return (Event)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Script_Type)jcasType).casFeatCode_goal)));}
    
  /** setter for goal - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setGoal(Event v) {
    if (Script_Type.featOkTst && ((Script_Type)jcasType).casFeat_goal == null)
      jcasType.jcas.throwFeatMissing("goal", "edu.cmu.cs.lti.script.type.Script");
    jcasType.ll_cas.ll_setRefValue(addr, ((Script_Type)jcasType).casFeatCode_goal, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: subevents

  /** getter for subevents - gets List of subevents in the script
   * @generated
   * @return value of the feature 
   */
  public FSList getSubevents() {
    if (Script_Type.featOkTst && ((Script_Type)jcasType).casFeat_subevents == null)
      jcasType.jcas.throwFeatMissing("subevents", "edu.cmu.cs.lti.script.type.Script");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Script_Type)jcasType).casFeatCode_subevents)));}
    
  /** setter for subevents - sets List of subevents in the script 
   * @generated
   * @param v value to set into the feature 
   */
  public void setSubevents(FSList v) {
    if (Script_Type.featOkTst && ((Script_Type)jcasType).casFeat_subevents == null)
      jcasType.jcas.throwFeatMissing("subevents", "edu.cmu.cs.lti.script.type.Script");
    jcasType.ll_cas.ll_setRefValue(addr, ((Script_Type)jcasType).casFeatCode_subevents, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    