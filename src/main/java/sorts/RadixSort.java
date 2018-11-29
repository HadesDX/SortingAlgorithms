package sorts;

public class RadixSort implements Sortable {

	@Override
	public int[] sort(int[] in) {

		// int n = in.length;
		// int m = getMax(in, n);
		// for (int exp = 1; m / exp > 0; exp *= 10) {
		// System.out.println("exp: " + exp);
		// countSort(in, n, exp);
		// }
		// return in;

		int w = 32;
		int d = 8;
		int[] a = in;
		int[] b = null;
		for (int p = 0; p < w / d; p++) {
			int c[] = new int[1 << d];
			b = new int[a.length];
			for (int i = 0; i < a.length; i++)
				c[(a[i] >> d * p) & ((1 << d) - 1)]++;
			for (int i = 1; i < 1 << d; i++)
				c[i] += c[i - 1];
			for (int i = a.length - 1; i >= 0; i--)
				b[--c[(a[i] >> d * p) & ((1 << d) - 1)]] = a[i];
			a = b;
		}
		return b;

	}

	@Override
	public int[] sortThreaded(int version, int[] in, int threads) throws Exception {
		switch (version) {
		}
		return in;
	}

	@Override
	public String toString() {
		return "RadixSort";
	}

	private int getMax(int arr[], int n) {
		int mx = arr[0];
		for (int i = 1; i < n; i++)
			if (arr[i] > mx)
				mx = arr[i];
		return mx;
	}

	private void countSort(int in[], int n, int exp) {
		int output[] = new int[n];
		int i;
		int count[] = new int[10];

		for (i = 0; i < n; i++) {
			count[(in[i] / exp) % 10]++;
		}
		for (i = 1; i < 10; i++) {
			count[i] += count[i - 1];
		}
		for (i = n - 1; i >= 0; i--) {
			output[count[(in[i] / exp) % 10] - 1] = in[i];
			count[(in[i] / exp) % 10]--;
		}
		System.arraycopy(output, 0, in, 0, n);
	}
}
