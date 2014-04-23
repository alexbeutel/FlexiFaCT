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

public class Loss extends Configured implements Tool  {

	public static class LossReducer extends MapReduceBase implements Reducer<IntArray, FloatArray, NullWritable, NullWritable> {
		DenseTensor U;
		DenseTensor[] V;
		DenseTensor[] W;

		int rank;

		int N;
		int[] M;
		int[] P;

		int d;
		int dN;
		int[] dM;
		int[] dP;

		boolean[] is2D;

		double step_size = 0.000001;
		int dataSets = 1;
		JobConf thisjob;

		String outputPath; 
		String prevPath;
		boolean KL = false;

		boolean debug = false;

		public void configure(JobConf job) {

			thisjob = job;

			outputPath = job.getStrings("ff.outputPath")[0];
			prevPath = job.getStrings("ff.prevPath", new String[]{""})[0];

			KL = (job.getInt("ff.KL",0) == 1);

			if(job.getInt("ff.debug",0) == 1) {
				debug = true;
			}


			if(job.getInt("ff.M1", 1) == 1) {
				dataSets = 1;
			} else {
				dataSets = 2;
			}
			System.out.println("Data sets: " + dataSets);

			V = new DenseTensor[dataSets];
			W = new DenseTensor[dataSets];
			M = new int[dataSets]; 
			P = new int[dataSets]; 
			dM = new int[dataSets]; 
			dP = new int[dataSets]; 
			is2D = new boolean[dataSets]; 

			d = job.getInt("ff.d", 1);
			rank = job.getInt("ff.rank", 1);
			N = job.getInt("ff.N", 1);
			dN = (int)Math.ceil(1.0 * N/d); 
			U = new DenseTensor(dN,rank);

			System.out.println("d,rank,N,dn:" + d + ", " + rank + ", " + N + ", " + dN);

			for(int i = 0; i < dataSets; i++) {
				M[i] = job.getInt("ff.M"+i, 1);
				P[i] = job.getInt("ff.P"+i, 1);

				is2D[i] = (P[i]==1);

				dM[i] = (int)Math.ceil(1.0 * M[i]/d); 
				dP[i] = (int)Math.ceil(1.0 * P[i]/d); 

				System.out.println("dataSet, M,P,dM,dP: " + i + ", " + M[i] + ", " + P[i] + ", " + dM[i] + ", "  + dP[i]);

				V[i] = new DenseTensor(dM[i],rank);
				W[i] = new DenseTensor(dP[i],rank);
			}

			step_size = job.getFloat("ff.stepSize",0.000001f);

		}

		public boolean checkForFile(String path)  throws IOException {
			FileSystem fs = FileSystem.get(thisjob);
			boolean ans = checkForFile(path,fs);
			fs.close();
			return ans;
		}
		public boolean checkForFile(String path, FileSystem fs)  throws IOException {
			return fs.exists(new Path(path));
		}
		public boolean checkForFile(char c, int index, int iter, FileSystem fs) throws IOException {
			String fp = outputPath + "/iter" + iter + "/" + c + "/" + c + index;
			return checkForFile(fp,fs);
			//System.out.println("Path not found: " + fp);
		}

		public void updateFactorDebug(FSDataInputStream in, DenseTensor M, char c, int minI) throws IOException {
			Scanner s = new Scanner(in);
			while(s.hasNext()) {
				String key = s.next();
				int i = s.nextInt() - minI;
				int j = s.nextInt();
				float val = s.nextFloat();
				if(key.toString().charAt(0) == c) {
					M.set(i,j,val);
				} else {
					System.out.println("ERROR reading input.  Mismatch on factors.");
				}
			}
		}

		public boolean updateFactor(DenseTensor M, char c, int index, int iter, int minI, final Reporter reporter) throws IOException {

			System.out.println("Update " + c + index + ", iter " + iter + " (minI = " + minI + ")");

			//if(debug) {
			//return updateFactorDebug(M,c,index,iter,minI,reporter);
			//}

			//if(M.iter == iter) {
				//return true;
			//}

			FileSystem fs = FileSystem.get(thisjob);

			if(iter >= 0) {
				System.out.println("Check log: " + c + index + ", " + iter);
				String logfile = outputPath + "/log/" + c + index + "." + iter;
				if(!checkForFile(logfile,fs)) {
					fs.close();
					return false;
				}
				System.out.println("Log file found");
			}

			M.reset();

			String path = outputPath + "/iter" + iter + "/" + c + "/" + c + index;
			if(iter < 0 && prevPath != "") {
				path = prevPath + "/" + c + "/" + c + index;
			}

			FileStatus[] allFiles = fs.globStatus(new Path(path + ".*"));

			if(allFiles != null && allFiles.length > 0) {
				for ( FileStatus f : allFiles ) { 

					System.out.println("Update from " + f.getPath().toUri().getPath());
					try {
						//Path pt=new Path(path);
						FSDataInputStream in = fs.open(f.getPath());
						if(debug) {
							updateFactorDebug(in,M,c,minI);
						} else {
							for(int i = 0; i < M.N; i++) {
								for(int j = 0; j < M.M; j++) {
									M.set(i,j,in.readFloat());
								}
							}
						}

						M.iter = iter;
						fs.close();
						System.out.println("success on reading factors");
						return true;
					} catch (EOFException e) {
						//fs.close();
						System.out.println("ERROR EOFException, continue");
						//return false;
						continue;
					} catch (IOException e) {
						if (iter < 0) {
							System.out.println("ERROR reading factors");
							continue;
						} else {
							System.out.println("ERROR reading factors, continue");
							//fs.close();
							//throw e;
							continue;
						}
					} 
				}
			}
			fs.close();
			if(iter < 0) {
				System.out.println("ERROR reading factors exiting true");
				return false;
			}
			return false;
		}

		public void reduce ( final IntArray key, final Iterator<FloatArray> values, final OutputCollector<NullWritable, NullWritable> output, final Reporter reporter) throws IOException { 

			System.out.println("Key: " + key.toString());

			int numSoFar = 0;
			int curSubepoch = -99999;
			int ci = -1;
			int cj = -1;
			int ck = -1;
			while(values.hasNext()) {	// run SGD for U

				FloatArray v = values.next();

				int i = (int)(v.ar[0]);
				int j = (int)(v.ar[1]);
				int dataSet = (int)v.ar[v.ar.length-2];
				int subepoch = (int)v.ar[v.ar.length-1];

				int k = 0;
				float val = v.ar[2];
				if(!is2D[dataSet]) {
					k = (int)(v.ar[2]);
					val = v.ar[3];
				}


				int bi = (int)Math.floor(1.0 * i / dN);
				int bj = (int)Math.floor(1.0 * j / dM[dataSet]);
				int bk = (int)Math.floor(1.0 * k / dP[dataSet]);

				if (subepoch != curSubepoch) {
					System.out.println("New subepoch: " + bi +", " + bj  +", " + bk + ": " + subepoch + " (" + numSoFar + ")");

					if(subepoch == 0) {  // First iteration, possibly get stuff from past run and must load U
						updateFactor(U,'U',bi,-1,dN*bi,reporter);
					} else {

					}

					U.iter = curSubepoch;
					// Input
					boolean doneUpdating = false;
					int waiting = 0;
					while (!doneUpdating) {

						boolean passed = true;
						for(int set = 0; set < dataSets; set++) {

							int tj = (bi + subepoch) % d;
							int tk = (bi + (int)Math.floor(subepoch / d)) %d;

							if(!is2D[set] || subepoch < d) {
								char vc = (set == 0) ? 'V' : 'A';
								passed = updateFactor(V[set],vc,tj,-1,dM[set]*tj,reporter) && passed;
							}

							if(!is2D[set]) {
								char wc = (set == 0) ? 'W' : 'B';
								passed = updateFactor(W[set],wc,tk,-1,dP[set]*tk,reporter) && passed;
							}
						}

						doneUpdating = (curSubepoch < 0) || passed;
						if(!doneUpdating) {
							reporter.progress();
							try{
								Thread.sleep(3000);
							} catch (Exception e) {
							}
							reporter.progress();
							System.out.println("Waiting: "+waiting);
							waiting++;
						}
					}

					curSubepoch = subepoch;
					ci = bi;
					cj = bj;
					ck = bk;
					numSoFar = 0;
				}

				numSoFar++;

				i = i - bi * dN;
				j = j - bj * dM[dataSet];
				k = k - bk * dP[dataSet];

				getLoss(i,j,k,val,reporter,dataSet);
				reporter.progress();

			}
			System.out.println("Last batch: " + numSoFar);

		}

		float pertubation = 0.0001f;
		public void getLoss(int i, int j, int k, float val, Reporter reporter, int dataSet) {
			float eval = 0;
			//String s = "";
			for(int r = 0; r < rank; r++) {
				float prod = U.get(i,r) * V[dataSet].get(j,r);
				//s += U.get(i,r) + "*" +  V[dataSet].get(j,r);
				if(!is2D[dataSet]) {
					prod *= W[dataSet].get(k,r);
					//s += "*" + W[dataSet].get(k,r); 
				}
				eval += prod;
				//s += " + ";
			}

			//System.err.println("Difference: " + val + "\t" +  eval + "\t" + s);
			float loss = (float)Math.pow(val - eval, 2);
			if(KL) { 
				if(eval == 0)
					eval = pertubation;
				loss = (float)(val*Math.log(val/eval)); 
			}
			long loss_estimate = (long)Math.round(loss * 1000);
			reporter.incrCounter("FF Loss","Loss", loss_estimate);
			reporter.incrCounter("FF Loss","Loss-data"+dataSet, loss_estimate);
			reporter.incrCounter("FF Loss","Points", 1);
			reporter.incrCounter("FF Loss","Points-data"+dataSet, 1);
		}


	}


	public int run (String[] args) throws Exception {
		for(int i = 0; i < args.length; i++){
			System.out.println(i + " : " + args[i]);
		}

		if (args.length < 2) {
			System.err.printf("Usage: %s [generic options] <d> <maxDimensions> <dataSets> <key> <input> <input2?> <output> <prevrun?> \n",
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
			//FileOutputFormat.setOutputPath(conf, new Path(args[outputIndex] + "/final/"));
			conf.setStrings("ff.outputPath", args[outputIndex]);
			if(args.length > outputIndex + 1) {
				conf.setStrings("ff.prevPath", args[outputIndex+1]);
			}

			RunningJob job = JobClient.runJob(conf);

			//if(i % d == 0) {
			long loss = job.getCounters().findCounter("FF Loss","Loss").getCounter();
			long count = job.getCounters().findCounter("FF Loss","Points").getCounter();
			long updated = job.getCounters().findCounter("FlexiFaCT","Number Passed").getCounter();

			String key = "-" + args[3];

			BufferedWriter lossResults = new BufferedWriter(new FileWriter("/home/abeutel/loss" + key + ".txt",true)); ;
			//BufferedWriter lossResults = new BufferedWriter(new FileWriter("/home/abeutel/loss" + key + ".txt",true)); ;
			if(!isPaired) {
				lossResults.write(i + "\t" + (loss/1000.0) + "\t" + count + "\t" + updated + "\t" + ((loss/1000.0)/count)+ "\t" + Math.sqrt( (loss/1000.0)/count ) + "\n");
			} else {
				long loss0= job.getCounters().findCounter("FF Loss","Loss-data0").getCounter();
				long count0= job.getCounters().findCounter("FF Loss","Points-data0").getCounter();
				long loss1= job.getCounters().findCounter("FF Loss","Loss-data1").getCounter();
				long count1= job.getCounters().findCounter("FF Loss","Points-data1").getCounter();
				lossResults.write(i + "\t" + (loss/1000.0) + "\t" + count + "\t" + ( (loss/1000.0)/count )+ "\t" + Math.sqrt( (loss/1000.0)/count ) + "\t" + updated + "\t" + (loss0/1000.0) + "\t" + count0 + "\t" + (loss1/1000.0) + "\t" + count1 + "\n");
			}
			lossResults.close();
			//}
		}	

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
		JobConf conf = new JobConf(getConf(), Loss.class); 
		conf.setJobName("Loss-"+sub);

		if(!isPaired) conf.setMapperClass(FFMapper.class); 
		conf.setReducerClass(LossReducer.class);

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


	/**
	 * Required parameters:
	 * ff.subepoch (possibly set in run() with a loop
	 * ff.N size of input matrix 
	 * ff.M size of input matrix
	 * ff.d number of blocks to do in parallel (number of reducers) 
	 * ff.rank rank of matrix (dimension of U and V)
	 */
	public static void main(String[] args) throws Exception {
		int exitCode = ToolRunner.run(new Loss(), args);
		System.exit(exitCode); 
	}

}
