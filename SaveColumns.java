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

