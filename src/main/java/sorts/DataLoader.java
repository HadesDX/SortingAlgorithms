package sorts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public class DataLoader {
	private DataLoader() {

	}

	public static int[] load(String fileName) {
		int[] out = null;
		File f = new File(fileName);
		if (f.exists() && f.canRead()) {
			try (FileInputStream fi = new FileInputStream(f);
					InputStreamReader is = new InputStreamReader(fi);
					BufferedReader br = new BufferedReader(is)) {
				int s = Integer.parseInt(br.readLine());
				out = new int[s];
				for (int i = 0; i < s; ++i) {
					out[i] = Integer.parseInt(br.readLine());
				}
			} catch (Exception ex) {
			}
		}

		return out;
	}
}
