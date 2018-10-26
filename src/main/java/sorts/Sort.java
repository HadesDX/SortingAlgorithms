package sorts;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.openhft.affinity.AffinityLock;

/**
 * 
 * 
 * BitonicSort:
 * http://www.iti.fh-flensburg.de/lang/algorithmen/sortieren/bitonic/oddn.htm
 * 
 * QuickSort: https://www.geeksforgeeks.org/quick-sort/
 * 
 * @author Diego G.G.
 *
 */
public class Sort {
	static int[] ordered = null;

	public static void main(String[] args) {
		int[] data = DataLoader
				.load("Sorts" + File.separator + "data_in" + File.separator + "0100000_0000000_0001000.in");
		// int[] data = new int[] { 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, 19, 18, 17, 16, 0,
		// 15, 11, 14, 13, 12, 11 };
		// int[] data = new int[] { 68, 54, 15, 85, 89, 73, 23, 9, 69, 62, 39, 19, 38,
		// 99, 9, 74, 80, 11, 39, 54, 94, 6,
		// 97, 73, 38, 26, 74, 8, 5, 34, 73, 57, 54, 35, 62, 68, 85, 85, 81, 31, 80, 77,
		// 54, 55, 47, 32, 34, 87,
		// 70, 52, 27, 10, 90, 74, 100, 98, 81, 30, 5, 63, 33, 74, 30, 95, 70, 88, 40,
		// 61, 69, 45, 59, 2, 11, 32,
		// 33, 99, 1, 43, 2, 79, 15, 67, 25, 13, 33, 27, 24, 51, 44, 34, 18, 51, 39, 66,
		// 8, 80, 15, 88, 43, 72 };
		if (data == null) {
			System.out.println("No data");
		}
		System.out.println("Sample size: " + data.length);
		int cores = Runtime.getRuntime().availableProcessors();
		System.out.println("Sockets: " + AffinityLock.cpuLayout().sockets());
		System.out.println("Total cpus:" + AffinityLock.cpuLayout().cpus());
		System.out.println("Cores per socket: " + AffinityLock.cpuLayout().coresPerSocket());
		System.out.println("Threads per core: " + AffinityLock.cpuLayout().threadsPerCore());
		try (AffinityLock al = AffinityLock.acquireLock()) {
			// do some work while locked to a CPU.
			for (int i = 0; i < 1; ++i) {
				// testSortable(new BubbleSort(), i, data, cores);
				// testSortable(new OddEvenSort(), i, data, cores);
				// testSortable(new RankSort(), i, data, cores);
				testSortable(new CountingSort(), i, data, cores);
				testSortable(new BitonicSort(), i, data, cores);
				testSortable(new QuickSort(), i, data, cores);
				testSortable(new RadixSort(), i, data, cores);
				testSortable(new MergeSort(), i, data, cores);
			}
		}
	}

	public static void testSortable(Sortable s, int version, int data[], int cores) {
		long[] time = new long[cores + 1];
		time[0] = testSerial(s, 5, data);
		/*
		 * for (int i = 1; i < cores + 1; ++i) { try { time[i] = testThreaded(s,
		 * version, 10, data, i); } catch (Exception e) { e.printStackTrace(); } }
		 */
		for (int i = 1; i < cores + 1; ++i) {
			outEnhacement(i, time[0], time[i]);
		}
	}

	public static void outEnhacement(int cores, long t1, long t2) {
		System.out.println("Enhacement: " + cores + " cores " + (((double) t1 / (double) t2) * 100.0 - 100.0));
	}

	public static long testSerial(Sortable s, int runs, int[] data) {
		System.out.print("Serial:   " + s);
		List<Long> times = new ArrayList<>();
		for (int i = 0; i < runs; ++i) {
			int[] clone = data.clone();
			long t1 = System.nanoTime();
			clone = s.sort(clone);
			long t2 = System.nanoTime();
			long serialtime = t2 - t1;
			if (verify(clone)) {
				if (ordered == null) {
					ordered = clone;
				}
				times.add(serialtime);
				System.out.print("," + serialtime);
			} else {
				times.add(-1l);
				System.out.print(",-1 ");
				System.out.println(Arrays.toString(clone));
			}
		}

		// System.out.println(Arrays.toString(clone));
		long av = times.stream().filter(e -> e != -1).reduce((x, y) -> x + y).orElse(0l) / times.size();
		long avmilis = av / 1000000;
		long avsec = avmilis / 1000;
		System.out.println(" Av.Time Seconds:" + avsec + " Milis:" + avmilis + " Nanos:" + av);
		return av;
	}

	public static long testThreaded(Sortable s, int version, int runs, int[] data, int threads) throws Exception {
		System.out.print("Threaded: " + s + "@" + threads);
		List<Long> times = new ArrayList<>();
		for (int i = 0; i < runs; ++i) {
			int[] clone = data.clone();
			long t1 = System.nanoTime();
			clone = s.sortThreaded(version, clone, threads);
			long t2 = System.nanoTime();
			// System.out.println(Arrays.toString(clone));
			long serialtime = t2 - t1;
			if (verify(clone)) {
				if (ordered == null) {
					ordered = clone;
				}
				times.add(serialtime);
				System.out.print("," + serialtime);
			} else {
				times.add(-1l);
				System.out.print(",-1");
			}
			times.add(serialtime);
		}

		long av = times.stream().filter(e -> e != -1).reduce((x, y) -> x + y).orElse(0l) / times.size();
		long avmilis = av / 1000000;
		long avsec = avmilis / 1000;
		System.out.println(" Av.Time Seconds:" + avsec + " Milis:" + avmilis + " Nanos:" + av);
		return av;
	}

	public static boolean verify(int[] data) {
		int i;
		if (ordered != null) {
			for (i = 0; i < data.length; ++i) {
				if (data[i] != ordered[i]) {
					return false;
				}
			}
		} else {
			for (i = 0; i < data.length - 1; ++i) {
				if (data[i] > data[i + 1]) {
					return false;
				}
			}
		}
		return true;
	}
}
