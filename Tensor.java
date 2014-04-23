import java.lang.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.text.*;

public interface Tensor extends Matrix {
	public float get(int i, int j, int k);
	public void set(int i, int j, int k, float value);
}
