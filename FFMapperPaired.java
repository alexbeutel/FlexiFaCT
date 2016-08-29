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

public class FFMapperPaired extends FFMapper {

	public void configure(JobConf job) {
		rand = new Random();

		dataSet = 1;

		s = job.getInt("ff.subepoch", 0);
		//N = job.getInt("ff.N2", 1);
		N = job.getInt("ff.N", 1);
		M = job.getInt("ff.M1", 1);
		P = job.getInt("ff.P1", 1);
		is2D = (P==1);

		d = job.getInt("ff.d", 1);
		rank = job.getInt("ff.rank", 1);

		dN = (int)Math.ceil(1.0 * N/d); 
		dM = (int)Math.ceil(1.0 * M/d); 
		dP = (int)Math.ceil(1.0 * P/d); 

		System.out.println("N,M,P,d,s: " + N + ", " + M + ", " + P + ", " + d + ", " + s);
		System.out.println("dN,dM,dP: " + dN + ", " + dM + ", " + dP);

	}

}

