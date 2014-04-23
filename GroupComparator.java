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

public class GroupComparator extends WritableComparator {
	protected GroupComparator() {
		super(IntArray.class, true);
	}
	public int compare(WritableComparable w1, WritableComparable w2) {
		IntArray t1 = (IntArray) w1;
		IntArray t2 = (IntArray) w2;

		int bi = t1.ar[0];
		int bi2= t2.ar[0];

		return bi - bi2;
	}
}

