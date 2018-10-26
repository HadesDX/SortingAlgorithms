package sorts;

public class QuickSort implements Sortable {

	@Override
	public int[] sort(int[] in) {
		quickSort(in, 0, in.length - 1);
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
		return "QuickSort";
	}

	private void quickSort(int[] in, int low, int high) {
		if (low < high) {
			int pi = partition(in, low, high);
			quickSort(in, low, pi - 1);
			quickSort(in, pi + 1, high);
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
}
