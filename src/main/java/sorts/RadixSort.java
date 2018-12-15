package sorts;

public class RadixSort implements Sortable {

	@Override
	public int[] sort(int[] in) {
		/*
		 * int w = 32; int d = 8; int[] a = in; int[] b = null; for (int p = 0; p < w /
		 * d; p++) { int c[] = new int[1 << d]; b = new int[a.length]; for (int i = 0; i
		 * < a.length; i++) c[(a[i] >> d * p) & ((1 << d) - 1)]++; for (int i = 1; i < 1
		 * << d; i++) c[i] += c[i - 1]; for (int i = a.length - 1; i >= 0; i--)
		 * b[--c[(a[i] >> d * p) & ((1 << d) - 1)]] = a[i]; a = b; } return b;
		 */
		ParaSortTS.ARLsort(in);
		return in;
	}

	@Override
	public int[] sortThreaded(int version, int[] in, int threads) throws Exception {
		switch (version) {
		case 0:
			/**
			 * Call Arne Maus parallel solution
			 */
			ParaSortTS p = new ParaSortTS(threads);
			p.paraARL(in);
			p.endPool();
			break;
		}
		return in;
	}

	@Override
	public String toString() {
		return "RadixSort";
	}
}
