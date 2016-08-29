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

