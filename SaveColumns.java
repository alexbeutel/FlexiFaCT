import java.io.*;
import java.util.*;
import java.text.*;

public class SaveColumns {
	public static void main(String[] args) {

		for(int i = 0; i < args.length; i++){
			System.out.println(i + " : " + args[i]);
		}

		int rank = Integer.parseInt(args[0]);
		int N = Integer.parseInt(args[1]);
		int offset = Integer.parseInt(args[2]);
		String folder = args[3];

		try {
			System.out.println("Start");
			BufferedWriter[] cols = new BufferedWriter[rank];
			for(int j = 0; j < rank; j++) {
				cols[j] = new BufferedWriter(new FileWriter(folder + "/col-" + j + ".txt",true)); 
			}
			System.out.println("Files Opened");

			DataInputStream ds = new DataInputStream(System.in);
			for(int i = 0; i < N; i++) {
				for(int j = 0; j < rank; j++) {
					float val = ds.readFloat();
					cols[j].write( (i + offset * N) + "\t" + val + "\n");
				}
			}
			System.out.println("Data Read");

			for(int j = 0; j < rank; j++) {
				cols[j].close();
			}
			System.out.println("Done");
		} catch (IOException e) {
			System.err.println("ERROR");
			System.err.println(e.toString());
		}

	}
}

