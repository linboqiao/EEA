/**
 *
 */
package edu.cmu.cs.lti.script.demo;

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
//        Processor proc = new FastNLPProcessor(true, true);

        Processor proc = new CoreNLPProcessor(true, false, true);
        File out = new File("data/discourse_out_original");
        System.out.println(out.getAbsolutePath());
        PrintWriter writer = new PrintWriter(new FileOutputStream(out));

        String demo2 = "Ever since the Trung sisters rallied tribal chieftains and raised an army of female generals to drive out the Chinese in A.D. 40 , women have shared the fighting and dying throughout Vietnam 's 2,000-year struggle for independence . \n" +
                "The heroism of Trung Nhi and her sister , Trung Trac , in one of Vietnam 's most famous rebellions is celebrated here in the capital every spring . \n" +
                "It has become the rallying cry for women who are demanding more equality in a communist country that now marches to the tune of free-market capitalism . \n" +
                "But many see it as an uphill battle in a society that still expects women to display the Confucian ideals of `` cong , '' `` dung , '' `` ngon '' and `` hanh '' -- diligence , beauty , grace and virtue -- while men continue to wield power and accumulate wealth in Southeast Asia 's newest economic dynamo . \n" +
                "And if the weight of Confucian teachings were n't enough to dampen their hopes for greater equality , Vietnamese women outnumber men in a society that values husbands , brothers and sons above wives , sisters and daughters . \n" +
                "Women account for 52 percent of Vietnam 's 72 million citizens , an imbalance blamed on three decades of war with France and the United States . \n" +
                "Still , some women who fought the French as communist Viet Minh soldiers and the United States as Viet Cong guerrillas have made it to top government jobs and are using their power to advance the cause of their generally impoverished sisters . \n" +
                "`` The Trungs left a profound imprint on Vietnamese women , '' says Vice President Nguyen Thi Binh , the country 's highest-ranking woman , during an interview with National Geographic in the presidential palace . \n" +
                "It once housed the French governor-general but was eschewed because of its opulence by Ho Chi Minh , the founder of modern Vietnam . \n" +
                "`` Women have always had an important role in our society because of their contributions to national defense and reconstruction after our wars , '' says Binh , a prim 67-year-old who wears the `` ao dai , '' silk pants and tunic , traditional costume of Vietnamese women . \n" +
                "`` But that role is not yet commensurate with our profound contributions , because of thousands of years of feudalism . '' \n" +
                "Binh was jailed by the French as a revolutionary in 1951 . \n" +
                "But she went on to become foreign minister of the Viet Cong guerrillas , squaring off with former U.S. Secretary of State Henry Kissinger and other American diplomats at the Paris peace talks that eventually led to the withdrawal of U.S. combat troops in 1973 . \n" +
                "She blames Vietnam 's crushing poverty and international isolation since the end of the Indochina war in 1975 for keeping women in menial , low-paying jobs or at home . \n" +
                "The country 's per-capita income remains about $ 250 a year despite a record $ 7 billion in foreign investment in 1993 , while the often malnourished population explodes at an average rate of 2.8 percent a year . \n" +
                "Women make up more than 70 percent of Vietnam 's commercial work force , 67 percent of its teachers , 63 percent of its doctors and 50 percent of its farm laborers . \n" +
                "They hold 29 percent of high-paying management jobs and 73 seats in the 395-member National Assembly -- higher numbers than in the United States . \n" +
                "But percentages can be misleading . \n" +
                "`` In an underdeveloped economy like ours , '' Binh says , `` it is very difficult for women to be both mothers and constructive members of society . \n" +
                "`` Even in this transition period , women are the last hired and first fired . '' \n" +
                "For every venture capitalist like Nguyen Thi Anh Nhan , president of Hanoi 's booming South East Asia Brewery , millions of undereducated young women face a bleak future selling everything from food to fireworks as street vendors or toiling in the country 's vast rice fields . \n" +
                "Even worse , prostitution has returned with a vengeance to Vietnamese cities after a 20-year hiatus . \n" +
                "But Nhan 's brewery has more business than it can handle . \n" +
                "`` For every 10 customers who want to buy beer , nine go away empty-handed , '' she says . \n" +
                "`` But I know that my good fortune is hardly typical of women in Vietnam . '' \n" +
                "Nhan started the brewery in 1992 in a run-down biochemical research center that she rented from the cash-starved government . \n" +
                "Now she brews two brands , her own `` Halida , '' for Vietnamese palates , and Carlsberg , in a joint venture with the Danish beer giant . \n" +
                "Women 's - rights activists have mounted a two-pronged campaign to improve the lives of Vietnamese mothers and their children through better medical care , nutrition and extensive family-planning programs , while making low-interest loans available to women to start businesses . \n" +
                "On this year 's 1,954 th anniversary of the Trung sisters ' uprising , the Vietnam Women 's Union announced that it had created 370,000 new jobs for women at 24 training centers since the economic reforms known as `` doi moi '' took hold in the late 1980s . \n" +
                "Last year the union , which claims 40 percent of Vietnamese women as members , loaned $ 32 million to female entrepreneurs , most of them poor . \n" +
                "`` We have a lot of female heroines in our 4,000-year history , '' says Nguyen Kim Cuc , a spokeswoman for the Women 's Union in Hanoi . \n" +
                "`` But the Trung sisters are only remembered as part of history . \n" +
                "`` There is a street in every town named for them , but that does n't fill the gap between rich and poor , especially in rural areas . '' \n" +
                "But men almost exclusively head banks , companies and all-powerful families that nurture start-up businesses , resulting in what she calls `` significantly high underemployment '' for women . \n" +
                "`` So far , '' says Cuc , `` most women have missed out in Vietnam 's conversion to a market-oriented economy . '' \n" +
                "Cuc 's boss is Truong My Hoa , 47 , the powerful secretary of the Communist Party Central Committee and president of the Vietnam Women 's Union . \n" +
                "Imprisoned by the U.S.-backed South Vietnamese government between 1964 and 1975 for her role as a student protest leader , Hoa says her current mission is to battle for women 's rights , create jobs and prevent the spread of AIDS . \n" +
                "`` A woman 's reproductive function is still seen as a national asset , '' she says , even though government policy is to lower the birth rate to 1.7 percent through intensive family-planning campaigns , especially in rural areas , where 80 percent of Vietnamese live . \n" +
                "`` People still want sons , not daughters , and they keep having children until they get a son who can inherit property . '' \n" +
                "In the meantime , Hoa says , many women in boom towns such as Ho Chi Minh City , formerly Saigon , and Hanoi are victims of growing social evils such as prostitution , drug addiction and HIV infections . \n" +
                "In an eerie echo of what already has happened in nearby Thailand , Vietnam has gone from a single known HIV case in 1990 to more than 1,000 reported cases last year . \n" +
                "`` During the war with the United States , '' Hoa recalls , `` we called on women to be mothers , wives and patriots . \n" +
                "Our slogan was ` increase war production and be ready to fight . '' ' \n" +
                "Recalling the late President Ho Chi Minh 's list of Vietnam 's three enemies -- illiteracy , hunger and foreign domination -- Hoa says : `` Although our constitution and law talk about women 's rights , we have a long way to go to defeat two of those enemies . '' \n" +
                "To get all of the photos , please write or fax -LRB- no telephone calls please , to Gladys Pera , New York Times Syndicate , 122 E. 42nd Street , 14th Floor , New York , N.Y. 10168 , FAX : 212-499-3382 . \n" +
                "Please give your name , title , newspaper , full mailing address and telephone number . \n" +
                "The photos will be mailed to you . \n" +
                "Or give Ms. Pera your Federal Express\\/Airborne number if you want overnight delivery . \n" +
                "All of the photos are by Steve Raymer and are copyright 1994 National Geographic Society : \n" +
                "1 . \n" +
                "-LRB- CARRY -RRB- Women peddlers from the countryside outside the Vietnamese capital , Hanoi , carry fresh produce to streetside stands near the city 's colonial-era opera house -LRB- background -RRB- . \n" +
                "Making up more than half of Vietnam 's 72 million population , women outnumber men in agricultural and low-paying commercial jobs . \n" +
                "-LRB- Horizontal -RRB- \n" +
                "2 . \n" +
                "-LRB- BIKE -RRB- Clad in blue jeans , a Western-style pantsuit and the traditional `` ao dai '' silk tunic and slacks , three generations of Vietnamese females mount a motorbike at Hanoi 's Quan Su Pagoda . \n" +
                "Women are officially guaranteed equal rights in the communist country , but they continue to struggle for higher-paying jobs . \n" +
                "-LRB- Vertical -RRB- \n" +
                "3 . \n" +
                "-LRB- TV -RRB- Eager to share in Vietnam 's economic boom , women have joined the assembly line at a Sony television factory in Ho Chi Minh City , formerly Saigon , where workers earn $ 65 a month for making 250 sets a day . \n" +
                "While Vietnamese women earned equality on the battlefield , now they seek equality in the marketplace . \n" +
                "-LRB- Horizontal -RRB- \n" +
                "4 . \n" +
                "-LRB- GREET -RRB- Women greet one another at Hanoi 's Tran Quoc Pagoda as a beggar pleads for money . \n" +
                "Although some women occupy prominent positions in Vietnam , feminist leaders are trying to relieve the plight of the less fortunate by lowering the birth rate , improving childhood nutrition and creating better jobs for women . \n" +
                "-LRB- Horizontal -RRB- \n";

        String demo1 = "With the nation 's attention riveted again on a Los Angeles courtroom , a knife dealer testified that O.J. Simpson bought a 15-inch knife five weeks before the slashing deaths of his ex-wife and her friend .\n" +
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
                "OPTIONAL 2ND TAKE FOLLOWS . -RRB-";

        Document doc = proc
                .annotate(demo2);

        DiscourseTree dt = doc.discourseTree().get();
        writer.println(dt);
        writer.close();
    }
}