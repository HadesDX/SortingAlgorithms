package sorts;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

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

		// System.out.println("First val:" + mm[clone[0]] + " - ");
		for (i = 1; i < m; i++) {
			mm[i] += mm[i - 1];
		}
		// System.out.println(Arrays.toString(mm));
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
			// return sortPerThreadData(in, threads);10% slower
			// return sortAtomicArray(in, threads); // 2xSlower thant the other one
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

	public int[] sortAtomicArray(int[] in, int threads) throws Exception {
		int m = 1001;
		int n = in.length;
		int[] mmA = new int[m];
		int[] clone = in.clone();
		AtomicInteger[] mm = new AtomicInteger[m];
		// AtomicIntegerArray mm = new AtomicIntegerArray(m);

		class CountingSortRunnableAA implements Runnable {

			private int s;
			private int e;
			private int[] in;
			public CyclicBarrier barrier;

			CountingSortRunnableAA(int[] in, int s, int e, CyclicBarrier barrier) {
				this.s = s;
				this.in = in;
				this.e = e;
				this.barrier = barrier;
			}

			@Override
			public void run() {
				// System.out.println("\n" + s + "-" + e);
				for (int i = s; i < e; i++) {
					// mm.incrementAndGet(in[i]);
					mm[in[i]].incrementAndGet();
				}
				try {
					barrier.await();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (BrokenBarrierException e) {
					e.printStackTrace();
				}
			}
		}

		class CountingSortRunnableAACheker implements Runnable {
			public CountingSortRunnableAA[] runners;

			public CountingSortRunnableAACheker(CountingSortRunnableAA[] runners) {
				this.runners = runners;
			}

			@Override
			public void run() {
				int i;

				for (i = 0; i < m; i++) {
					// mmA[i] = mm.get(i);
					mmA[i] = mm[i].get();
				}
				for (i = 1; i < m; i++) {
					mmA[i] += mmA[i - 1];
				}
				// for (i = 1; i < m; i++) {
				// mmA[i] = mm.set(i, mm.get(i) + mm.get(i - 1));
				// }

				for (i = 0; i < n; i++) {
					in[mmA[clone[i]] - 1] = clone[i];
					mmA[clone[i]]--;
				}
				// for (i = 0; i < n; i++) {
				// in[mm.get(clone[i]) - 1] = clone[i];
				// mm.decrementAndGet(clone[i]);
				// }
			}
		}

		CountingSortRunnableAA[] runners = new CountingSortRunnableAA[threads];
		CountingSortRunnableAACheker checker = new CountingSortRunnableAACheker(runners);
		CyclicBarrier barrier = new CyclicBarrier(threads, checker);

		int chunks = (in.length - 1) / threads;
		int index = 0;
		int i;
		for (i = 0; i < threads - 1; i++) {
			runners[i] = new CountingSortRunnableAA(in, index, index + chunks, barrier);
			index += chunks;
		}
		runners[i] = new CountingSortRunnableAA(in, index, in.length, barrier);
		for (i = 0; i < m; i++) {
			mm[i] = new AtomicInteger();
		}
		Thread[] results = new Thread[threads];
		for (i = 0; i < threads; i++) {
			results[i] = new Thread(runners[i], "CountingSortT-" + i);
			results[i].setDaemon(true);
			results[i].start();
		}
		for (i = 0; i < threads; i++) {
			results[i].join();
		}
		return in;
	}

	public int[] sortPerThreadData(int[] in, int threads) throws Exception {
		int m = 1001;
		int n = in.length;
		int[] clone = in.clone();

		class CountingSortRunnableT extends HelperRunnable {

			CyclicBarrier barrier;
			private int s;
			private int e;
			private int id;
			private int join;
			private int[] in;
			private int[] result;
			public boolean joinb = true;
			CountingSortRunnableT[] runners;

			CountingSortRunnableT(int[] in, int s, int end, CyclicBarrier barrier, int id,
					CountingSortRunnableT[] runners) {
				this.s = s;
				this.in = in;
				this.e = end;
				this.barrier = barrier;
				this.result = new int[m];
				this.id = id;
				this.join = 1;
				this.runners = runners;
			}

			@Override
			public void run() {
				// System.out.println("\n" + s + "-" + e);
				for (int i = s; i < e; i++) {
					++result[in[i]];
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
						
						for (int i = 0; i < m; ++i) {
							result[i] += runners[id + join].result[i];
						}
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

		class CountingSortRunnableTCheker implements Runnable {
			CountingSortRunnableT[] runners;
			int start = 1;
			int join = 2;

			public CountingSortRunnableTCheker(CountingSortRunnableT[] runners) {
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

		CountingSortRunnableT[] runners = new CountingSortRunnableT[threads];
		CountingSortRunnableTCheker checker = new CountingSortRunnableTCheker(runners);
		CyclicBarrier barrier = new CyclicBarrier(threads, checker);

		int chunks = in.length / threads;
		int index = 0;
		int i;
		for (i = 0; i < threads - 1; i++) {
			runners[i] = new CountingSortRunnableT(in, index, index + chunks, barrier, i, runners);
			index += chunks;
		}
		runners[i] = new CountingSortRunnableT(in, index, in.length, barrier, i, runners);

		Thread[] results = new Thread[threads];
		for (i = 0; i < threads; i++) {
			results[i] = new Thread(runners[i], "CountingSortT-" + i);
			results[i].setDaemon(true);
			results[i].start();
		}
		for (i = 0; i < threads; i++) {
			results[i].join();
		}

		for (i = 1; i < m; i++) {
			runners[0].result[i] += runners[0].result[i - 1];
		}

		for (i = 0; i < n; i++) {
			in[runners[0].result[clone[i]] - 1] = clone[i];
			runners[0].result[clone[i]]--;
		}

		return in;
	}
}
