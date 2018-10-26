package sorts;

public class RadixSort implements Sortable {

	@Override
	public int[] sort(int[] in) {
		int n = in.length;
		int m = getMax(in, n);
		for (int exp = 1; m / exp > 0; exp *= 10) {
			countSort(in, n, exp);
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
		return "RadixSort";
	}

	private int getMax(int arr[], int n) {
		int mx = arr[0];
		for (int i = 1; i < n; i++)
			if (arr[i] > mx)
				mx = arr[i];
		return mx;
	}

	private void countSort(int arr[], int n, int exp) {
		int output[] = new int[n];
		int i;
		int count[] = new int[10];

		for (i = 0; i < n; i++) {
			count[(arr[i] / exp) % 10]++;
		}
		for (i = 1; i < 10; i++) {
			count[i] += count[i - 1];
		}
		for (i = n - 1; i >= 0; i--) {
			output[count[(arr[i] / exp) % 10] - 1] = arr[i];
			count[(arr[i] / exp) % 10]--;
		}
		System.arraycopy(output, 0, arr, 0, n);
	}
}
