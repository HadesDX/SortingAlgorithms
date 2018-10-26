package sorts;

public class BitonicSort implements Sortable {
	int[] in;
	private boolean ASCENDING = true;

	@Override
	public int[] sort(int[] in) {
		this.in = in;
		bitonicSort(0, in.length, ASCENDING);
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
		return "BitonicSort";
	}

	private void bitonicSort(int lo, int n, boolean up) {
		if (n > 1) {
			int m = n / 2;
			bitonicSort(lo, m, !up);
			bitonicSort(lo + m, n - m, up);
			bitonicMerge(lo, n, up);
		}
	}

	private void bitonicMerge(int lo, int n, boolean up) {
		if (n > 1) {
			int m = greatestPowerOfTwoLessThan(n);
			for (int i = lo; i < lo + n - m; i++)
				compare(i, i + m, up);
			bitonicMerge(lo, m, up);
			bitonicMerge(lo + m, n - m, up);
		}
	}

	private void compare(int i, int j, boolean dir) {
		if (dir == (in[i] > in[j]))
			exchange(i, j);
	}

	private void exchange(int i, int j) {
		int t = in[i];
		in[i] = in[j];
		in[j] = t;
	}

	// n>=2 and n<=Integer.MAX_VALUE
	private int greatestPowerOfTwoLessThan(int n) {
		int k = 1;
		while (k > 0 && k < n)
			k = k << 1;
		return k >>> 1;
	}

}
