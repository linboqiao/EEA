
/* First created by JCasGen Sat Sep 13 18:23:45 EDT 2014 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;

/** A generic dependency node , specific annotator dependency nodes should inherite this. In principle, dependency nodes can be different as words, that's why it was annotated individually.
 * Updated by JCasGen Sun Sep 14 17:48:23 EDT 2014
 * @generated */
public class DependencyNode_Type extends ComponentAnnotation_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (DependencyNode_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = DependencyNode_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new DependencyNode(addr, DependencyNode_Type.this);
  			   DependencyNode_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new DependencyNode(addr, DependencyNode_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = DependencyNode.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.cs.lti.script.type.DependencyNode");
 
  /** @generated */
  final Feature casFeat_headDependencyRelations;
  /** @generated */
  final int     casFeatCode_headDependencyRelations;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getHeadDependencyRelations(int addr) {
        if (featOkTst && casFeat_headDependencyRelations == null)
      jcas.throwFeatMissing("headDependencyRelations", "edu.cmu.cs.lti.script.type.DependencyNode");
    return ll_cas.ll_getRefValue(addr, casFeatCode_headDependencyRelations);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setHeadDependencyRelations(int addr, int v) {
        if (featOkTst && casFeat_headDependencyRelations == null)
      jcas.throwFeatMissing("headDependencyRelations", "edu.cmu.cs.lti.script.type.DependencyNode");
    ll_cas.ll_setRefValue(addr, casFeatCode_headDependencyRelations, v);}
    
  
 
  /** @generated */
  final Feature casFeat_childDependencyRelations;
  /** @generated */
  final int     casFeatCode_childDependencyRelations;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getChildDependencyRelations(int addr) {
        if (featOkTst && casFeat_childDependencyRelations == null)
      jcas.throwFeatMissing("childDependencyRelations", "edu.cmu.cs.lti.script.type.DependencyNode");
    return ll_cas.ll_getRefValue(addr, casFeatCode_childDependencyRelations);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setChildDependencyRelations(int addr, int v) {
        if (featOkTst && casFeat_childDependencyRelations == null)
      jcas.throwFeatMissing("childDependencyRelations", "edu.cmu.cs.lti.script.type.DependencyNode");
    ll_cas.ll_setRefValue(addr, casFeatCode_childDependencyRelations, v);}
    
  
 
  /** @generated */
  final Feature casFeat_isRoot;
  /** @generated */
  final int     casFeatCode_isRoot;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public boolean getIsRoot(int addr) {
        if (featOkTst && casFeat_isRoot == null)
      jcas.throwFeatMissing("isRoot", "edu.cmu.cs.lti.script.type.DependencyNode");
    return ll_cas.ll_getBooleanValue(addr, casFeatCode_isRoot);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setIsRoot(int addr, boolean v) {
        if (featOkTst && casFeat_isRoot == null)
      jcas.throwFeatMissing("isRoot", "edu.cmu.cs.lti.script.type.DependencyNode");
    ll_cas.ll_setBooleanValue(addr, casFeatCode_isRoot, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public DependencyNode_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_headDependencyRelations = jcas.getRequiredFeatureDE(casType, "headDependencyRelations", "uima.cas.FSList", featOkTst);
    casFeatCode_headDependencyRelations  = (null == casFeat_headDependencyRelations) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_headDependencyRelations).getCode();

 
    casFeat_childDependencyRelations = jcas.getRequiredFeatureDE(casType, "childDependencyRelations", "uima.cas.FSList", featOkTst);
    casFeatCode_childDependencyRelations  = (null == casFeat_childDependencyRelations) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_childDependencyRelations).getCode();

 
    casFeat_isRoot = jcas.getRequiredFeatureDE(casType, "isRoot", "uima.cas.Boolean", featOkTst);
    casFeatCode_isRoot  = (null == casFeat_isRoot) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_isRoot).getCode();

  }
}



    