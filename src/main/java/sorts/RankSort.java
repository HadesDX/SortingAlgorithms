package sorts;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

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
			return rankByFragmentsJoinAtTheFinal(in, threads);
		}
		return null;
	}

	public int[] rankByFragmentsJoinAtTheFinal(int[] in, int threads) throws Exception {

		class RankSortRunnable extends HelperRunnable {
			CyclicBarrier barrier;
			private int s;
			private int e;
			private int n;
			private int id;
			private int join;
			private int[] in;
			private int[] result;
			public boolean joinb = true;
			RankSortRunnable[] runners;

			RankSortRunnable(int[] in, int s, int end, CyclicBarrier barrier, int id, RankSortRunnable[] runners) {
				this.s = s;
				this.in = in;
				this.e = end;
				this.n = e - s;
				this.barrier = barrier;
				this.result = new int[n];
				this.id = id;
				this.join = 1;
				this.runners = runners;
			}

			public void run() {
				int i, j, k;
				int[] out = new int[n];
				int rank;

				for (i = s; i < e; ++i) {
					rank = 0;
					for (j = s; j < e; ++j) {
						if (in[j] < in[i]) {
							++rank;
						}
					}
					out[rank] = in[i];
				}
				for (k = 0; k < n; ++k) {
					if (out[k] == 0) {
						if (k != 0) {
							result[k] = result[k - 1];
						} else {
							result[k] = 0;
						}
					} else {
						result[k] = out[k];
					}
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
						int[] r = new int[result.length + runners[id + join].result.length];
						merge(result, runners[id + join].result, r);
						result = r;
						runners[id + join].result = null;
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
		class RankSortRunnableChecker implements Runnable {
			RankSortRunnable[] runners;
			int start = 1;
			int join = 2;

			public RankSortRunnableChecker(RankSortRunnable[] runners) {
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

		RankSortRunnable[] runners = new RankSortRunnable[threads];
		RankSortRunnableChecker checker = new RankSortRunnableChecker(runners);
		CyclicBarrier barrier = new CyclicBarrier(threads, checker);
		int chunks = in.length / threads;
		int index = 0;
		int i;
		for (i = 0; i < threads - 1; i++) {
			runners[i] = new RankSortRunnable(in, index, index + chunks, barrier, i, runners);
			index += chunks;
		}
		runners[i] = new RankSortRunnable(in, index, in.length, barrier, i, runners);

		Thread[] results = new Thread[threads];
		for (i = 0; i < threads; i++) {
			results[i] = new Thread(runners[i], "RankSortT-" + i);
			results[i].setDaemon(true);
			results[i].start();
		}
		for (i = 0; i < threads; i++) {
			results[i].join();
		}
		return runners[0].result;
	}

}
