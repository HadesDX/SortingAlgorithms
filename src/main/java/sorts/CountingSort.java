package sorts;

public class CountingSort implements Sortable {

	@Override
	public int[] sort(int[] in) {
		int m = 1001;
		int[] mm = new int[m];
		int[] clone = in.clone();
		int i;
		int n = in.length;

		for (i = 0; i < n; i++) {
			mm[in[i]]++;
		}

		for (i = 1; i < m; i++)
			mm[i] += mm[i - 1];
		
		for (i = 0; i < n; i++) {
			in[mm[clone[i]] - 1] = clone[i];
			mm[clone[i]]--;
		}

		return in;
	}

	@Override
	public int[] sortThreaded(int version, int[] in, int threads) throws Exception {
		switch (version) {
		}
		return in;
	}

	@Override
	public String toString() {
		return "CountingSort";
	}

}
