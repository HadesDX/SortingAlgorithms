package sorts;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class RadixSort implements Sortable {

	@Override
	public int[] sort(int[] in) {

		// int n = in.length;
		// int m = getMax(in, n);
		// for (int exp = 1; m / exp > 0; exp *= 10) {
		// System.out.println("exp: " + exp);
		// countSort(in, n, exp);
		// }
		// return in;

		int w = 32;
		int d = 8;
		int[] a = in;
		int[] b = null;
		for (int p = 0; p < w / d; p++) {
			int c[] = new int[1 << d];
			b = new int[a.length];
			for (int i = 0; i < a.length; i++)
				c[(a[i] >> d * p) & ((1 << d) - 1)]++;
			for (int i = 1; i < 1 << d; i++)
				c[i] += c[i - 1];
			for (int i = a.length - 1; i >= 0; i--)
				b[--c[(a[i] >> d * p) & ((1 << d) - 1)]] = a[i];
			a = b;
		}
		return b;
	}

	public int[] sort(int[] in, int s, int e, int d) {
		int w = 32;
		int[] a = in;
		int[] b = null;
		for (int p = 0; p < w / d; p++) {
			int c[] = new int[1 << d];
			b = new int[e - s];
			for (int i = s; i < e; i++)
				c[(a[i] >> d * p) & ((1 << d) - 1)]++;
			for (int i = 1; i < 1 << d; i++)
				c[i] += c[i - 1];
			for (int i = e - 1; i >= s; i--)
				b[--c[(a[i] >> d * p) & ((1 << d) - 1)]] = a[i];
			a = b;
		}
		return b;
	}

	@Override
	public int[] sortThreaded(int version, int[] in, int threads) throws Exception {
		switch (version) {
		case 0:
			partitionedParallelRadix(in, threads);
			break;
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

	private void countSort(int in[], int n, int exp) {
		int output[] = new int[n];
		int i;
		int count[] = new int[10];

		for (i = 0; i < n; i++) {
			count[(in[i] / exp) % 10]++;
		}
		for (i = 1; i < 10; i++) {
			count[i] += count[i - 1];
		}
		for (i = n - 1; i >= 0; i--) {
			output[count[(in[i] / exp) % 10] - 1] = in[i];
			count[(in[i] / exp) % 10]--;
		}
		System.arraycopy(output, 0, in, 0, n);
	}

	public int[] partitionedParallelRadix(int[] in, int threads) throws Exception {

		int[] localMax = new int[threads];
		int[] buff = new int[in.length];

		class RadixSortRunnable extends HelperRunnable {
			CyclicBarrier barrier;
			private int s;
			private int e;
			private int id;
			private int join;
			private int[] in;
			public boolean joinb = true;
			RadixSortRunnable[] runners;
			RadixSort q;

			RadixSortRunnable(int[] in, int s, int end, CyclicBarrier barrier, int id, RadixSortRunnable[] runners) {
				this.s = s;
				this.in = in;
				this.e = end;
				this.barrier = barrier;
				this.id = id;
				this.join = 1;
				this.runners = runners;
				this.q = new RadixSort();
			}

			public void run() {
				// this.q.sort(in, s, e, 8);

				/* FIND LOCAL MAX */
				int i;
				for (i = s; i < e; ++i) {
					if (in[i] > localMax[id]) {
						localMax[id] = in[i];
					}
				}
				System.out.println(id + " FINISH STEP 2 FIND LOCAL MAX: " + localMax[id]);
				try {
					barrier.await();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (BrokenBarrierException e) {
					e.printStackTrace();
				}
				if (true) {
					return;
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
		class RadixSortRunnableRunnableChecker implements Runnable {
			RadixSortRunnable[] runners;
			int start = 1;
			int join = 2;
			int algStep;
			public int globalMax;

			public RadixSortRunnableRunnableChecker(RadixSortRunnable[] runners) {
				this.runners = runners;
			}

			@Override
			public void run() {
				int i;
				switch (algStep) {
				case 0:
					++algStep;
					for (i = 0; i < threads; ++i) {
						if (localMax[i] > globalMax) {
							globalMax = localMax[i];
						}
					}
					System.out.println("\nGLOBAL MAX:" + globalMax);
					return;
				// break;
				case 1:

				default:
					break;
				}
				for (i = start; i < threads; i += join) {
					runners[i].joinb = false;
				}
				start <<= 1;
				join <<= 1;
			}

		}

		RadixSortRunnable[] runners = new RadixSortRunnable[threads];
		RadixSortRunnableRunnableChecker checker = new RadixSortRunnableRunnableChecker(runners);
		CyclicBarrier barrier = new CyclicBarrier(threads, checker);
		int chunks = in.length / threads;
		int index = 0;
		int i;
		for (i = 0; i < threads - 1; i++) {
			runners[i] = new RadixSortRunnable(in, index, index + chunks, barrier, i, runners);
			index += chunks;
		}
		runners[i] = new RadixSortRunnable(in, index, in.length, barrier, i, runners);

		Thread[] results = new Thread[threads];
		for (i = 0; i < threads; i++) {
			results[i] = new Thread(runners[i], "RadixSortT-" + i);
			results[i].setDaemon(true);
			results[i].start();
		}
		for (i = 0; i < threads; i++) {
			results[i].join();
		}
		return in;
	}
}
