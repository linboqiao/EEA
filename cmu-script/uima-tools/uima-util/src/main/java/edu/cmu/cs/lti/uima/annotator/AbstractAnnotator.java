package edu.cmu.cs.lti.uima.annotator;

import edu.cmu.cs.lti.model.UimaConst;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/15/15
 * Time: 5:11 PM
 *
 * @author Zhengzhong Liu
 */
public abstract class AbstractAnnotator extends JCasAnnotator_ImplBase {
    public final static String PARAM_GOLD_STANDARD_VIEW_NAME = "goldStandard";

    public final static String PARAM_ENCODING = "encoding";

    public static final String DEFAULT_ENCODING = "UTF-8";

    @ConfigurationParameter(description = "The view name for the golden standard view", name =
            PARAM_GOLD_STANDARD_VIEW_NAME, defaultValue = UimaConst.goldViewName)
    protected String goldStandardViewName;

    @ConfigurationParameter(description = "Specify the encoding of the input", name = PARAM_ENCODING, defaultValue =
            DEFAULT_ENCODING)
    protected String encoding;

    public final String COMPONENT_ID = this.getClass().getSimpleName();
}
