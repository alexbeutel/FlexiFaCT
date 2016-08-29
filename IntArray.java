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
import org.apache.hadoop.io.*;

public class IntArray implements WritableComparable<IntArray> {

	public int[] ar;

	public IntArray() { }

	public IntArray(int n) {
		ar = new int[n];
	}

	public IntArray(int[] a) {
		ar = a;
	}

	@Override
		public void write(DataOutput out) throws IOException {
			out.writeInt(ar.length);
			for(int i = 0; i < ar.length; i++) {
				out.writeInt(ar[i]);
			}
		}

	@Override
		public void readFields(DataInput in) throws IOException {
			int n = in.readInt();
			ar = new int[n];
			for(int i = 0; i < ar.length; i++) {
				ar[i] = in.readInt();
			}
		}

	@Override
		public int hashCode() {
			return this.toString().hashCode();
		}

	@Override
		public boolean equals(Object o) {
			if (o instanceof IntArray) {
				IntArray ia = (IntArray) o;
				if(ia.ar.length == ar.length) {
					for(int i = 0; i < ar.length; i++) {
						if( ia.ar[i] != ar[i] ) {
							return false;
						}
					}
					return true;
				} else {
					return false;
				}
			}
			return false;
		}

	@Override
		public String toString() {
			String s = "";
			for (int i = 0; i < ar.length; i++) {
				if (i > 0) {
					s += "\t";
				}
				s += ar[i];
			}
			return s;
		}

	@Override
		public int compareTo(IntArray ia) {
			int cmp = compare(ar.length,ia.ar.length);
			for(int i = 0; i < ar.length; i++) {
				cmp = compare(ar[i],ia.ar[i]);
				if (cmp != 0) {
					return cmp;
				}
			}
			return 0;
		}

	public static int compare(int a, int b) {
		return (a < b ? -1 : (a == b ? 0 : 1));
	}

}

