package sorts;

public abstract class HelperRunnable implements Runnable {
	public void merge(int[] a, int[] b, int[] out) {
		int i, j, k, m, n;
		i = 0;
		j = 0;
		k = 0;
		m = a.length;
		n = b.length;

		while (i < m && j < n) {
			if (a[i] <= b[j]) {
				out[k] = a[i];
				i++;
			} else {
				out[k] = b[j];
				j++;
			}
			k++;
		}

		if (i < m) {
			for (int p = i; p < m; p++) {
				out[k] = a[p];
				k++;
			}
		} else {
			for (int p = j; p < n; p++) {
				out[k] = b[p];
				k++;
			}
		}
	}

	public void merge(int[] in, int[] out, int s1, int e1, int s2, int e2) {
		int i, j, k, m, n;
		i = 0;
		j = 0;
		k = s1;
		m = e1 - s1;
		n = e2 - s2;

		while (i < m && j < n) {
			if (in[s1 + i] <= in[s2 + j]) {
				out[k] = in[s1 + i];
				i++;
			} else {
				out[k] = in[s2 + j];
				j++;
			}
			k++;
		}

		if (i < m) {
			for (int p = i; p < m; p++) {
				out[k] = in[s1 + p];
				k++;
			}
		} else {
			for (int p = j; p < n; p++) {
				out[k] = in[s2 + p];
				k++;
			}
		}
	}
}
