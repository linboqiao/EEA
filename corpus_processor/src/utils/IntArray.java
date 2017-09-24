package utils;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class IntArray implements Comparable
{
	public int start;
	public int end;
	private volatile int hashCode = 0; // (See Item 48)
	public IntArray(int start, int end)
	{
		this.start = start;
		this.end = end;
	}

	public int spanCompare(IntArray aObj){
		
		if( (   aObj.start == this.start) && (aObj.end == this.end)   ) {
			return OverlapType.exact;
		}
		
		/* a obj subsumes this span  */
		if( (   aObj.start <= this.start)  && (aObj.end >=  this.end)   ) {
			return OverlapType.subsumes;
		}
		
		/* a obj is a subset of this span  */
		if( (   aObj.start >= this.start)  && (aObj.end <=  this.end)   ) {
			return OverlapType.subset;
		}
		
		if( (   aObj.start < this.start)  && (aObj.end <  this.end)  && (aObj.end >  this.start) ) {
			return OverlapType.overlap;
		}
		
		
		if( (   aObj.start > this.start) && ( aObj.start < this.end) && (aObj.end >  this.end)   ) {
			return OverlapType.overlap;
		}
		
		
		if( (   aObj.start < this.start)  &&  (aObj.end <=  this.start) ) {
			return OverlapType.seperate;
		}
		if( (   aObj.start > this.start) && ( aObj.start >= this.end) && (aObj.end >  this.end)  ) {
			return OverlapType.seperate;
		}
		
		return -1;
		
		
		
	}
	
	
	/* Overload compareTo method */

	public int compareTo(Object obj)
	{
		IntArray tmp = (IntArray)obj;
		
		if(this.start<tmp.start){
			return -1;
		}
		else if(this.start == tmp.start && this.end < tmp.end)
		{
			/* instance lt received */
			return -1;
		}else if(this.start>tmp.start){
			return 1;
		}
		else if(this.start == tmp.start && this.end>tmp.end)
		{
			/* instance gt received */
			return 1;
		}
		/* instance == received */
		return 0;
	}
	
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof IntArray))
			return false;
		IntArray pn = (IntArray)o;
		return pn.getStart() == this.getStart() &&
		pn.getEnd() == this.getEnd();
	}

	public int hashCode() {
		if (hashCode == 0) {
			int result = 17;
			result = 37*result + this.start;
			result = 37*result + this.end;
			hashCode = result;
		}
		return hashCode;
	}
	
	public int getStart(){
		return this.start;
	}
	public int getEnd(){
		return this.end;
	}

	public static void main(String[] args)
	{
		/* Create an array of Student object */

		IntArray[] students = new IntArray[4];
		students[0] = new IntArray(52645, 52678);
		students[1] = new IntArray(98765, 98789);
		students[2] = new IntArray(1354, 1909);
		students[3] = new IntArray(1354, 1358);

		//List<IntArray> aList = Arrays.asList(students);
		//Collections.sort(aList);
		
		/* Sort array */
		Arrays.sort(students);

		/* Print out sorted values */

		for(int i = 0; i < students.length; i++)
		{
			System.out.println(students[i].start +" "+
					students[i].end);
		}
	}

	public void setStart(int start) {
		this.start = start;
	}

	public void setEnd(int end) {
		this.end = end;
	}

}