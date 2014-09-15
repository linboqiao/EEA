
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

/** Cluster of entity mentions
 * Updated by JCasGen Sun Sep 14 21:48:57 EDT 2014
 * @generated */
public class EntityCoreferenceCluster_Type extends ComponentTOP_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (EntityCoreferenceCluster_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = EntityCoreferenceCluster_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new EntityCoreferenceCluster(addr, EntityCoreferenceCluster_Type.this);
  			   EntityCoreferenceCluster_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new EntityCoreferenceCluster(addr, EntityCoreferenceCluster_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = EntityCoreferenceCluster.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.cs.lti.script.type.EntityCoreferenceCluster");
 
  /** @generated */
  final Feature casFeat_entityMentions;
  /** @generated */
  final int     casFeatCode_entityMentions;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getEntityMentions(int addr) {
        if (featOkTst && casFeat_entityMentions == null)
      jcas.throwFeatMissing("entityMentions", "edu.cmu.cs.lti.script.type.EntityCoreferenceCluster");
    return ll_cas.ll_getRefValue(addr, casFeatCode_entityMentions);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setEntityMentions(int addr, int v) {
        if (featOkTst && casFeat_entityMentions == null)
      jcas.throwFeatMissing("entityMentions", "edu.cmu.cs.lti.script.type.EntityCoreferenceCluster");
    ll_cas.ll_setRefValue(addr, casFeatCode_entityMentions, v);}
    
  
 
  /** @generated */
  final Feature casFeat_representativeMention;
  /** @generated */
  final int     casFeatCode_representativeMention;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getRepresentativeMention(int addr) {
        if (featOkTst && casFeat_representativeMention == null)
      jcas.throwFeatMissing("representativeMention", "edu.cmu.cs.lti.script.type.EntityCoreferenceCluster");
    return ll_cas.ll_getRefValue(addr, casFeatCode_representativeMention);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setRepresentativeMention(int addr, int v) {
        if (featOkTst && casFeat_representativeMention == null)
      jcas.throwFeatMissing("representativeMention", "edu.cmu.cs.lti.script.type.EntityCoreferenceCluster");
    ll_cas.ll_setRefValue(addr, casFeatCode_representativeMention, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public EntityCoreferenceCluster_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_entityMentions = jcas.getRequiredFeatureDE(casType, "entityMentions", "uima.cas.FSList", featOkTst);
    casFeatCode_entityMentions  = (null == casFeat_entityMentions) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_entityMentions).getCode();

 
    casFeat_representativeMention = jcas.getRequiredFeatureDE(casType, "representativeMention", "edu.cmu.cs.lti.script.type.EntityMention", featOkTst);
    casFeatCode_representativeMention  = (null == casFeat_representativeMention) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_representativeMention).getCode();

  }
}



    