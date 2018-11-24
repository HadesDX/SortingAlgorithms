package sorts;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class RankSort implements Sortable {

	@Override
	public int[] sort(int[] in) {
		int i, j, k, n = in.length;
		int[] out = new int[n];
		int rank = 0;

		for (i = 0; i < n; ++i) {
			rank = 0;
			for (j = 0; j < n; ++j) {
				if (in[j] < in[i]) {
					++rank;
				}
			}
			out[rank] = in[i];
		}
		for (k = 0; k < n; ++k) {
			if (out[k] == 0) {
				if (k != 0) {
					in[k] = in[k - 1];
				} else {
					in[k] = 0;
				}
			} else {
				in[k] = out[k];
			}
		}
		return in;
	}

	@Override
	public String toString() {
		return "RankSort";
	}

	@Override
	public int[] sortThreaded(int version, int[] in, int threads) throws Exception {
		switch (version) {
		case 0:
			return sortThreaded0(in, threads);
		}
		return null;
	}

	public int[] sortThreaded0(int[] in, int threads) throws Exception {
		AtomicIntegerArray array = new AtomicIntegerArray(in.length);

		class RankSortRunnable implements Runnable {
			private int s;
			private int e;
			private int step;
			private int[] in;
			public boolean swaps;

			RankSortRunnable(int[] in, int s, int step) {
				this.s = s;
				this.in = in;
				this.e = in.length - 1;
				this.step = step;
			}

			public void run() {
				swaps = false;
				int aux;
				for (int i = s; i < e; i += step) {
					if (in[i] > in[i + 1]) {
						aux = in[i];
						in[i] = in[i + 1];
						in[i + 1] = aux;
						swaps = true;
					}
				}
			}
		}

		RankSortRunnable[] runners = new RankSortRunnable[threads];

		int i;
		for (i = 0; i < threads; i++) {
			runners[i] = new RankSortRunnable(in, i * 2, threads);
		}

		Future<?>[] results = new Future<?>[threads];

		ExecutorService executor = Executors.newFixedThreadPool(threads);
		boolean swaps = true;
		while (swaps) {
			// System.out.println("Loop " + j);
			for (i = 0; i < threads; i++) {
				results[i] = executor.submit(runners[i]);
			}
			swaps = false;
			for (i = 0; i < threads; i++) {
				results[i].get();
				swaps = swaps | runners[i].swaps;
			}
			// System.out.println("Loop End " + j);
		}
		executor.shutdown();
		// System.out.println("Finallizing");

		return in;
	}

}
