

/* First created by JCasGen Sat Sep 13 18:23:45 EDT 2014 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.FSList;


/** A generic dependency node , specific annotator dependency nodes should inherite this. In principle, dependency nodes can be different as words, that's why it was annotated individually.
 * Updated by JCasGen Sun Sep 14 17:48:23 EDT 2014
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/TypeSystem.xml
 * @generated */
public class DependencyNode extends ComponentAnnotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(DependencyNode.class);
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
  protected DependencyNode() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public DependencyNode(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public DependencyNode(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public DependencyNode(JCas jcas, int begin, int end) {
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
  //* Feature: headDependencyRelations

  /** getter for headDependencyRelations - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getHeadDependencyRelations() {
    if (DependencyNode_Type.featOkTst && ((DependencyNode_Type)jcasType).casFeat_headDependencyRelations == null)
      jcasType.jcas.throwFeatMissing("headDependencyRelations", "edu.cmu.cs.lti.script.type.DependencyNode");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((DependencyNode_Type)jcasType).casFeatCode_headDependencyRelations)));}
    
  /** setter for headDependencyRelations - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setHeadDependencyRelations(FSList v) {
    if (DependencyNode_Type.featOkTst && ((DependencyNode_Type)jcasType).casFeat_headDependencyRelations == null)
      jcasType.jcas.throwFeatMissing("headDependencyRelations", "edu.cmu.cs.lti.script.type.DependencyNode");
    jcasType.ll_cas.ll_setRefValue(addr, ((DependencyNode_Type)jcasType).casFeatCode_headDependencyRelations, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: childDependencyRelations

  /** getter for childDependencyRelations - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getChildDependencyRelations() {
    if (DependencyNode_Type.featOkTst && ((DependencyNode_Type)jcasType).casFeat_childDependencyRelations == null)
      jcasType.jcas.throwFeatMissing("childDependencyRelations", "edu.cmu.cs.lti.script.type.DependencyNode");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((DependencyNode_Type)jcasType).casFeatCode_childDependencyRelations)));}
    
  /** setter for childDependencyRelations - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setChildDependencyRelations(FSList v) {
    if (DependencyNode_Type.featOkTst && ((DependencyNode_Type)jcasType).casFeat_childDependencyRelations == null)
      jcasType.jcas.throwFeatMissing("childDependencyRelations", "edu.cmu.cs.lti.script.type.DependencyNode");
    jcasType.ll_cas.ll_setRefValue(addr, ((DependencyNode_Type)jcasType).casFeatCode_childDependencyRelations, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: isRoot

  /** getter for isRoot - gets 
   * @generated
   * @return value of the feature 
   */
  public boolean getIsRoot() {
    if (DependencyNode_Type.featOkTst && ((DependencyNode_Type)jcasType).casFeat_isRoot == null)
      jcasType.jcas.throwFeatMissing("isRoot", "edu.cmu.cs.lti.script.type.DependencyNode");
    return jcasType.ll_cas.ll_getBooleanValue(addr, ((DependencyNode_Type)jcasType).casFeatCode_isRoot);}
    
  /** setter for isRoot - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setIsRoot(boolean v) {
    if (DependencyNode_Type.featOkTst && ((DependencyNode_Type)jcasType).casFeat_isRoot == null)
      jcasType.jcas.throwFeatMissing("isRoot", "edu.cmu.cs.lti.script.type.DependencyNode");
    jcasType.ll_cas.ll_setBooleanValue(addr, ((DependencyNode_Type)jcasType).casFeatCode_isRoot, v);}    
  }

    