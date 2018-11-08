package sorts;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

		for (i = 1; i < m; i++) {
			mm[i] += mm[i - 1];
		}

		for (i = 0; i < n; i++) {
			in[mm[clone[i]] - 1] = clone[i];
			mm[clone[i]]--;
		}

		return in;
	}

	@Override
	public int[] sortThreaded(int version, int[] in, int threads) throws Exception {
		switch (version) {
		case 0:
			return sortThreaded0(in, threads);
		}
		return null;
	}

	@Override
	public String toString() {
		return "CountingSort";
	}

	public int[] sortThreaded0(int[] in, int threads) throws Exception {
		class CountingSortRunnable implements Runnable {
			private final int s;
			private final int e;
			private int[] in;
			public int[] mm;

			CountingSortRunnable(int[] in, int start, int end, int m) {
				mm = new int[m];
				s = start;
				e = end;
				this.in = in;
			}

			public void run() {
				for (int i = s; i < e; i++) {
					mm[in[i]]++;
				}
			}
		}
		int m = 1001;
		int n = in.length;
		int[] mm = new int[m];
		int[] clone = in.clone();

		CountingSortRunnable[] runners = new CountingSortRunnable[threads];
		int chunks = (in.length - 1) / threads;
		int index = 0;
		int i, j;
		for (i = 0; i < threads - 1; i++) {
			runners[i] = new CountingSortRunnable(in, index, index + chunks, m);
			index += chunks;
		}
		runners[i] = new CountingSortRunnable(in, index, in.length, m);
		Future<?>[] results = new Future<?>[threads];

		ExecutorService executor = Executors.newFixedThreadPool(threads);

		// System.out.println("Loop " + j);
		for (i = 0; i < threads; i++) {
			results[i] = executor.submit(runners[i]);
		}
		for (i = 0; i < threads; i++) {
			results[i].get();
			for (j = 0; j < m; j++) {
				mm[j] += runners[i].mm[j];
			}
		}

		for (i = 1; i < m; i++) {
			mm[i] += mm[i - 1];
		}

		for (i = 0; i < n; i++) {
			in[mm[clone[i]] - 1] = clone[i];
			mm[clone[i]]--;
		}

		// System.out.println("Loop End " + j);

		executor.shutdown();
		// System.out.println("Finallizing");

		return in;
	}
}
