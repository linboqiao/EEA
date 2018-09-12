package limo.test;

import static org.junit.Assert.assertEquals;

import java.io.File;

import limo.exrel.utils.Index;
import limo.exrel.utils.Index.OpenMode;

import org.junit.Test;

public class IndexTest {
	
	@Test
	public void testWriteIndex() {
		
		File f = new File("/tmp/test.idx");
		if (f.exists()) f.delete();
		
		Index test = new Index("/tmp/test.idx", OpenMode.WRITE);
		test.put("first", 0);
		test.put("second", 1);
		test.put("Frontières", 2);
		
		//assertEquals(1, test.numKeys());
		assertEquals(3, test.numRecords());

		test.close(); // writing done only when closing
	}
	
	@Test
	public void testAppendIndex() {
		
		Index test = new Index("/tmp/test.idx", OpenMode.APPEND);
		test.dumpIndex();
		test.put("another",3);
		test.dumpIndex();
		assertEquals(4, test.numRecords());
		
		test.close(); // writing done only when closing
	}
	
	@Test
	public void testReadIndex() {
		
		Index test = new Index("/tmp/test.idx", OpenMode.READ);
		String first = test.get(0);
		String third = test.get(2);
		assertEquals("first", first);
		assertEquals("Frontières",third);
	
		test.close();
	}
	
/*	@Test
	public void testReadIndexReal() {
		
		Index test = new Index(System.getProperty("user.home")+"/project/limosine/tools/limo/debug.idx", OpenMode.READ);
		String first = test.get(0);
		//String third = test.get(2);
		assertEquals("NONE	qid:0 |BT| (NP (NP (NP (E1 (NNP KUALA)(NNP LUMPUR)))(, ,)(NP (NNP April)(CD 18)))(PRN (-LRB- -LRB-)(NP (E2 (NNP AFP))))) |ET|	GPE@ORG	KUALA/B-GPE.NAM LUMPUR/I-GPE.NAM ,/O April/O 18/O -LRB-/O AFP/B-ORG.NAM -RRB-/O Malaysia/O 's/O Appeal/O Court/O Friday/O refused/O to/O overturn/O the/O conviction/O and/O nine-year/O jail/O sentence/O imposed/O on/O ex-deputy/O prime/O minister/O Anwar/O Ibrahim/O for/O sodomy/O ./O", first);
		
	
	}*/

}
