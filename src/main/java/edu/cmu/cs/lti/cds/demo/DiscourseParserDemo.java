/**
 *
 */
package edu.cmu.cs.lti.cds.demo;

import edu.arizona.sista.discourse.rstparser.DiscourseTree;
import edu.arizona.sista.processors.Document;
import edu.arizona.sista.processors.Processor;
import edu.arizona.sista.processors.corenlp.CoreNLPProcessor;

/**
 * @author zhengzhongliu
 */
public class DiscourseParserDemo {
    /**
     * @param args
     */
    public static void main(String[] args) {
        Processor proc = new CoreNLPProcessor(true, false, true);
        Document doc = proc
                .annotate("John Smith went to China, he visited Beijing, on January 10th, 2013.");
        DiscourseTree dt = doc.discourseTree().get();
        System.out.println(dt);
    }
}