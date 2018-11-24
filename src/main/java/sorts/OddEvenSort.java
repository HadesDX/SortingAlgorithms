package sorts;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
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
			return cyclicBarrier(in, threads);
		case 1:
			return sortThreaded1(in, threads);
		case 2:
			return sortThreaded0(in, threads);
		}

		return null;
	}

	@Override
	public String toString() {
		return "OddEvenSort";
	}

	static volatile boolean work = true;
	public boolean globalSwaps = false;
	public boolean even = true;

	public int[] cyclicBarrier(int[] in, int threads) throws Exception {
		// System.out.println(" @ " + this + " cyclicBarrier");
		work = true;
		class OddEvenSortRunnableCB implements Runnable {
			private int s;
			private int sAlt;
			private int e;
			private int step;
			private int[] in;
			public boolean swaps;
			public CyclicBarrier barrier;

			OddEvenSortRunnableCB(int[] in, int s, int step, CyclicBarrier barrier) {
				this.s = s;
				this.sAlt = s + 1;
				this.in = in;
				this.e = in.length - 1;
				this.step = step;
				this.barrier = barrier;
			}

			public void run() {
				// System.out.println(" S:" + s + " E:" + e);
				// System.out.println(" Do work " + s / 2);
				//try (AffinityLock al = AffinityLock.acquireCore()) {
					int aux;
					while (work) {
						swaps = false;
						// System.out.println("Do work " + s / 2);
						if (even) {
							for (int i = s; i < e; i += step) {
								// System.out.println(step / 2 + " IT:" + i + " ?:" + (in[i] > in[i + 1]) + "
								// Swap:" + swaps);
								if (in[i] > in[i + 1]) {
									aux = in[i];
									in[i] = in[i + 1];
									in[i + 1] = aux;
									swaps = true;
								}
							}
						} else {
							for (int i = sAlt; i < e; i += step) {
								// System.out.println(step / 2 + " IT:" + i + " ?:" + (in[i] > in[i + 1]) + "
								// Swap:" + swaps);
								if (in[i] > in[i + 1]) {
									aux = in[i];
									in[i] = in[i + 1];
									in[i + 1] = aux;
									swaps = true;
								}
							}
						}

						try {
							barrier.await();
						} catch (InterruptedException e) {
							e.printStackTrace();
						} catch (BrokenBarrierException e) {
							e.printStackTrace();
						}
					}
				//} catch (Exception e) {
				//	e.printStackTrace();
				//}

			}
		}

		class OddEvenSortChecker implements Runnable {
			public OddEvenSortRunnableCB[] runners;

			public OddEvenSortChecker(OddEvenSortRunnableCB[] runners) {
				this.runners = runners;
			}

			@Override
			public void run() {
				// try (AffinityLock al = AffinityLock.acquireCore()) {
				// System.out.println("CHEKING EVEN:" + even);
				int i;
				if (even) {
					globalSwaps = false;
				}
				// System.out.println("Thread check:" + threads);
				for (i = 0; i < threads; i++) {
					// System.out.println("Runner:" + runners[i].swaps);
					globalSwaps = globalSwaps | runners[i].swaps;
				}
				if (!even) {
					if (!globalSwaps) {
						work = false;
						// System.out.println("SORT FINISH:" + System.currentTimeMillis());
					}
				}
				// System.out.println(Arrays.toString(in));
				even = !even;
				// } catch (Exception e) {
				// e.printStackTrace();
				// }
			}
		}

		OddEvenSortRunnableCB[] runners = new OddEvenSortRunnableCB[threads];
		OddEvenSortChecker checker = new OddEvenSortChecker(runners);
		CyclicBarrier barrier = new CyclicBarrier(threads, checker);
		work = true;
		even = true;
		int i;
		for (i = 0; i < threads; i++) {
			// System.out.println(i + "-" + (i * 2) + "-" + (i * 2 + 1));
			// ..................................data, start, step .., barrier
			runners[i] = new OddEvenSortRunnableCB(in, i * 2, threads * 2, barrier);
		}

		Thread[] results = new Thread[threads];
		for (i = 0; i < threads; i++) {
			results[i] = new Thread(runners[i], "OddEvenSortT-" + i);
			results[i].setDaemon(true);
			results[i].start();
		}
		for (i = 0; i < threads; i++) {
			results[i].join();
		}
		// System.out.println("MAIN SORT FINISH:" + System.currentTimeMillis());
		// System.out.println("Finallizing");
		return in;
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

	public int[] sortThreaded0(int[] in, int threads) throws Exception {
		System.out.println(this);
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
			// System.out.println("Loop ");
			for (i = 0; i < threads; i++) {
				results[i] = executor.submit(runners[i]);
			}
			swaps = false;
			for (i = 0; i < threads; i++) {
				results[i].get();
				swaps = swaps | runners[i].swaps;
			}
			// System.out.println("Loop End ");
		}
		executor.shutdown();
		System.out.println("Finallizing");

		return in;
	}

}
