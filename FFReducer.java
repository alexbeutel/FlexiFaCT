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

public class FFReducer extends MapReduceBase implements Reducer<IntArray, FloatArray, NullWritable, NullWritable> {
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
	double[] weight;
	int dataSets = 1;
	JobConf thisjob;

	String outputPath; 
	String prevPath;

	boolean debug = false;
	String taskId;

	boolean sparse = false;
	float lambda = 10;
	boolean nonNegative = false;
	boolean KL = false;

	float initMean = 0;

	public void configure(JobConf job) {
		//System.out.println("TEST");

		thisjob = job;

		outputPath = job.getStrings("ff.outputPath")[0];
		prevPath = job.getStrings("ff.prevPath", new String[]{""})[0];

		if(job.getInt("ff.debug",0) == 1) {
			debug = true;
		}

		sparse = (job.getInt("ff.sparse",0) == 1);
		nonNegative = (job.getInt("ff.nnmf",0) == 1);
		KL = (job.getInt("ff.KL",0) == 1);

		lambda = job.getFloat("ff.regularizerLambda",10);
		initMean = job.getFloat("ff.initMean",0);

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
		weight = new double[dataSets]; 

		d = job.getInt("ff.d", 1);
		rank = job.getInt("ff.rank", 1);
		N = job.getInt("ff.N", 1);
		dN = (int)Math.ceil(1.0 * N/d); 
		U = new DenseTensor(dN,rank);

		System.out.println("d,rank,N,dn:" + d + ", " + rank + ", " + N + ", " + dN);

		for(int i = 0; i < dataSets; i++) {
			M[i] = job.getInt("ff.M"+i, 1);
			P[i] = job.getInt("ff.P"+i, 1);

			weight[i] = job.getFloat("ff.weight"+i,1.0f);

			is2D[i] = (P[i]==1);

			dM[i] = (int)Math.ceil(1.0 * M[i]/d); 
			dP[i] = (int)Math.ceil(1.0 * P[i]/d); 

			System.out.println("dataSet, M,P,dM,dP: " + i + ", " + M[i] + ", " + P[i] + ", " + dM[i] + ", "  + dP[i]);

			V[i] = new DenseTensor(dM[i],rank);
			W[i] = new DenseTensor(dP[i],rank);
		}

		step_size = job.getFloat("ff.stepSize",0.000001f);
		System.out.println("Step size: " + step_size);

		taskId = getAttemptId(job);
	}

	public static String getAttemptId(Configuration conf) { // throws IllegalArgumentException {
		if (conf == null) {
			return "";
			//throw new NullPointerException("conf is null");
		}

		String taskId = conf.get("mapred.task.id");
		if (taskId == null) {
			return "";
			//throw new IllegalArgumentException("Configutaion does not contain the property mapred.task.id");
		}

		String[] parts = taskId.split("_");
		//if (parts.length != 6 || !parts[0].equals("attempt") || (!"m".equals(parts[3]) && !"r".equals(parts[3]))) {
			//throw new IllegalArgumentException("TaskAttemptId string : " + taskId + " is not properly formed");
		//}
		return parts[parts.length - 1];

		//return parts[4] + "-" + parts[5];
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
	}

	public void writeLog(char c, int index, int iter, FileSystem fs) throws IOException {
		try {
			String fp = outputPath + "/log";
			Path path = new Path(fp);
			if (!fs.exists(path)) {
				fs.mkdirs(path);
			}

			fp += "/" + c  + index + "." + iter;
			path = new Path(fp);
			if (!fs.exists(path)) {
				fs.createNewFile(path);
			}
		} catch (IOException e) { }
	}

	public void writeFactor(DenseTensor T, char c, int index, int iter, int minI, final Reporter reporter) throws IOException {

		//System.out.println("WRITE FACTOR: " + c + index + ", " + iter);
		FileSystem fs = FileSystem.get(thisjob);

		String fp = outputPath + "/iter" + iter + "/" + c;
		Path path = new Path(fp);
		if (!fs.exists(path)) {
			fs.mkdirs(path);
		}

		fp += "/" + c + index + "." + taskId;
		FSDataOutputStream out = fs.create(new Path(fp));

		System.out.println("Write to " + fp);
		for(int i = 0; i < T.N; i++) {
			for(int j = 0; j < T.M; j++) {
				if(!debug) {
					//if(sparse) {
						//if(T.get(i,j) != 0) {
							//out.writeInt(j);
							//out.writeInt(j);
							//out.writeFloat(T.get(i,j));
						//}
					//} else {
						out.writeFloat(T.get(i,j));
						//}
				} else {
					//if(Double.isNaN(T.get(i,j))) {
						//System.err.println("Error writing " + i + ", " + j + " - " + minI + ": " + T.get(i,j));
					//}
					String val = c + "" + index + "\t" + (i+minI) + "\t" + j + "\t" + T.get(i,j) + "\n";
					out.writeBytes(val);
				}
			}
		}

		writeLog(c,index,iter,fs);
		fs.close();

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

		if(M.iter == iter) {
			return true;
		}

		FileSystem fs = FileSystem.get(thisjob);

		if(iter >= 0) {
			String logfile = outputPath + "/log/" + c + index + "." + iter;
			System.out.println("Check log: " + c + index + ", " + iter + ": " + logfile);
			if(!checkForFile(logfile,fs)) {
				fs.close();
				return false;
			}
			System.out.println("Log file found");

		}

		M.reset(initMean);

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
			return true;
		}
		return false;
	}

	public void reduce (
		final IntArray key, 
		final Iterator<FloatArray> values, 
		final OutputCollector<NullWritable, NullWritable> output, 
		final Reporter reporter
	) throws IOException { 

		System.out.println("Key: " + key.toString());
		
		int numSoFar = 0;
		int curSubepoch = -99999;
		int ci = -99999;
		int cj = -99999;
		int ck = -99999;
		int Ublock = -99999;
		boolean first = true;
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

			if(Float.isNaN(val) || Float.isInfinite(val) || Math.abs(val) > 10) { 
				System.out.print("val NaN: ");
				System.out.println(v.toString());
			}


			int bi = (int)Math.floor(1.0 * i / dN);
			int bj = (int)Math.floor(1.0 * j / dM[dataSet]);
			int bk = (int)Math.floor(1.0 * k / dP[dataSet]);

			if(first) {
				Ublock = bi;
				first = false;
			}

			if (subepoch != curSubepoch) {
				System.out.println("New subepoch: " + bi +", " + bj  +", " + bk + ": " + subepoch + " (" + numSoFar + ")");

				int tj = (bi + curSubepoch) % d;
				int tk = (bi + (int)Math.floor(curSubepoch / d)) %d;

				int tjNew = (bi + subepoch) % d;
				int tkNew = (bi + (int)Math.floor(subepoch / d)) %d;

				System.out.println("Tj,Tk,TjNew,TkNew: " + bi +", " + tj  +", " + tk + ", " + tjNew + ", " + tkNew);

				if(subepoch == 0) {  // First iteration, possibly get stuff from past run and must load U
					updateFactor(U,'U',bi,curSubepoch,dN*bi,reporter);
				} else {
					// Output
					System.out.println("Output!");
					for(int set = 0; set < dataSets; set++) {

						//if(!is2D[set] || tkNew == 0) {
						if(!is2D[set] || subepoch < d) {
							// output V[set]
							if(tj != tjNew) {
								char vc = (set == 0) ? 'V' : 'A';
								writeFactor(V[set],vc,tj,curSubepoch,dM[set]*tj,reporter);
							}
						}

						if(!is2D[set]) {
							// output W[set]
							if(tk != tkNew) {
								char wc = (set == 0) ? 'W' : 'B';
								writeFactor(W[set],wc,tk,curSubepoch,dP[set]*tk,reporter);
							}
						}
					}

				}

				U.iter = curSubepoch;
				// Input
				boolean doneUpdating = false;
				int waiting = 0;
				while (!doneUpdating) {

					boolean passed = true;
					for(int set = 0; set < dataSets; set++) {

						//if(!is2D[set] || tkNew == 0) {
						if(!is2D[set] || subepoch < d) {
							if(tj != tjNew) {
								// read V[set]
								char vc = (set == 0) ? 'V' : 'A';
								passed = updateFactor(V[set],vc,tjNew,curSubepoch,dM[set]*tjNew,reporter) && passed;
							}
						}

						if(!is2D[set]) {
							// read W[set]
							if(tk != tkNew) {
								char wc = (set == 0) ? 'W' : 'B';
								passed = updateFactor(W[set],wc,tkNew,curSubepoch,dP[set]*tkNew,reporter) && passed;
							}
						}
					}

					doneUpdating = (curSubepoch < 0) || passed;
					if(!doneUpdating) {
						System.out.println("Waiting: "+waiting);
						reporter.incrCounter("FlexiFaCT", "Time Waiting", 1);
						reporter.incrCounter("Time waiting", "U" + Ublock, 1);
						reporter.progress();
						try{
							Thread.sleep(3000);
						} catch (Exception e) { }
						reporter.progress();
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


			// Alex: check this
			i = i - bi * dN;
			j = j - bj * dM[dataSet];
			k = k - bk * dP[dataSet];

			float coeff = getGradient(i,j,k,val,dataSet);
			coeff = (float)(coeff * weight[dataSet]);

			if(Float.isNaN(coeff) || Float.isInfinite(coeff)) { 
				System.out.print("coeff NaN: ");
				System.out.println(i + ", " + j + ", " + k + ", " + val + ", " + dataSet + ": " + coeff);
			}

			float[] U_i = new float[rank];
			float[] V_j = new float[rank];
			float[] W_k = new float[rank];

			for(int r=0;r<rank;r++){
				U_i[r] = U.get(i,r);
				V_j[r] = V[dataSet].get(j,r);

				if(!is2D[dataSet]) {
					W_k[r] = W[dataSet].get(k,r);
				} else {
					W_k[r] = 1.0f;
				}
			}

			reporter.progress();

			for(int r=0;r<rank;r++){
				setGradient(U,i,r, coeff, V_j,W_k);
				setGradient(V[dataSet],j,r, coeff, U_i,W_k);

				if(!is2D[dataSet]) {
					setGradient(W[dataSet],k,r, coeff, U_i,V_j);
				}
			}

			reporter.incrCounter("FlexiFaCT", "Number Processed", 1);
			reporter.incrCounter("Subepochs", "U" + Ublock, 1);
		}

		System.out.println("Last batch: " + numSoFar);

		writeFactor(U,'U',Ublock,curSubepoch,dN*Ublock,reporter);

		for(int set = 0; set < dataSets; set++) {

			int tj = (Ublock + curSubepoch) % d;
			int tk = (Ublock + (int)Math.floor(curSubepoch / d)) %d;

			// output V[i]
			char vc = (set == 0) ? 'V' : 'A';
			writeFactor(V[set],vc,tj,curSubepoch,dM[set]*tj,reporter);

			if(!is2D[set]) {
				// output W[i]
				char wc = (set == 0) ? 'W' : 'B';
				writeFactor(W[set],wc,tk,curSubepoch,dP[set]*tk,reporter);
			}
		}

	}
	

	//private void setGradient(Matrix M, int i, int r, double coeff, ArrayList<Double> M1, ArrayList<Double> M2) {
	private void setGradient(DenseTensor M, int i, int r, double coeff, float[] M1, float[] M2) {
		if(KL) {
			setGradientKL(M, i, r, coeff, M1, M2);
			return;
		}

		//double newVal = M.get(i, r) - step_size * coeff * M1.get(r) * M2.get(r);
		float newVal = (float)(M.get(i, r) - step_size * coeff * M1[r] * M2[r]);
		
		if(Float.isNaN(newVal) || Float.isInfinite(newVal)) { 
			System.out.print("newVal NaN: ");
			System.out.println(i + ", " + r + ", " + coeff + ": " + newVal);
		}

		if(sparse) {
			newVal = softThreshold(newVal,lambda * step_size);
		}

		if(nonNegative && newVal < 0) {
			newVal = 0f;
		}

		//System.out.println("Change: " + (step_size * coeff * M1[r] * M2[r]));
		M.set(i, r, newVal);
	}


	private float softThreshold(float val, double lambda){
		if(val > lambda)
			return (float)(val - lambda);
		if(val < -1.0f * lambda)
			return  (float)(val + lambda);
		return 0f;
	}


	float pertubation = 0.00001f;
	private void setGradientKL(DenseTensor M, int i, int r, double coeff, float[] M1, float[] M2) {
		float newVal = (float)(M.get(i,r) - step_size*coeff*M1[r]*M2[r]);
		
		if(Float.isNaN(newVal) || Float.isInfinite(newVal)) { 
			System.out.print("newVal NaN: ");
			System.out.println(i + ", " + r + ", " + coeff + ": " + newVal);
		}

		if(newVal < 0) {
			newVal = 0f;
		}

		M.set(i, r, newVal);
	}

	private float getGradientKL(int i, int j, int k,  float val, int dataSet){
		float sum = 0;
		for(int r=0;r<rank;r++) {
            float prod = U.get(i, r)*V[dataSet].get(j, r); 
            if(!is2D[dataSet]) { 
                prod *= W[dataSet].get(k,r);
            }
            sum+=prod;
        }
        if(Float.isNaN(sum) || Float.isInfinite(sum)){
            System.out.println("getGradient sum NaN: " + sum);
        }
        //System.out.println(i + "," + j + "," + k + ": " + val + " - " + sum);
		//return -1.0f*val/sum;
		if(sum == 0) {
			return -1.0f*val/pertubation;
		}
        return -1.0f*(val)/(sum);
	}

	private float getGradient(int i, int j, int k,  float val, int dataSet){
		if(KL) {
			return getGradientKL(i,j,k,val,dataSet);
		}

		float sum = 0;
		for(int r=0;r<rank;r++) {
			float prod = U.get(i, r)*V[dataSet].get(j, r); 
			if(!is2D[dataSet]) { 
				prod *= W[dataSet].get(k,r);
			}
			sum+=prod;
		}
		if(Float.isNaN(sum) || Float.isInfinite(sum)){
			System.out.println("getGradient sum NaN: " + sum);
		}
		//System.out.println(i + "," + j + "," + k + ": " + val + " - " + sum);
		return -2.0f*(val-sum);
	}


}
	
