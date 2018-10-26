package sorts;

public class MergeSort implements Sortable {

	@Override
	public int[] sort(int[] in) {
		topDownMergeSort(in, in.clone(), in.length);
		return in;
	}

	@Override
	public int[] sortThreaded(int version, int[] in, int threads) throws Exception {
		switch (version) {
		}
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
}
