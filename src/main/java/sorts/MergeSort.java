package sorts;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class MergeSort implements Sortable {

	@Override
	public int[] sort(int[] in) {
		topDownMergeSort(in, in.clone(), in.length);
		return in;
	}

	@Override
	public String toString() {
		return "MergeSort";
	}

	private void topDownMergeSort(int[] A, int[] B, int n) {
		copyArray(A, 0, n, B);
		topDownSplitMerge(B, 0, n, A);
	}

	private void topDownMergeSort(int[] A, int[] B, int s, int e) {
		copyArray(A, s, e, B);
		topDownSplitMerge(B, s, e, A);
	}

	private void topDownSplitMerge(int[] B, int iBegin, int iEnd, int[] A) {
		if (iEnd - iBegin < 2) {
			return;
		}
		int iMiddle = (iEnd + iBegin) / 2;
		topDownSplitMerge(A, iBegin, iMiddle, B);
		topDownSplitMerge(A, iMiddle, iEnd, B);
		topDownMerge(B, iBegin, iMiddle, iEnd, A);
	}

	private void topDownMerge(int[] A, int iBegin, int iMiddle, int iEnd, int[] B) {
		int i = iBegin, j = iMiddle;

		for (int k = iBegin; k < iEnd; ++k) {
			if (i < iMiddle && (j >= iEnd || A[i] <= A[j])) {
				B[k] = A[i];
				++i;
			} else {
				B[k] = A[j];
				++j;
			}
		}
	}

	private void copyArray(int A[], int iBegin, int iEnd, int B[]) {
		System.arraycopy(A, iBegin, B, iBegin, iEnd - iBegin);
	}

	@Override
	public int[] sortThreaded(int version, int[] in, int threads) throws Exception {
		switch (version) {
		case 0:
			MergeByFragmentsJoinAtTheFinal(in, threads);
			break;
		}
		return in;
	}

	public int[] MergeByFragmentsJoinAtTheFinal(int[] in, int threads) throws Exception {

		int[] buff = new int[in.length];

		class MergeSortRunnable extends HelperRunnable {
			CyclicBarrier barrier;
			private int s;
			private int e;
			private int id;
			private int join;
			private int[] in;
			public boolean joinb = true;
			MergeSortRunnable[] runners;
			MergeSort q;

			MergeSortRunnable(int[] in, int s, int end, CyclicBarrier barrier, int id, MergeSortRunnable[] runners) {
				this.s = s;
				this.in = in;
				this.e = end;
				this.barrier = barrier;
				this.id = id;
				this.join = 1;
				this.runners = runners;
				this.q = new MergeSort();
			}

			public void run() {
				int i;
				this.q.topDownMergeSort(in, buff, s, e);

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
		class MergeSortRunnableRunnableChecker implements Runnable {
			MergeSortRunnable[] runners;
			int start = 1;
			int join = 2;

			public MergeSortRunnableRunnableChecker(MergeSortRunnable[] runners) {
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

		MergeSortRunnable[] runners = new MergeSortRunnable[threads];
		MergeSortRunnableRunnableChecker checker = new MergeSortRunnableRunnableChecker(runners);
		CyclicBarrier barrier = new CyclicBarrier(threads, checker);
		int chunks = in.length / threads;
		int index = 0;
		int i;
		for (i = 0; i < threads - 1; i++) {
			runners[i] = new MergeSortRunnable(in, index, index + chunks, barrier, i, runners);
			index += chunks;
		}
		runners[i] = new MergeSortRunnable(in, index, in.length, barrier, i, runners);

		Thread[] results = new Thread[threads];
		for (i = 0; i < threads; i++) {
			results[i] = new Thread(runners[i], "MergeSortT-" + i);
			results[i].setDaemon(true);
			results[i].start();
		}
		for (i = 0; i < threads; i++) {
			results[i].join();
		}
		return in;
	}
}
