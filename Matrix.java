import java.lang.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.text.*;

public interface Matrix {
	public float get(int i, int j);
	public void set(int i, int j, float value);
	public void reset();
}
