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

public class KeyComparator extends WritableComparator {
	protected KeyComparator() {
		super(IntArray.class, true);
	}
	public int compare(WritableComparable w1, WritableComparable w2) {
		IntArray t1 = (IntArray) w1;
		IntArray t2 = (IntArray) w2;

		int bi = t1.ar[0]; 
		int o = t1.ar[3];  

		int bi2= t2.ar[0];
		int o2= t2.ar[3]; 

		if(bi != bi2) {
			return bi - bi2;
		}
		return o - o2;
	}
}

