package sorts;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class BitonicSort implements Sortable {
	public int[] in;
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
		case 0:
			return BitonicByFragmentsJoinAtTheFinal(in, threads);
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
			// System.out.println(
			// "lo:" + lo + " n:" + n + " m:" + m + " up:" + up + " lo+m:" + (lo + m) + "
			// n-m:" + (n - m));
			bitonicSort(lo, m, !up);
			bitonicSort(lo + m, n - m, up);
			bitonicMerge(lo, n, up);
		}
	}

	private void bitonicMerge(int lo, int n, boolean up) {
		if (n > 1) {
			int m = greatestPowerOfTwoLessThan(n);
			for (int i = lo; i < lo + n - m; i++) {
				compare(i, i + m, up);
			}
			bitonicMerge(lo, m, up);
			bitonicMerge(lo + m, n - m, up);
		}
	}

	private void compare(int i, int j, boolean dir) {
		if (dir == (in[i] > in[j])) {
			exchange(i, j);
		}
	}

	private void exchange(int i, int j) {
		int t = in[i];
		in[i] = in[j];
		in[j] = t;
	}

	// n>=2 and n<=Integer.MAX_VALUE
	private int greatestPowerOfTwoLessThan(int n) {
		int k = 1;
		while (k > 0 && k < n) {
			k = k << 1;
		}
		return k >>> 1;
	}

	public int[] BitonicByFragmentsJoinAtTheFinal(int[] in, int threads) throws Exception {

		int[] buff = new int[in.length];

		class BitonicSortRunnable extends HelperRunnable {
			CyclicBarrier barrier;
			private int s;
			private int e;
			private int id;
			private int join;
			private int[] in;
			private int[] result;
			public boolean joinb = true;
			BitonicSortRunnable[] runners;
			BitonicSort q;

			BitonicSortRunnable(int[] in, int s, int end, CyclicBarrier barrier, int id,
					BitonicSortRunnable[] runners) {
				this.s = s;
				this.in = in;
				this.e = end;
				this.barrier = barrier;
				this.id = id;
				this.join = 1;
				this.runners = runners;
				this.q = new BitonicSort();
				this.q.in = in;
				this.result = new int[e - s];
			}

			public void run() {
				int i, j;
				//System.out.println("Start: " + s + " End: " + e);
				for (i = s, j = 0; i < e; ++i, ++j) { //Copia local
					result[j] = in[i];
				}
				this.q.sort(result);
				for (i = s, j = 0; i < e; ++i, ++j) { // devolver al buffer
					in[i] = result[j];
				}
				try {
					barrier.await();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (BrokenBarrierException e) {
					e.printStackTrace();
				}

				// Join
				while (join < threads) {
					if (!joinb || id + join > threads - 1) {

					} else {
						merge(in, buff, s, e, runners[id + join].s, runners[id + join].e);
						for (i = s; i < runners[id + join].e; ++i) {
							in[i] = buff[i];
						}
						e = runners[id + join].e;
					}
					join <<= 1;
					try {
						barrier.await();
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (BrokenBarrierException e) {
						e.printStackTrace();
					}
				}

			}
		}
		class BitonicSortRunnableRunnableChecker implements Runnable {
			BitonicSortRunnable[] runners;
			int start = 1;
			int join = 2;

			public BitonicSortRunnableRunnableChecker(BitonicSortRunnable[] runners) {
				this.runners = runners;
			}

			@Override
			public void run() {
				for (int i = start; i < threads; i += join) {
					runners[i].joinb = false;
				}
				start <<= 1;
				join <<= 1;
			}

		}

		BitonicSortRunnable[] runners = new BitonicSortRunnable[threads];
		BitonicSortRunnableRunnableChecker checker = new BitonicSortRunnableRunnableChecker(runners);
		CyclicBarrier barrier = new CyclicBarrier(threads, checker);
		int chunks = in.length / threads;
		int index = 0;
		int i;
		for (i = 0; i < threads - 1; i++) {
			runners[i] = new BitonicSortRunnable(in, index, index + chunks, barrier, i, runners);
			index += chunks;
		}
		runners[i] = new BitonicSortRunnable(in, index, in.length, barrier, i, runners);

		Thread[] results = new Thread[threads];
		for (i = 0; i < threads; i++) {
			results[i] = new Thread(runners[i], "BitonicSortT-" + i);
			results[i].setDaemon(true);
			results[i].start();
		}
		for (i = 0; i < threads; i++) {
			results[i].join();
		}
		return in;
	}

}
