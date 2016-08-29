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
