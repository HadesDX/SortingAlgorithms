package sorts;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

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
		long ltime = 1;
		try {
			Thread.sleep(ltime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		int data[] = null;
		boolean smallTestData = false;
		if (smallTestData) {
			data = new int[] { 68, 54, 15, 85, 89, 73, 23, 9, 69, 62, 39, 19, 38, 99, 9, 74, 80, 11, 39, 54, 94, 6, 97,
					73, 38, 26, 74, 8, 5, 34, 73, 57, 54, 35, 62, 68, 85, 85, 81, 31, 80, 77, 54, 55, 47, 32, 34, 87,
					70, 52, 27, 10, 90, 74, 100, 98, 81, 30, 5, 63, 33, 74, 30, 95, 70, 88, 40, 61, 69, 45, 59, 2, 11,
					32, 33, 99, 1, 43, 2, 79, 15, 67, 25, 13, 33, 27, 24, 51, 44, 34, 18, 51, 39, 66, 8, 80, 15, 88, 43,
					72 };
		} else {
			data = DataLoader
					.load("Sorts" + File.separator + "data_in" + File.separator + "0100000_0000000_0001000.in");
		}
		if (data == null) {
			System.out.println("No data");
		}
		ordered = data.clone();
		Arrays.sort(ordered);
		if (data.length < 500) {
			System.out.println("Ordered Data:");
			System.out.println(Arrays.toString(ordered));
		}
		int cores = Runtime.getRuntime().availableProcessors();
		int runs = 100;
		cores = 3;
		System.out.println("Sockets: " + AffinityLock.cpuLayout().sockets());
		System.out.println("Total cpus:" + AffinityLock.cpuLayout().cpus());
		System.out.println("Cores per socket: " + AffinityLock.cpuLayout().coresPerSocket());
		System.out.println("Threads per core: " + AffinityLock.cpuLayout().threadsPerCore());

		// do some work while locked to a CPU.
		for (int version = 0; version < 1; ++version) {// threaded version
			// testSortable(new BubbleSort(), version, data, cores, runs);
			// testSortable(new OddEvenSort(), version, data, cores, runs);
			// testSortable(new RankSort(), version, data, cores, runs);
			testSortable(new CountingSort(), version, data, cores, runs);
			// testSortable(new BitonicSort(), version, data, cores, runs);
			testSortable(new QuickSort(), version, data, cores, runs);
			// testSortable(new RadixSort(), version, data, cores, runs);
			// testSortable(new MergeSort(), version, data, cores, runs);
		}
		if (data.length < 500) {
			System.out.println("Ordered Data:");
			System.out.println(Arrays.toString(ordered));
		}
	}

	public static void testSortable(Sortable s, int version, int data[], int cores, int runs) {
		long[] time = new long[cores + 1];
		AffinityLock singleLock = AffinityLock.acquireCore();
		try {
			time[0] = testSerial(s, runs, data);
		} finally {
			if (singleLock != null) {
				singleLock.release();
			}
		}
		AffinityLock[] coresLocks = new AffinityLock[cores];
		// for (int i = 0; i < cores; ++i) {
		// coresLocks[i] = AffinityLock.acquireCore();
		// }
		// AffinityLock multiLock = AffinityLock.acquireCore();
		// AffinityLock multiLock2 = AffinityLock.acquireCore();
		// AffinityLock multiLock3 = AffinityLock.acquireCore();
		try {
			for (int i = 1; i < cores + 1; ++i) {
				try {
					time[i] = testThreaded(s, version, runs, data, i);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} finally {
			// for (int i = 0; i < cores; ++i) {
			// if (coresLocks[i] != null) {
			// coresLocks[i].release();
			// }
			// }
		}

		for (int i = 1; i < cores + 1; ++i) {
			outEnhacement(i, time[0], time[i]);
		}
	}

	public static void outEnhacement(int cores, long t1, long t2) {
		double val = ((double) t1 / (double) t2) * 100.0 - 100.0;
		System.out.println("Enhacement: " + cores + " cores " + val + (val < 0 ? "% slower" : "% faster"));
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
				times.add(serialtime);
				System.out.print("," + serialtime);
			} else {
				times.clear();
				times.add(-1l);
				System.out.print(",-1 " + serialtime);
				// System.out.println(Arrays.toString(clone));
				break;
			}
		}

		// System.out.println(Arrays.toString(clone));
		Long st = times.stream().filter(e -> e != -1).count();
		long av = 0;
		if (st > 0) {
			av = times.stream().filter(e -> e != -1).reduce((x, y) -> x + y).orElse(0l) / st;
		}
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
			// System.out.println(Arrays.toString(data));
			clone = s.sortThreaded(version, clone, threads);
			long t2 = System.nanoTime();
			// System.out.println(Arrays.toString(clone));
			long threadedTime = t2 - t1;
			if (verify(clone)) {
				times.add(threadedTime);
				System.out.print("," + threadedTime);
			} else {
				times.clear();
				times.add(-1l);
				System.out.print(",-1 " + threadedTime);
				break;
			}
			times.add(threadedTime);
		}

		Long st = times.stream().filter(e -> e != -1).count();
		long av = 0;
		if (st > 0) {
			av = times.stream().filter(e -> e != -1).reduce((x, y) -> x + y).orElse(0l) / st;
		}
		long avmilis = av / 1000000;
		long avsec = avmilis / 1000;
		System.out.println(" Av.Time Seconds:" + avsec + " Milis:" + avmilis + " Nanos:" + av);
		return av;
	}

	public static boolean verify(int[] data) {
		int i;
		if (data == null) {
			return false;
		}
		if (ordered != null) {
			for (i = 0; i < data.length; ++i) {
				if (data[i] != ordered[i]) {
					if (data.length < 500) {
						System.out.println("Verifing data error:");
						System.out.println(Arrays.toString(ordered));
						System.out.println(Arrays.toString(data));
					}
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
		// System.out.println(Arrays.toString(data));
		return true;
	}
}
