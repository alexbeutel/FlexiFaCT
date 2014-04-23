import java.io.*;
import java.util.*;
import java.text.*;

public class FloatReader {
	public static void main(String[] args) {
		DataInputStream ds = new DataInputStream(System.in);
		while(true) {
			try {
			System.out.println(ds.readFloat());
			} catch (IOException e) {
				System.out.println(e.toString());
				break;
			}
		}
	}
}

