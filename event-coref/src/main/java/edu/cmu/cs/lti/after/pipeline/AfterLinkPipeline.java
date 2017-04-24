package edu.cmu.cs.lti.after.pipeline;

import edu.cmu.cs.lti.event_coref.pipeline.EventMentionPipeline;
import edu.cmu.cs.lti.utils.Configuration;

/**
 * Created with IntelliJ IDEA.
 * Date: 12/3/16
 * Time: 2:54 PM
 *
 * @author Zhengzhong Liu
 */
public class AfterLinkPipeline {
    public static void main(String[] argv) throws Exception {
        Configuration commonConfig = new Configuration("settings/common.properties");
        String typeSystemName = commonConfig.get("edu.cmu.cs.lti.event.typesystem");

        Configuration config = new Configuration(argv[0]);

        // Now prepare the real pipeline.
        EventMentionPipeline pipeline = new EventMentionPipeline(typeSystemName, config);

        pipeline.prepareData(config);

        if (config.getBoolean("edu.cmu.cs.lti.development", false)) {
            pipeline.crossValidation(config);
        }

        if (config.getBoolean("edu.cmu.cs.lti.test", false)) {
            pipeline.trainTest(config, false, config.getBoolean("edu.cmu.cs.lti.test.has_gold", false));
        }
    }
}
