import java.lang.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.text.*;


public class SparseTensor implements Tensor {
	private HashMap<String, Float> data;
	public SparseTensor() {
		data = new HashMap<String, Float>();
	}

	private String getKey(int i, int j) {
		return getKey(i,j,0);
	}

	private String getKey(int i, int j, int k) {
		return i + "," + j + "," + k;
	}

	public float get(int i, int j) {
		return get(i,j,0);
	}

	public float get(int i, int j, int k) {
		String key = getKey(i,j,k);
		if ( data.containsKey(key) ) {
			return data.get(key);
		} else {
			// Throw error?
		}
		return -1;
	}

	public void set(int i, int j, float value) {
		set(i,j,0,value);
	}

	public void set(int i, int j, int k, float value) {
		String key = getKey(i,j,k);
		data.put(key, value);
	}
	
	public void reset() {
		data.clear();
	}

}

