
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

/** Intend to be use for script based analysis. Still tentative
 * Updated by JCasGen Sun Sep 14 23:46:31 EDT 2014
 * @generated */
public class Script_Type extends ComponentAnnotation_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (Script_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = Script_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new Script(addr, Script_Type.this);
  			   Script_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new Script(addr, Script_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = Script.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.cs.lti.script.type.Script");
 
  /** @generated */
  final Feature casFeat_goal;
  /** @generated */
  final int     casFeatCode_goal;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getGoal(int addr) {
        if (featOkTst && casFeat_goal == null)
      jcas.throwFeatMissing("goal", "edu.cmu.cs.lti.script.type.Script");
    return ll_cas.ll_getRefValue(addr, casFeatCode_goal);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setGoal(int addr, int v) {
        if (featOkTst && casFeat_goal == null)
      jcas.throwFeatMissing("goal", "edu.cmu.cs.lti.script.type.Script");
    ll_cas.ll_setRefValue(addr, casFeatCode_goal, v);}
    
  
 
  /** @generated */
  final Feature casFeat_subevents;
  /** @generated */
  final int     casFeatCode_subevents;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getSubevents(int addr) {
        if (featOkTst && casFeat_subevents == null)
      jcas.throwFeatMissing("subevents", "edu.cmu.cs.lti.script.type.Script");
    return ll_cas.ll_getRefValue(addr, casFeatCode_subevents);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setSubevents(int addr, int v) {
        if (featOkTst && casFeat_subevents == null)
      jcas.throwFeatMissing("subevents", "edu.cmu.cs.lti.script.type.Script");
    ll_cas.ll_setRefValue(addr, casFeatCode_subevents, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public Script_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_goal = jcas.getRequiredFeatureDE(casType, "goal", "edu.cmu.cs.lti.script.type.Event", featOkTst);
    casFeatCode_goal  = (null == casFeat_goal) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_goal).getCode();

 
    casFeat_subevents = jcas.getRequiredFeatureDE(casType, "subevents", "uima.cas.FSList", featOkTst);
    casFeatCode_subevents  = (null == casFeat_subevents) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_subevents).getCode();

  }
}



    