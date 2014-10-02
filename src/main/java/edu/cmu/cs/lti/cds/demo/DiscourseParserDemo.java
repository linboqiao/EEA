/**
 *
 */
package edu.cmu.cs.lti.cds.demo;

import edu.arizona.sista.discourse.rstparser.DiscourseTree;
import edu.arizona.sista.processors.Document;
import edu.arizona.sista.processors.Processor;
import edu.arizona.sista.processors.corenlp.CoreNLPProcessor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

/**
 * @author zhengzhongliu
 */
public class DiscourseParserDemo {
    /**
     * @param args
     */
    public static void main(String[] args) throws FileNotFoundException {
        Processor proc = new CoreNLPProcessor(true, false, true);
        File out = new File("data/discourse_out_original");
        PrintWriter writer = new PrintWriter(new FileOutputStream(out));


        Document doc = proc
                .annotate("With the nation 's attention riveted again on a Los Angeles courtroom , a knife dealer testified that O.J. Simpson bought a 15-inch knife five weeks before the slashing deaths of his ex-wife and her friend .\n" +
                        "The German-made Stilleto knife Simpson purchased May 3 had a 6-inch stainless steel blade and a handle made of deer antlers with brass trim , said Allen Wattenberg , owner of Ross Cutlery in downtown Los Angeles .\n" +
                        "Simpson asked that the knife be sharpened and Wattenberg complied , he said .\n" +
                        "Wattenberg was the first witness to testify Thursday in a hearing to determine if prosecutors have sufficient evidence to bring the former National Football League great to trial for murder in the June 12 slayings of Nicole Brown Simpson and Ronald Goldman .\n" +
                        "The disclosure -- and new details about evidence in the case -- was among several surprises in a hearing broadcast on television live .\n" +
                        "The preliminary hearing in the downtown Criminal Courts Building continues today .\n" +
                        "Earlier Thursday , prosecutor Marcia Clark and Simpson 's attorney , Robert L. Shapiro , sparred on evidence , and agreed to proceed next Tuesday on a defense motion that complains that evidence pulled from Simpson 's estate was illegally obtained .\n" +
                        "A motion filed Wednesday by the defense contends that a detective who scaled the wall of Simpson 's estate June 13 violated Simpson 's privacy , compromising a subsequent search and tainting evidence .\n" +
                        "Jailed without bail since June 17 in the slayings of his ex-wife and her friend , Simpson appeared in court Thursday looking more comfortable and rested than he has at any of his four prior court appearances .\n" +
                        "He wore a navy blue suit , white shirt and a burgundy tie with white polka dots .\n" +
                        "As he was escorted into the courtroom , Simpson smiled and put an arm around his attorney .\n" +
                        "The families of both victims monitored proceedings from the courtroom .\n" +
                        "Members of the Brown family wore an angel pin that was a replica of one frequently worn by Nicole Brown Simpson .\n" +
                        "Farther down on the same bench , Ronald Goldman 's father , Fred , and stepmother , Patti , sat grim-faced , often gripping each other 's hands or shoulders for support .\n" +
                        "Three witnesses testified before the hearing was recessed at 4:30 p.m. , including the co-owner and a store clerk at Ross Cutlery , where Simpson was said to have purchased a knife while filming a TV pilot outside .\n" +
                        "`` I guess he was attracted to the knife , '' said store clerk Jose Camacho , who testified after Wattenberg .\n" +
                        "`` It 's a nice-looking knife .\n" +
                        "And he just -- he just liked it . ''\n" +
                        "The testimony of Camacho was made more striking when he conceded he was paid $ 12,500 by the tabloid The National Enquirer for his story about selling the folding knife .\n" +
                        "Wattenberg testified that the payment would be split between Camacho , him and his brother , Richard Wattenberg , a co-owner of the store .\n" +
                        "Asked by Shapiro whether he had been told by prosecutors not to talk to anybody about the Simpson case , Camacho -- who testified before the Los Angeles County grand jury -- replied that he had been given conflicting orders .\n" +
                        "He explained that while prosecutors had told him not to talk , `` this lady , Patti -- she works for the district attorney -- said that we could if we wanted to .\n" +
                        "`` She told us that people talk to the press , '' Camacho said .\n" +
                        "`` Are you a little uncertain about this , Mr. Camacho ? '' asked Deputy District Attorney William Hodgman .\n" +
                        "`` Yes , '' the witness replied .\n" +
                        "The grand jury hearing evidence in the case against Simpson was disbanded last week by a judge who said he believed that some jurors had been tainted by pre-trial publicity in the case .\n" +
                        "Camacho said he was under a lot of pressure to talk when the media descended on the store days after the killings .\n" +
                        "`` I was lying because I was told to lie , '' he said .\n" +
                        "When he learned the Enquirer was paying someone else money for less information than he had , Camacho said he decided to talk .\n" +
                        "`` I wanted to stop lying , '' he said , adding that he found the media to be `` tricky people . ''\n" +
                        "Camacho also testified that the television tabloid , `` Hard Copy , '' had offered to pay him for his story , but it was only `` some peanuts . ''\n" +
                        "Camacho testified that he sold Simpson the knife for $ 74.98 , which Simpson paid for with a $ 100 bill , although the store kept no written record of the sale .\n" +
                        "He also said Simpson asked to have the knife 's 6-inch blade sharpened .\n" +
                        "A third witness Thursday , John DeBello , general manager of Mezzaluna , testified that Nicole Simpson dined at his restaurant in the Brentwood area of Los Angeles that night with friends and family .\n" +
                        "`` She frequented the restaurant quite often , '' DeBello said .\n" +
                        "-LRB- STORY CAN END HERE .\n" +
                        "OPTIONAL 2ND TAKE FOLLOWS . -RRB-");
        DiscourseTree dt = doc.discourseTree().get();
        writer.println(dt);
        writer.close();
    }
}