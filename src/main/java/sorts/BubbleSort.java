package sorts;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class BubbleSort implements Sortable {

	@Override
	public int[] sort(int[] in) {
		boolean swaps = true;
		int aux;
		int i, n = in.length;
		while (swaps) {
			swaps = false;
			for (i = 0; i < n - 1; ++i) {
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
	public String toString() {
		return "BubbleSort";
	}

	@Override
	public int[] sortThreaded(int version, int[] in, int threads) throws Exception {
		switch (version) {
		case 0:
			return bubleByFragmentsJoinAtTheFinal(in, threads);
		case 1:
			return sortThreaded1(in, threads);
		}
		return null;
	}

	public int[] sortThreaded0(int[] in, int threads) throws Exception {
		class BubbleSortRunnable implements Runnable {
			private final int s;
			private final int e;
			private int[] in;
			public boolean swaps;

			BubbleSortRunnable(int[] in, int start, int end) {
				s = start;
				e = end;
				this.in = in;
			}

			public void run() {
				swaps = false;
				// System.out.println(s + "-" + e);
				int aux;
				for (int i = s; i < e; i++) {
					if (in[i] > in[i + 1]) {
						aux = in[i];
						in[i] = in[i + 1];
						in[i + 1] = aux;
						swaps = true;
					}
				}
			}
		}

		BubbleSortRunnable[] runners = new BubbleSortRunnable[threads];
		int chunks = (in.length - 1) / threads;
		int index = 0;
		int i;
		for (i = 0; i < threads - 1; i++) {
			runners[i] = new BubbleSortRunnable(in, index, index + chunks);
			index += chunks;
		}
		runners[i] = new BubbleSortRunnable(in, index, in.length - 1);
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

	public int[] sortThreaded1(int[] in, int threads) throws Exception {
		final Object[] lock = new Object[threads];
		for (int i = 0; i < threads; ++i) {
			lock[i] = new Object();
		}

		class BubbleSortRunnable implements Runnable {
			private int id;
			private int s;
			private int e;
			private int[] in;
			public volatile boolean swaps;
			public BubbleSortRunnable[] siblings;

			BubbleSortRunnable(int id, int[] in, BubbleSortRunnable[] siblings, int start, int end) {
				this.id = id;
				this.s = start;
				this.e = id == siblings.length - 1 ? end - 1 : end;
				this.in = in;
				this.siblings = siblings;
				swaps = true;
			}

			public void run() {
				// System.out.println("Range:" + s + "-" + e);
				while (swaps) {
					swaps = false;
					int i = s;
					if (id > 0) {
						synchronized (lock[id - 1]) {
							int aux;
							if (in[i] > in[i + 1]) {
								aux = in[i];
								in[i] = in[i + 1];
								in[i + 1] = aux;
								swaps = true;
							}
						}
					}
					for (; i < e; i++) {
						int aux;
						if (in[i] > in[i + 1]) {
							aux = in[i];
							in[i] = in[i + 1];
							in[i + 1] = aux;
							swaps = true;
						}
					}
					synchronized (lock[id]) {
						int aux;
						if (in[i] > in[i + 1]) {
							aux = in[i];
							in[i] = in[i + 1];
							in[i + 1] = aux;
							if (id < siblings.length - 1) {
								siblings[id + 1].swaps = true;
							}
						}
					}
				}
			}
		}

		BubbleSortRunnable[] runners = new BubbleSortRunnable[threads];
		int chunks = (in.length - 1) / threads;
		int index = 0;
		int i;
		for (i = 0; i < threads - 1; i++) {
			runners[i] = new BubbleSortRunnable(i, in, runners, index, index + chunks);
			index += chunks;
		}
		runners[i] = new BubbleSortRunnable(i, in, runners, index, in.length - 1);
		Future<?>[] results = new Future<?>[threads];

		ExecutorService executor = Executors.newFixedThreadPool(threads);
		boolean swaps = true;
		while (swaps) {
			// System.out.println("in " + Arrays.toString(in));
			// Thread.sleep(1000);
			for (i = 0; i < threads; i++) {
				results[i] = executor.submit(runners[i]);
			}
			for (i = 0; i < threads; i++) {
				results[i].get();
			}
			swaps = false;
			for (i = 0; i < threads; i++) {
				swaps = swaps | runners[i].swaps;
			}
			if (!swaps) {
				for (i = 0; i < in.length - 1; ++i) {
					if (in[i] > in[i + 1]) {
						swaps = true;
						for (i = 0; i < threads; ++i) {
							runners[i].swaps = true;
						}
						break;

					}
				}
			}
			// System.out.println("Loop End " + j);
		}
		executor.shutdown();
		// System.out.println("Finallizing");

		return in;
	}

	public int[] bubleByFragmentsJoinAtTheFinal(int[] in, int threads) throws Exception {

		int[] buff = new int[in.length];

		class BubbleSortRunnable extends HelperRunnable {
			CyclicBarrier barrier;
			private int s;
			private int e;
			private int id;
			private int join;
			private int[] in;
			public boolean joinb = true;
			BubbleSortRunnable[] runners;

			BubbleSortRunnable(int[] in, int s, int end, CyclicBarrier barrier, int id, BubbleSortRunnable[] runners) {
				this.s = s;
				this.in = in;
				this.e = end;
				this.barrier = barrier;
				this.id = id;
				this.join = 1;
				this.runners = runners;
			}

			public void run() {
				boolean swaps = true;
				int aux;
				int i;
				while (swaps) {
					swaps = false;
					for (i = s; i < e - 1; ++i) {
						if (in[i] > in[i + 1]) {
							aux = in[i];
							in[i] = in[i + 1];
							in[i + 1] = aux;
							swaps = true;
						}
					}
				}
				// System.out.println("size:" + result.length + " - " +
				// Arrays.toString(result));

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
		class BubbleSortRunnableChecker implements Runnable {
			BubbleSortRunnable[] runners;
			int start = 1;
			int join = 2;

			public BubbleSortRunnableChecker(BubbleSortRunnable[] runners) {
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

		BubbleSortRunnable[] runners = new BubbleSortRunnable[threads];
		BubbleSortRunnableChecker checker = new BubbleSortRunnableChecker(runners);
		CyclicBarrier barrier = new CyclicBarrier(threads, checker);
		int chunks = in.length / threads;
		int index = 0;
		int i;
		for (i = 0; i < threads - 1; i++) {
			runners[i] = new BubbleSortRunnable(in, index, index + chunks, barrier, i, runners);
			index += chunks;
		}
		runners[i] = new BubbleSortRunnable(in, index, in.length, barrier, i, runners);

		Thread[] results = new Thread[threads];
		for (i = 0; i < threads; i++) {
			results[i] = new Thread(runners[i], "BubbleSortT-" + i);
			results[i].setDaemon(true);
			results[i].start();
		}
		for (i = 0; i < threads; i++) {
			results[i].join();
		}

		return in;
	}
}
