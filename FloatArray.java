import java.io.*;
import org.apache.hadoop.io.*;

public class FloatArray implements WritableComparable<FloatArray> {

	public float[] ar;

	public FloatArray() { }

	public FloatArray(int n) {
		ar = new float[n];
	}

	public FloatArray(float[] a) {
		ar = a;
	}

	@Override
		public void write(DataOutput out) throws IOException {
			out.writeInt(ar.length);
			for(int i = 0; i < ar.length; i++) {
				out.writeFloat(ar[i]);
			}
		}

	@Override
		public void readFields(DataInput in) throws IOException {
			int n = in.readInt();
			ar = new float[n];
			for(int i = 0; i < ar.length; i++) {
				ar[i] = in.readFloat();
			}
		}

	@Override
		public int hashCode() {
			return this.toString().hashCode();
		}

	@Override
		public boolean equals(Object o) {
			if (o instanceof FloatArray) {
				FloatArray ia = (FloatArray) o;
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
		public int compareTo(FloatArray ia) {
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

	public static int compare(float a, float b) {
		return (a < b ? -1 : (a == b ? 0 : 1));
	}

}

