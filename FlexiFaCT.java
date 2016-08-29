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

public class FlexiFaCT extends Configured implements Tool  {

	public int run (String[] args) throws Exception {

		long startTime = System.currentTimeMillis() / 1000L;

		for(int i = 0; i < args.length; i++){
			System.out.println(i + " : " + args[i]);
		}

        if (args.length < 2) {
			System.err.printf("Usage: %s [Hadoop Options] <d> <maxDimensions> <dataSets> <key> <input> <input2?> <output> <prevrun?> \n"
					+ "Required Hadoop Options:\n"
					+ "ff.N=# Number of columns (or rows, whatever the first dimension is) for the primary matrix or tensor.  (This dimension will be shared with coupled data.)\n"
					+ "ff.M0=# Range of second dimension in 1st data set\n"
					+ "ff.rank=# Rank of the decomposition\n"
					+ "ff.stepSize=# Step size for SGD.  This is typically 1/N where N is the number of non-zero elements\n"
					+ "mapred.reduce.tasks=# This should be set to the value of d so that the number of reducers matches the parallelism of the problem precisely\n\n"
					+ "Optional Hadoop Options:\n"
					+ "ff.P0=# Range of third dimension in 1st data set\n"
					+ "ff.M1=# Range of second dimension in 2nd data set\n"
					+ "ff.P1=# Range of third dimension in 2nd data set\n"
					+ "ff.weight0=# - Weight data set 1 loss by this weight\n"
					+ "ff.weight1=# - Weight data set 2 loss by this weight\n"
					+ "ff.initMean=# - We will initialize the factors to be a Gaussian around this number (with variance 1)\n"
					+ "ff.regularizerLambda=# - Weight L1 penalty with this lambda value\n"
					+ "ff.sparse=1 - If set to 1 will add an L1 penalty to the loss\n"
					+ "ff.nnmf=1 - If set to 1 will do a projection to make sure all factors are non-negative\n"
					+ "ff.kl=1 - If set to 1 will use the KL divergence for the loss function\n"
					+ "ff.debug=1 - If set to 1 will use plain text files and will be more verbose\n\n",
					getClass().getSimpleName()); ToolRunner.printGenericCommandUsage(System.err); 
			return -1;
		}

		int d = Integer.parseInt(args[0]);

		boolean is2D = (Integer.parseInt(args[1]) < 3);
		int iter = d;
		if(!is2D) {
			iter = d*d;
		}

		boolean isPaired = (Integer.parseInt(args[2]) == 2);

		iter = 1;
		for(int i = 0; i < iter; i++) {
			System.out.println("Sub-iteration " + i);

			JobConf conf = getJobInstance(i,isPaired);
			FileSystem fs = FileSystem.get(conf);

			conf.setInt("ff.d", d);
			conf.setInt("ff.subepoch", i);

			int outputIndex = 4;
			if(isPaired) {
				MultipleInputs.addInputPath(conf, new Path(args[4]), KeyValueTextInputFormat.class, FFMapper.class);
				MultipleInputs.addInputPath(conf, new Path(args[5]), KeyValueTextInputFormat.class, FFMapperPaired.class);
				outputIndex = 6;
			} else {
				FileInputFormat.addInputPath(conf, new Path(args[4])); 
				outputIndex = 5;
			}
			//FileOutputFormat.setOutputPath(conf, new Path(args[outputIndex] + "/iter"+i+"/"));
			//FileOutputFormat.setOutputPath(conf, new Path(args[outputIndex] + "/final/"));
			conf.setStrings("ff.outputPath", args[outputIndex]);
			if(args.length > outputIndex + 1) {
				conf.setStrings("ff.prevPath", args[outputIndex+1]);
			} else {
				conf.setStrings("ff.prevPath", "");
			}

			RunningJob job = JobClient.runJob(conf);

		}	

		long endTime = System.currentTimeMillis() / 1000L;
		BufferedWriter timeResults = new BufferedWriter(new FileWriter("time" +"-" + args[3]+ ".txt",true)); ;
		timeResults.write(startTime + "\t" + endTime + "\t" + (endTime-startTime) + "\n");
		timeResults.close();

		return 0;
	}
	public void addFilesToCache(String path, FileSystem fs, JobConf conf) throws Exception {

		if(fs.exists(new Path(path))) {
			FileStatus[] Vfiles = fs.listStatus(new Path(path));
			for(FileStatus f : Vfiles) {
				DistributedCache.addCacheFile(f.getPath().toUri(), conf);
			}
		}

	}

	public JobConf getJobInstance(int sub, boolean isPaired) {
		JobConf conf = new JobConf(getConf(), FlexiFaCT.class); 
		conf.setJobName("FF-"+sub);

		if(!isPaired) conf.setMapperClass(FFMapper.class); 
		conf.setReducerClass(FFReducer.class);

		conf.setInputFormat(KeyValueTextInputFormat.class);
		conf.setOutputFormat(NullOutputFormat.class);

		conf.setMapOutputKeyClass(IntArray.class); 
		conf.setMapOutputValueClass(FloatArray.class);

		conf.setPartitionerClass(FFPartitioner.class);
		conf.setOutputKeyComparatorClass(KeyComparator.class);
		conf.setOutputValueGroupingComparator(GroupComparator.class);


		conf.setOutputKeyClass(Text.class); 
		conf.setOutputValueClass(Text.class);

		return conf;
	}

	public static void main(String[] args) throws Exception {
		int exitCode = ToolRunner.run(new FlexiFaCT(), args);
		System.exit(exitCode); 
	}

}
