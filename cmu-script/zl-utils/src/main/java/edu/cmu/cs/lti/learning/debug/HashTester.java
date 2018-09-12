package edu.cmu.cs.lti.learning.debug;

import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/6/15
 * Time: 3:11 PM
 *
 * @author Zhengzhong Liu
 */
public class HashTester {
    HashFunction hasher = Hashing.murmur3_32();

    public int hash(String s) {
        HashCode code = hasher.hashString(s, Charsets.UTF_8);
        return code.asInt();
    }

    public void cutToRange(int hv, int bitsOfRange) {
        int k = (int) Math.pow(2, bitsOfRange);
        System.out.println("Cut range for : " + hv);
        System.out.println("Take modulo k : " + (hv > 0 ? hv % k : hv % k + k));
        System.out.println("Take modulo k -1 : " + (hv > 0 ? hv % (k - 1) : hv % (k - 1) + k));
        System.out.println(String.format("Mask by k-1 : %d", hv & (k - 1)));
    }

    public static void main(String[] args) {
        HashTester tester = new HashTester();

        int code1 = tester.hash("LemmaBefore_i=-2::Benjamin:Ti=Manufacture_Artifact");
        int code2 = tester.hash("Lemma_i=0::terrorism:Ti=Conflict_Attack");

        tester.cutToRange(code1, 24);
        tester.cutToRange(code2, 24);
    }
}
