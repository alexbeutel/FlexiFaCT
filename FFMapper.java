/*
 *  Copyright 2016 Alex Beutel alex@beu.tel
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  	http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import java.io.*;
import java.util.*;
import java.text.*;

import org.apache.hadoop.conf.*;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.*;

import org.apache.hadoop.filecache.*;

public class FFMapper extends MapReduceBase implements Mapper<Text, Text, IntArray, FloatArray> {
	int s = 0;
    int N = 0;
    int M = 0;
    int P = 0;
    int d = 0;
	int dN = 0;
	int dM = 0;
	int dP = 0;
	int rank;

	boolean is2D;

	Random rand;

	int dataSet = 0;

	public void configure(JobConf job) {
		rand = new Random();

		dataSet = 0;

		s = job.getInt("ff.subepoch", 0);
		N = job.getInt("ff.N", 1);
		M = job.getInt("ff.M0", 1);
		P = job.getInt("ff.P0", 1);
		is2D = (P==1);

		d = job.getInt("ff.d", 1);
		rank = job.getInt("ff.rank", 1);

		dN = (int)Math.ceil(1.0 * N/d); 
		dM = (int)Math.ceil(1.0 * M/d); 
		dP = (int)Math.ceil(1.0 * P/d); 

		System.out.println("N,M,P,d,s: " + N + ", " + M + ", " + P + ", " + d + ", " + s);
		System.out.println("dN,dM,dP: " + dN + ", " + dM + ", " + dP);

	}

	public void map(Text key, Text value, OutputCollector<IntArray,FloatArray> output, Reporter reporter) throws IOException {

		//System.out.println("Key: " + key.toString());
		//System.out.println("Value: " + value.toString());
		String[] vals1 = key.toString().split("\\s+");
		String[] vals2 = value.toString().split("\\s+");
		String[] vals = new String[vals1.length + vals2.length];
		int cnt = 0;
		for(int i = 0; i < vals1.length; i++) {
			vals[cnt] = vals1[i];
			cnt++;
		}
		for(int i = 0; i < vals2.length; i++) {
			vals[cnt] = vals2[i];
			cnt++;
		}
		
		// Load from key/values
		int i = 0; 
		int j = 0; 
		int k = 0; 
		float val = 0;
		try {
			i = Integer.parseInt(vals[0]); 
			j = Integer.parseInt(vals[1]); 

			val = Float.parseFloat(vals[2]);
			if(!is2D) {
				k = Integer.parseInt(vals[2]);
				val = Float.parseFloat(vals[3]); 
			}
		} catch (Exception e) {
			System.out.println("Error on input: ");
			System.out.println("Key: " + key.toString());
			System.out.println("Value: " + value.toString());
			return;
		}

		int bi = (int)Math.floor(1.0 * i / dN);
		int bj = (int)Math.floor(1.0 * j / dM);
		int bk = (int)Math.floor(1.0 * k / dP);

		//System.out.println(bi + ", " + bj);

		int cj = (bi + s) % d;

		int ck = (bi + (int)Math.floor(s / d)) %d;

		//if(dataSet == 1 && is2D)
		
		int order = 9999;
		if (is2D) {
			order = (bj - bi + d) % d;
		} else { 
			order = d * ((bk - bi + d) % d) + ((bj - bi + d) % d);
			//order = d * ((bj - bi + d) % d) + ((bk - bi + d) % d);
		}

		int multiple = d;
		if(!is2D) {
			multiple = d*d;
		}

		multiple = 0;
		int epochs = 1;
		for(int e = 0; e < epochs; e++) {
			IntArray newkey = new IntArray(new int[]{bi,bj,bk,order + e*multiple});
			FloatArray newvalue = new FloatArray(new float[]{i,j,val,dataSet,order + e*multiple});
			if(!is2D) {
				newvalue = new FloatArray(new float[]{i,j,k,val,dataSet,order + e*multiple}); 
			}

			output.collect(newkey, newvalue);
			reporter.incrCounter("FlexiFaCT", "Number Passed", 1);
			reporter.incrCounter("FlexiFaCT", "Number Passed-"+dataSet, 1);
		}

		reporter.incrCounter("FlexiFaCT", "Number Total", 1);
	}

}

