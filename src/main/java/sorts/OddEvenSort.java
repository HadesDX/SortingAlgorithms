package sorts;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class OddEvenSort implements Sortable {

	@Override
	public int[] sort(int[] in) {
		boolean swaps = true;
		int aux;
		int i;
		while (swaps) {
			swaps = false;
			for (i = 0; i < in.length - 1; i += 2) {
				if (in[i] > in[i + 1]) {
					aux = in[i];
					in[i] = in[i + 1];
					in[i + 1] = aux;
					swaps = true;
				}
			}
			for (i = 1; i < in.length - 1; i += 2) {
				if (in[i] > in[i + 1]) {
					aux = in[i];
					in[i] = in[i + 1];
					in[i + 1] = aux;
					swaps = true;
				}
			}
		}
		return in;
	}

	@Override
	public int[] sortThreaded(int version, int[] in, int threads) throws Exception {
		switch (version) {
		case 0:
			return sortThreaded0(in, threads);
		case 1:
			return sortThreaded1(in, threads);
		}
		return null;
	}

	public int[] sortThreaded0(int[] in, int threads) throws Exception {
		class OddEvenSortRunnable implements Runnable {
			private int s;
			private int e;
			private int step;
			private int[] in;
			public boolean swaps;

			OddEvenSortRunnable(int[] in, int s, int step) {
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

		OddEvenSortRunnable[] runnersOdd = new OddEvenSortRunnable[threads];
		OddEvenSortRunnable[] runnersEven = new OddEvenSortRunnable[threads];

		int i;
		for (i = 0; i < threads; i++) {
			// System.out.println(i + "-" + (i * 2) + "-" + (i * 2 + 1));
			runnersOdd[i] = new OddEvenSortRunnable(in, i * 2, threads);
			runnersEven[i] = new OddEvenSortRunnable(in, i * 2 + 1, threads);
		}

		Future<?>[] results = new Future<?>[threads];

		ExecutorService executor = Executors.newFixedThreadPool(threads);
		boolean swaps = true;
		while (swaps) {
			// System.out.println("Loop " + j);
			for (i = 0; i < threads; i++) {
				results[i] = executor.submit(runnersOdd[i]);
			}
			swaps = false;
			for (i = 0; i < threads; i++) {
				results[i].get();
				swaps = swaps | runnersOdd[i].swaps;
			}
			for (i = 0; i < threads; i++) {
				results[i] = executor.submit(runnersEven[i]);
			}
			for (i = 0; i < threads; i++) {
				results[i].get();
				swaps = swaps | runnersEven[i].swaps;
			}
			// System.out.println("Loop End " + j);
		}
		executor.shutdown();
		// System.out.println("Finallizing");

		return in;
	}

	@Override
	public String toString() {
		return "OddEvenSort";
	}

	public int[] sortThreaded1(int[] in, int threads) throws Exception {
		class OddEvenSortRunnable implements Runnable {
			private int s;
			private int e;
			private int step;
			private int[] in;
			public boolean swaps;

			OddEvenSortRunnable(int[] in, int s, int step) {
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

		OddEvenSortRunnable[] runners = new OddEvenSortRunnable[threads];

		int i;
		for (i = 0; i < threads; i++) {
			// System.out.println(i + "-" + (i * 2) + "-" + (i * 2 + 1));
			runners[i] = new OddEvenSortRunnable(in, i * 2, threads);
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
