import java.io.*;
import java.util.*;
import java.text.*;
import java.net.*;

import org.apache.hadoop.conf.*;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.mapred.lib.*;
import org.apache.hadoop.util.*;

import org.apache.hadoop.filecache.*;

public class KeyComparator extends WritableComparator {
	protected KeyComparator() {
		super(IntArray.class, true);
	}
	public int compare(WritableComparable w1, WritableComparable w2) {
		IntArray t1 = (IntArray) w1;
		IntArray t2 = (IntArray) w2;

		int bi = t1.ar[0]; 
		int o = t1.ar[3];  

		int bi2= t2.ar[0];
		int o2= t2.ar[3]; 

		if(bi != bi2) {
			return bi - bi2;
		}
		return o - o2;
	}
}

