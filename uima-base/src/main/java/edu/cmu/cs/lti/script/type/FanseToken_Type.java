
/* First created by JCasGen Mon Sep 15 15:04:26 EDT 2014 */
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
 * Updated by JCasGen Tue Sep 16 00:38:29 EDT 2014
 * @generated */
public class FanseToken_Type extends Word_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (FanseToken_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = FanseToken_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new FanseToken(addr, FanseToken_Type.this);
  			   FanseToken_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new FanseToken(addr, FanseToken_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = FanseToken.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.cs.lti.script.type.FanseToken");
 
  /** @generated */
  final Feature casFeat_coarsePos;
  /** @generated */
  final int     casFeatCode_coarsePos;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getCoarsePos(int addr) {
        if (featOkTst && casFeat_coarsePos == null)
      jcas.throwFeatMissing("coarsePos", "edu.cmu.cs.lti.script.type.FanseToken");
    return ll_cas.ll_getStringValue(addr, casFeatCode_coarsePos);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setCoarsePos(int addr, String v) {
        if (featOkTst && casFeat_coarsePos == null)
      jcas.throwFeatMissing("coarsePos", "edu.cmu.cs.lti.script.type.FanseToken");
    ll_cas.ll_setStringValue(addr, casFeatCode_coarsePos, v);}
    
  
 
  /** @generated */
  final Feature casFeat_pos;
  /** @generated */
  final int     casFeatCode_pos;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getPos(int addr) {
        if (featOkTst && casFeat_pos == null)
      jcas.throwFeatMissing("pos", "edu.cmu.cs.lti.script.type.FanseToken");
    return ll_cas.ll_getStringValue(addr, casFeatCode_pos);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setPos(int addr, String v) {
        if (featOkTst && casFeat_pos == null)
      jcas.throwFeatMissing("pos", "edu.cmu.cs.lti.script.type.FanseToken");
    ll_cas.ll_setStringValue(addr, casFeatCode_pos, v);}
    
  
 
  /** @generated */
  final Feature casFeat_lexicalSense;
  /** @generated */
  final int     casFeatCode_lexicalSense;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getLexicalSense(int addr) {
        if (featOkTst && casFeat_lexicalSense == null)
      jcas.throwFeatMissing("lexicalSense", "edu.cmu.cs.lti.script.type.FanseToken");
    return ll_cas.ll_getStringValue(addr, casFeatCode_lexicalSense);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setLexicalSense(int addr, String v) {
        if (featOkTst && casFeat_lexicalSense == null)
      jcas.throwFeatMissing("lexicalSense", "edu.cmu.cs.lti.script.type.FanseToken");
    ll_cas.ll_setStringValue(addr, casFeatCode_lexicalSense, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public FanseToken_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_coarsePos = jcas.getRequiredFeatureDE(casType, "coarsePos", "uima.cas.String", featOkTst);
    casFeatCode_coarsePos  = (null == casFeat_coarsePos) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_coarsePos).getCode();

 
    casFeat_pos = jcas.getRequiredFeatureDE(casType, "pos", "uima.cas.String", featOkTst);
    casFeatCode_pos  = (null == casFeat_pos) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_pos).getCode();

 
    casFeat_lexicalSense = jcas.getRequiredFeatureDE(casType, "lexicalSense", "uima.cas.String", featOkTst);
    casFeatCode_lexicalSense  = (null == casFeat_lexicalSense) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_lexicalSense).getCode();

  }
}



    