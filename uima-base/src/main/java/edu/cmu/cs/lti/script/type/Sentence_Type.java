
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
 * Updated by JCasGen Tue Sep 16 00:38:30 EDT 2014
 * @generated */
public class Sentence_Type extends ComponentAnnotation_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (Sentence_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = Sentence_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new Sentence(addr, Sentence_Type.this);
  			   Sentence_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new Sentence(addr, Sentence_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = Sentence.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.cs.lti.script.type.Sentence");
 
  /** @generated */
  final Feature casFeat_paragraphId;
  /** @generated */
  final int     casFeatCode_paragraphId;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getParagraphId(int addr) {
        if (featOkTst && casFeat_paragraphId == null)
      jcas.throwFeatMissing("paragraphId", "edu.cmu.cs.lti.script.type.Sentence");
    return ll_cas.ll_getStringValue(addr, casFeatCode_paragraphId);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setParagraphId(int addr, String v) {
        if (featOkTst && casFeat_paragraphId == null)
      jcas.throwFeatMissing("paragraphId", "edu.cmu.cs.lti.script.type.Sentence");
    ll_cas.ll_setStringValue(addr, casFeatCode_paragraphId, v);}
    
  
 
  /** @generated */
  final Feature casFeat_alignmentScore;
  /** @generated */
  final int     casFeatCode_alignmentScore;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public double getAlignmentScore(int addr) {
        if (featOkTst && casFeat_alignmentScore == null)
      jcas.throwFeatMissing("alignmentScore", "edu.cmu.cs.lti.script.type.Sentence");
    return ll_cas.ll_getDoubleValue(addr, casFeatCode_alignmentScore);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setAlignmentScore(int addr, double v) {
        if (featOkTst && casFeat_alignmentScore == null)
      jcas.throwFeatMissing("alignmentScore", "edu.cmu.cs.lti.script.type.Sentence");
    ll_cas.ll_setDoubleValue(addr, casFeatCode_alignmentScore, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public Sentence_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_paragraphId = jcas.getRequiredFeatureDE(casType, "paragraphId", "uima.cas.String", featOkTst);
    casFeatCode_paragraphId  = (null == casFeat_paragraphId) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_paragraphId).getCode();

 
    casFeat_alignmentScore = jcas.getRequiredFeatureDE(casType, "alignmentScore", "uima.cas.Double", featOkTst);
    casFeatCode_alignmentScore  = (null == casFeat_alignmentScore) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_alignmentScore).getCode();

  }
}



    