package sorts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import net.openhft.affinity.AffinityLock;
import sorts.time.AlgoritmHistogram;
import sorts.time.CoresRun;
import sorts.time.Runs;

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
	public enum Dataset {
		SMALL(null, new int[] { 68, 54, 15, 85, 89, 73, 23, 9, 69, 62, 39, 19, 38, 99, 9, 74, 80, 11, 39, 54, 94, 6, 97,
				73, 38, 26, 74, 8, 5, 34, 73, 57, 54, 35, 62, 68, 85, 85, 81, 31, 80, 77, 54, 55, 47, 32, 34, 87, 70,
				52, 27, 10, 90, 74, 100, 98, 81, 30, 5, 63, 33, 74, 30, 95, 70, 88, 40, 61, 69, 45, 59, 2, 11, 32, 33,
				99, 1, 43, 2, 79, 15, 67, 25, 13, 33, 27, 24, 51, 44, 34, 18, 51, 39, 66, 8, 80, 15, 88, 43, 72 }),
		S0_100_000("0100000_0000000_0001000.in", null), S0_200_000("0200000_0000000_0001000.in", null),
		S0_300_000("0300000_0000000_0001000.in", null), S0_400_000("0400000_0000000_0001000.in", null),
		S0_500_000("0500000_0000000_0001000.in", null), S0_600_000("0600000_0000000_0001000.in", null),
		S0_700_000("0700000_0000000_0001000.in", null), S0_800_000("0800000_0000000_0001000.in", null),
		S0_900_000("0900000_0000000_0001000.in", null), S1_000_000("1000000_0000000_0001000.in", null),
		S2_000_000("2000000_0000000_0001000.in", null), S3_000_000("3000000_0000000_0001000.in", null),
		S4_000_000("4000000_0000000_0001000.in", null), S5_000_000("5000000_0000000_0001000.in", null),
		S6_000_000("6000000_0000000_0001000.in", null), S7_000_000("7000000_0000000_0001000.in", null),
		S8_000_000("8000000_0000000_0001000.in", null), S9_000_000("9000000_0000000_0001000.in", null),;
		public String file;
		int[] set;

		private Dataset(String file, int[] set) {
			this.file = "Sorts" + File.separator + "data_in" + File.separator + file;
		}

		public String getFile() {
			return file;
		}

		public int[] getSet() {
			return set;
		}

		public int size() {
			return set.length;
		}

	}

	static int[] ordered = null;
	static int[] orderedReverse = null;
	static String baseNameOut = "resultados";
	static String strb = "";
	static long ltime = 4000;

	static Dataset dataSet = Dataset.S9_000_000;
	static Dataset[] testSets = new Dataset[] { Dataset.S0_100_000, Dataset.S1_000_000, Dataset.S2_000_000,
			Dataset.S3_000_000, Dataset.S4_000_000, Dataset.S5_000_000, Dataset.S6_000_000, Dataset.S7_000_000,
			Dataset.S8_000_000, Dataset.S9_000_000 };
	static int runs = 10000;
	static int minCores = 0;
	static int maxCores = 4;// Runtime.getRuntime().availableProcessors();
	static boolean u = true;
	static boolean o = false;
	static boolean r = false;
	static boolean cycleDataset = true;

	public static void main(String[] args) {
		AlgoritmHistogram histogram;
		AlgoritmHistogram histogramO;
		AlgoritmHistogram histogramU;
		AlgoritmHistogram histogramR;
		int data[] = null;
		data = dataSet.getSet() != null ? dataSet.getSet() : DataLoader.load(dataSet.getFile());

		if (data == null) {
			System.out.println("No data");
		}
		ordered = data.clone();
		Arrays.sort(ordered);
		int l = data.length;
		orderedReverse = new int[l];
		IntStream.range(0, data.length).parallel().forEach(e -> {
			orderedReverse[e] = ordered[l - e - 1];
		});

		if (data.length < 500) {
			System.out.println("Ordered Data:");
			System.out.println(Arrays.toString(ordered));
		}

		System.out.println("Sockets: " + AffinityLock.cpuLayout().sockets());
		System.out.println("Total cpus:" + AffinityLock.cpuLayout().cpus());
		System.out.println("Cores per socket: " + AffinityLock.cpuLayout().coresPerSocket());
		System.out.println("Threads per core: " + AffinityLock.cpuLayout().threadsPerCore());
		// do some work while locked to a CPU.
		int version = 0;
		Sortable[] algs = new Sortable[] { //
				new RadixSort(), //
				new CountingSort()// , //
				// new BitonicSort(), //
				// new QuickSort(), //
				// new MergeSort() //
				// new OddEvenSort() // n^2
				// new RankSort(), // n^2
				// new BubbleSort() // n^2
		};
		histogramU = new AlgoritmHistogram(data.length, "Uniforme", algs.length, maxCores, runs);
		histogramO = new AlgoritmHistogram(ordered.length, "Ordenado", algs.length, maxCores, runs);
		histogramR = new AlgoritmHistogram(orderedReverse.length, "Inverso", algs.length, maxCores, runs);
		for (int i = 0; i < algs.length; ++i) {
			histogramU.setAlgName(i, algs[i].toString());
			histogramO.setAlgName(i, algs[i].toString());
			histogramR.setAlgName(i, algs[i].toString());
		}

		try {
			Thread.sleep(ltime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (u) {
			if (cycleDataset) {
				for (Dataset set : testSets) {
					dataSet = set;
					data = dataSet.getSet() != null ? dataSet.getSet() : DataLoader.load(dataSet.getFile());
					ordered = data.clone();
					Arrays.sort(ordered);

					histogram = new AlgoritmHistogram(data.length, "Uniforme", algs.length, maxCores, runs);
					histogramU = histogram;
					for (int i = 0; i < algs.length; ++i) {
						histogramU.setAlgName(i, algs[i].toString());
					}
					for (int i = 0; i < algs.length; ++i) {
						new Sort().testSortable(algs[i], i, version, data, minCores, maxCores, runs,
								histogram.getAlgRuns().get(i), histogram);
					}
					try (FileWriter fw = new FileWriter(
							baseNameOut + File.separator + "uniforme" + data.length + ".csv"); //
							BufferedWriter bw = new BufferedWriter(fw)) {
						histogramU.toTable(bw);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} else {
				histogram = histogramU;
				for (int i = 0; i < algs.length; ++i) {
					new Sort().testSortable(algs[i], i, version, data, minCores, maxCores, runs,
							histogram.getAlgRuns().get(i), histogram);
				}
				try (FileWriter fw = new FileWriter(baseNameOut + File.separator + "uniforme" + data.length + ".csv"); //
						BufferedWriter bw = new BufferedWriter(fw)) {
					histogramU.toTable(bw);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		if (o) {
			histogram = histogramO;
			for (int i = 0; i < algs.length; ++i) {
				new Sort().testSortable(algs[i], i, version, ordered, minCores, maxCores, runs,
						histogram.getAlgRuns().get(i), histogram);
			}

			try (FileWriter fw = new FileWriter(baseNameOut + File.separator + "ordenado" + data.length + ".csv"); //
					BufferedWriter bw = new BufferedWriter(fw)) {
				histogramO.toTable(bw);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (r) {
			histogram = histogramR;
			for (int i = 0; i < algs.length; ++i) {
				new Sort().testSortable(algs[i], i, version, orderedReverse, minCores, maxCores, runs,
						histogram.getAlgRuns().get(i), histogram);
			}
			try (FileWriter fw = new FileWriter(baseNameOut + File.separator + "inverso" + data.length + ".csv"); //
					BufferedWriter bw = new BufferedWriter(fw)) {
				histogramR.toTable(bw);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// try {
		// histogram.toTable(System.out);
		// } catch (IOException e) {
		// e.printStackTrace();
		// }

		if (data.length < 500) {
			System.out.println("Ordered Data:");
			System.out.println(Arrays.toString(ordered));
		}
		System.out.println(strb);
		try (FileWriter fw = new FileWriter("synctimeoddeven.csv");
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw)) {
			out.println(strb);
		} catch (IOException e) {
		}
	}

	public void testSortable(Sortable s, int alg, int version, int data[], int minCores, int maxCores, int runs,
			CoresRun coresRun, AlgoritmHistogram histogram) {
		long[] time = new long[maxCores + 1];
		if (minCores == 0) {
			// AffinityLock singleLock = AffinityLock.acquireCore();
			try {
				time[0] = testSerial(s, runs, data, coresRun.getCores().get(0));
				histogram.toTableStepByStep(baseNameOut, alg, 0);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				// if (singleLock != null) {
				// singleLock.release();
				// }
			}
		}
		AffinityLock[] coresLocks = new AffinityLock[maxCores];
		// for (int i = 0; i < cores; ++i) {
		// coresLocks[i] = AffinityLock.acquireCore();
		// }
		// AffinityLock multiLock = AffinityLock.acquireCore();
		// AffinityLock multiLock2 = AffinityLock.acquireCore();
		// AffinityLock multiLock3 = AffinityLock.acquireCore();
		if (minCores == 0) {
			minCores = 1;
		}
		try {
			for (int i = minCores; i < maxCores + 1; ++i) {
				try {
					time[i] = testThreaded(s, version, runs, data, i, coresRun.getCores().get(i));
					histogram.toTableStepByStep(baseNameOut, alg, i);
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

		for (int i = 1; i < maxCores + 1; ++i) {
			outEnhacement(i, time[0], time[i]);
		}
	}

	public void outEnhacement(int cores, long t1, long t2) {
		double val = ((double) t1 / (double) t2) * 100.0 - 100.0;
		System.out.println("Enhacement: " + cores + " cores " + val + (val < 0 ? "% slower" : "% faster"));
	}

	public long testSerial(Sortable s, int runs, int[] data, Runs runResults) throws Exception {
		System.out.print("Serial:   " + s);
		List<Long> times = new ArrayList<>();
		for (int i = 0; i < runs; ++i) {
			int[] clone = data.clone();
			long t1 = System.nanoTime();
			clone = s.sort(clone);
			long t2 = System.nanoTime();
			long serialtime = t2 - t1;
			runResults.setRun(i, t2 - t1);
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
			// System.gc();
			// Thread.sleep(10);
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

	public long testThreaded(Sortable s, int version, int runs, int[] data, int threads, Runs runsResult)
			throws Exception {
		System.out.print("Threaded: " + s + "@" + threads);
		List<Long> times = new ArrayList<>();
		for (int i = 0; i < runs; ++i) {
			int[] clone = data.clone();
			if (s instanceof OddEvenSort) {
				((OddEvenSort) s).reset();
			}
			long t1 = System.nanoTime();
			// System.out.println(Arrays.toString(data));
			clone = s.sortThreaded(version, clone, threads);
			long t2 = System.nanoTime();
			long threadedTime = t2 - t1;

			if (s instanceof OddEvenSort) {
				long tt = ((OddEvenSort) s).syncLost();
				strb += threads + "," + threadedTime + "," + tt + "," + (threadedTime - tt) + "\n";
				System.out.println("@" + threadedTime + "/" + tt + "/" + (threadedTime - tt) + "@");
			}

			runsResult.setRun(i, threadedTime);
			// System.out.println(Arrays.toString(clone));
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
			// System.gc();
			// Thread.sleep(10);
		}

		Long st = times.stream().filter(e -> e != -1).count();
		long av = 0;
		if (st > 0) {
			av = times.stream().filter(e -> e != -1).reduce((x, y) -> x + y).orElse(0l) / st;
		}
		long avmilis = av / 1000000;
		long avsec = avmilis / 1000;
		System.out.println("\nAv.Time Seconds:" + avsec + " Milis:" + avmilis + " Nanos:" + av);
		return av;
	}

	public boolean verify(int[] data) {
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
