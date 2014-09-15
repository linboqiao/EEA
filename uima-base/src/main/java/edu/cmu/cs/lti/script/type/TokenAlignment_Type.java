
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

/** 
 * Updated by JCasGen Sun Sep 14 23:46:31 EDT 2014
 * @generated */
public class TokenAlignment_Type extends ComponentTOP_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (TokenAlignment_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = TokenAlignment_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new TokenAlignment(addr, TokenAlignment_Type.this);
  			   TokenAlignment_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new TokenAlignment(addr, TokenAlignment_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = TokenAlignment.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.cs.lti.script.type.TokenAlignment");
 
  /** @generated */
  final Feature casFeat_targetViewName;
  /** @generated */
  final int     casFeatCode_targetViewName;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getTargetViewName(int addr) {
        if (featOkTst && casFeat_targetViewName == null)
      jcas.throwFeatMissing("targetViewName", "edu.cmu.cs.lti.script.type.TokenAlignment");
    return ll_cas.ll_getStringValue(addr, casFeatCode_targetViewName);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setTargetViewName(int addr, String v) {
        if (featOkTst && casFeat_targetViewName == null)
      jcas.throwFeatMissing("targetViewName", "edu.cmu.cs.lti.script.type.TokenAlignment");
    ll_cas.ll_setStringValue(addr, casFeatCode_targetViewName, v);}
    
  
 
  /** @generated */
  final Feature casFeat_sourceToken;
  /** @generated */
  final int     casFeatCode_sourceToken;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getSourceToken(int addr) {
        if (featOkTst && casFeat_sourceToken == null)
      jcas.throwFeatMissing("sourceToken", "edu.cmu.cs.lti.script.type.TokenAlignment");
    return ll_cas.ll_getRefValue(addr, casFeatCode_sourceToken);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setSourceToken(int addr, int v) {
        if (featOkTst && casFeat_sourceToken == null)
      jcas.throwFeatMissing("sourceToken", "edu.cmu.cs.lti.script.type.TokenAlignment");
    ll_cas.ll_setRefValue(addr, casFeatCode_sourceToken, v);}
    
  
 
  /** @generated */
  final Feature casFeat_targetToken;
  /** @generated */
  final int     casFeatCode_targetToken;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getTargetToken(int addr) {
        if (featOkTst && casFeat_targetToken == null)
      jcas.throwFeatMissing("targetToken", "edu.cmu.cs.lti.script.type.TokenAlignment");
    return ll_cas.ll_getRefValue(addr, casFeatCode_targetToken);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setTargetToken(int addr, int v) {
        if (featOkTst && casFeat_targetToken == null)
      jcas.throwFeatMissing("targetToken", "edu.cmu.cs.lti.script.type.TokenAlignment");
    ll_cas.ll_setRefValue(addr, casFeatCode_targetToken, v);}
    
  
 
  /** @generated */
  final Feature casFeat_sourceViewName;
  /** @generated */
  final int     casFeatCode_sourceViewName;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getSourceViewName(int addr) {
        if (featOkTst && casFeat_sourceViewName == null)
      jcas.throwFeatMissing("sourceViewName", "edu.cmu.cs.lti.script.type.TokenAlignment");
    return ll_cas.ll_getStringValue(addr, casFeatCode_sourceViewName);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setSourceViewName(int addr, String v) {
        if (featOkTst && casFeat_sourceViewName == null)
      jcas.throwFeatMissing("sourceViewName", "edu.cmu.cs.lti.script.type.TokenAlignment");
    ll_cas.ll_setStringValue(addr, casFeatCode_sourceViewName, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public TokenAlignment_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_targetViewName = jcas.getRequiredFeatureDE(casType, "targetViewName", "uima.cas.String", featOkTst);
    casFeatCode_targetViewName  = (null == casFeat_targetViewName) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_targetViewName).getCode();

 
    casFeat_sourceToken = jcas.getRequiredFeatureDE(casType, "sourceToken", "edu.cmu.cs.lti.script.type.StanfordCorenlpToken", featOkTst);
    casFeatCode_sourceToken  = (null == casFeat_sourceToken) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_sourceToken).getCode();

 
    casFeat_targetToken = jcas.getRequiredFeatureDE(casType, "targetToken", "uima.cas.FSList", featOkTst);
    casFeatCode_targetToken  = (null == casFeat_targetToken) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_targetToken).getCode();

 
    casFeat_sourceViewName = jcas.getRequiredFeatureDE(casType, "sourceViewName", "uima.cas.String", featOkTst);
    casFeatCode_sourceViewName  = (null == casFeat_sourceViewName) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_sourceViewName).getCode();

  }
}



    