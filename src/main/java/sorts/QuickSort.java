package sorts;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class QuickSort implements Sortable {
	public int[] in;

	@Override
	public int[] sort(int[] in) {
		this.in = in;
		quickSort(0, in.length - 1);
		return in;
	}

	@Override
	public int[] sortThreaded(int version, int[] in, int threads) throws Exception {
		switch (version) {
		case 0:
			QuickByFragmentsJoinAtTheFinal(in, threads);
		}
		return in;
	}

	@Override
	public String toString() {
		return "QuickSort";
	}

	public void quickSort(int low, int high) {
		if (low < high) {
			int pi = partition(in, low, high);
			quickSort(low, pi - 1);
			quickSort(pi + 1, high);
		}
	}

	int partition(int arr[], int low, int high) {
		int pivot = arr[high];
		int i = (low - 1);
		for (int j = low; j < high; j++) {
			if (arr[j] <= pivot) {
				i++;
				int temp = arr[i];
				arr[i] = arr[j];
				arr[j] = temp;
			}
		}

		int temp = arr[i + 1];
		arr[i + 1] = arr[high];
		arr[high] = temp;

		return i + 1;
	}

	public int[] QuickByFragmentsJoinAtTheFinal(int[] in, int threads) throws Exception {

		int[] buff = new int[in.length];

		class QuickSortRunnable extends HelperRunnable {
			CyclicBarrier barrier;
			private int s;
			private int e;
			private int id;
			private int join;
			private int[] in;
			public boolean joinb = true;
			QuickSortRunnable[] runners;
			QuickSort q;

			QuickSortRunnable(int[] in, int s, int end, CyclicBarrier barrier, int id, QuickSortRunnable[] runners) {
				this.s = s;
				this.in = in;
				this.e = end;
				this.barrier = barrier;
				this.id = id;
				this.join = 1;
				this.runners = runners;
				this.q = new QuickSort();
			}

			public void run() {
				int i;
				this.q.in = in;
				this.q.quickSort(s, e - 1);

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
		class QuickSortRunnableRunnableChecker implements Runnable {
			QuickSortRunnable[] runners;
			int start = 1;
			int join = 2;

			public QuickSortRunnableRunnableChecker(QuickSortRunnable[] runners) {
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

		QuickSortRunnable[] runners = new QuickSortRunnable[threads];
		QuickSortRunnableRunnableChecker checker = new QuickSortRunnableRunnableChecker(runners);
		CyclicBarrier barrier = new CyclicBarrier(threads, checker);
		int chunks = in.length / threads;
		int index = 0;
		int i;
		for (i = 0; i < threads - 1; i++) {
			runners[i] = new QuickSortRunnable(in, index, index + chunks, barrier, i, runners);
			index += chunks;
		}
		runners[i] = new QuickSortRunnable(in, index, in.length, barrier, i, runners);

		Thread[] results = new Thread[threads];
		for (i = 0; i < threads; i++) {
			results[i] = new Thread(runners[i], "QuickSortT-" + i);
			results[i].setDaemon(true);
			results[i].start();
		}
		for (i = 0; i < threads; i++) {
			results[i].join();
		}
		return in;
	}
}
