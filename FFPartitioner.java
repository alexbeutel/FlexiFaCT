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

public class FFPartitioner implements Partitioner<IntArray,FloatArray> {

	int d = 1;
	public void configure(JobConf job) {
		d = job.getInt("ff.d", 1);
	}

	public int getPartition(IntArray key, FloatArray value, int numPartitions) {

		int bi = key.ar[0];
		int bj = key.ar[1];
		int bk = key.ar[2];

		return (bi + bj * d + bk * d * d) % numPartitions;

	}
}
