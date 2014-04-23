import java.lang.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.text.*;

public class DenseTensor implements Tensor {
	int N;
	int M;
	int P;
	float[] data; // Keep one dimensional for memory efficiency
	int iter;

	public DenseTensor(int n, int m) {
		this(n,m,1);
	}

	public DenseTensor(int n, int m, int p) {
		N = n;
		M = m;
		P = p;
		data = new float[N*M*P];
		iter = -1;
		reset();
	}

	private int getIndex(int i, int j, int k) { 
		return (i * M + j) * P + k; 
	}

	public float get(int i, int j) {
		return get(i,j,0);
	}

	public float get(int i, int j, int k) {
		return data[getIndex(i,j,k)];
	}

	public void set(int i, int j, float value) {
		set(i,j,0,value);
	}

	public void set(int i, int j, int k,  float value) {
		data[getIndex(i,j,k)] = value;
	}
	public void zero() {
		for(int i = 0; i < N*M*P; i++) {
			data[i] = 0.0f;
		}
	}

	public void reset(float mean) {
		Random r = new Random();
		for(int i = 0; i < N*M*P; i++) {
			data[i] = (float)(r.nextGaussian()/2.0f + mean);
			if(Float.isNaN(data[i])) {
				System.out.println("NaN Error on Reset");
			}
		}
	}

	public void reset() {
		reset(0);
	}
}
