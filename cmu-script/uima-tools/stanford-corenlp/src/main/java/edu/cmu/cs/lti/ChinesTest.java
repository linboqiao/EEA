package edu.cmu.cs.lti;


import edu.stanford.nlp.hcoref.CorefCoreAnnotations;
import edu.stanford.nlp.hcoref.data.CorefChain;
import edu.stanford.nlp.hcoref.data.Mention;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.StringUtils;

import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * Date: 1/26/16
 * Time: 3:07 PM
 *
 * @author Zhengzhong Liu
 */
public class ChinesTest {
    public static void main(String[] args) throws Exception {
        long startTime = System.currentTimeMillis();

        String text =
                "我于2010年4月4日，通过北京乐居行中介公司在海淀菊园小区租了一间主卧带阳台，月租金：1050，期限：一年。当时，中介让我们交600元订金，我们如数交给了中介。于2010年4月6" +
                "日来中介公司签合同，中介公司要求订金交全，还能看合同，我们交齐后，中介又要求我们交730" +
                "卫生费，在交订金钱，中介称无其他费用，我们觉得有被欺骗的感觉，要求返回订金，中介称愿意签就签，不签拉到，订金一分不退，愿意起诉就起诉。\n" +
                "\n" +
                "在网上看了下，原来有好多受害者都掉进了这家中介的陷阱，没办法就这样哑巴吃黄连，房价太高，买不起，租房却掉进陷阱，现在在外租房人太多了，有谁能帮帮我们，为什么这样公司还能继续开，如今我们已经向法院提交了起诉书，打官司。不是为了钱，只是不希望再看到受害者掉进该中介陷阱。希望通过法律手段，维护自己的合法权益。且告知其他租房人，在租房时，一定要调查好，小心掉进中介陷阱\n" +
                "我当时租他们的房子也是这样，当初说什么包物业费，结果到后来去他们公司签合同才发现原来还要交卫生费，房子不久就要到期了，现在很担心到时候押金退不回来。\n" +
                "我今年4月租住菊园小区的房子，准备住到十一不住了，现在只住了5个月，北京乐居行居然说不交十月份到十二月份的房租不给住，我已经提前一个月通知他们不住了，简直可恶。\n" +
                "北京乐居行房屋中介简直就是一个地痞流氓公司，工商的居然都不管，还有没有天理啊？\n" +
                "吃了亏上了当也没办法，我们也是受害人。不知道将来退房时会是什么样子，受了气没关系，我们可以忍，善恶自有报。\n" +
                "乐居行是一个骗子公司，这个公司的人都没有良心，全是骗子，希望受过这个公司骗的人都去投诉一下，不管有没有用，相信多行不义必自毙，总有一天他们会受到惩罚的！\n";

        args = new String[]{"-props", "edu/stanford/nlp/hcoref/properties/zh-coref-default.properties"};

        Annotation document = new Annotation(text);
        Properties props = StringUtils.argsToProperties(args);
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        pipeline.annotate(document);
        System.out.println("---");
        System.out.println("coref chains");

        for (CorefChain cc : document.get(CorefCoreAnnotations.CorefChainAnnotation.class).values()) {
            System.out.println("\t" + cc);
        }
        for (CoreMap sentence : document.get(CoreAnnotations.SentencesAnnotation.class)) {
            System.out.println("---");
            System.out.println("mentions");
            for (Mention m : sentence.get(CorefCoreAnnotations.CorefMentionsAnnotation.class)) {
                System.out.println("\t" + m);
            }
        }

        long endTime = System.currentTimeMillis();
        long time = (endTime - startTime) / 1000;
        System.out.println("Running time " + time / 60 + "min " + time % 60 + "s");
    }
}
