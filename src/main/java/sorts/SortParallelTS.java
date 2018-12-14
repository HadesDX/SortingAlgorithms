package sorts;

import java.util.Arrays;

/**
 * ****************( ver 20 december 2011)
 * *********************************************** A full parallel sorting
 * algorithm - Parallel Ataptive Left Radix (PALR) i.e. no step is O(n) - all
 * are O(n/numCores) or less) This is Tread Safe Copyright: Arne Maus, Univ of
 * Oslo, arnem@ifi.uio.no, 2011 This code is avaiable on BSD License (full text
 * see below)
 **********************************************************************************/
public class SortParallelTS {
	// main class for interfacing

	synchronized static void println(String s) {
		System.out.println(s + "");
	}

	synchronized static void print(String s) {
		System.out.print(s + "");
	}

	public static void main(String args[]) {
		if (args.length < 3) {
			System.out.println(" Use : >java  SortParallelTS nlow stepMult nhigh");
		} else {
			Runtime r = Runtime.getRuntime();
			int numCore = r.availableProcessors();
			double threadMultCore = .0;
			int numThreads, maxNumThreads;

			// int numThreads = numCore;
			// numCore =1;
			int nLow = new Integer(args[0]).intValue(), // lowest n to sort for
					step = new Integer(args[1]).intValue(), // step (multiplication) for n
					nHigh = new Integer(args[2]).intValue(); // highest value sorted
			long num = 0;

			// Sorting object for ParaARL
			ParaSortTS p = new ParaSortTS();

			numThreads = maxNumThreads = p.numThreads;

			println("\nParaSorting parameters, INSERT_MAX:" + ParaSortTS.INSERT_MAX + ", THREAD_MAX:"
					+ ParaSortTS.THREAD_MAX + ", ARL_SEQ_MIN:" + ParaSortTS.ARL_SEQ_MIN + "\n,  MIN_NUM_BIT:"
					+ ParaSortTS.MIN_NUM_BIT + " , MAX_NUM_BIT:" + ParaSortTS.MAX_NUM_BIT + ", QUICK_SEQ_MIN:"
					+ ParaSortTS.QUICK_SEQ_MIN + ", numCore:" + numCore + ", maxNumThreads:" + maxNumThreads);

			// central test loop
			for (int n = nHigh; n >= nLow; n /= step) {
				long startTime = 0, ParaTime = 0, quickTime = 0;
				double qTime, pTime, relative;
				int[] a;

				println("\nSorting, length of a:" + n + ", numCores:" + numCore);

				num = nHigh / n; // iterate sorting to gain accuracy;

				for (int j = 0; j < num; j++) {
					a = ParaSortTS.getArray(n);
					startTime = System.nanoTime();
					Arrays.sort(a);
					quickTime += System.nanoTime() - startTime;
				}

				qTime = (double) quickTime / (num * 1000000.0);

				// --- ARLPara with borders sort test:
				for (int j = 0; j < num; j++) {
					a = ParaSortTS.getArray(n);
					startTime = System.nanoTime();
					p.paraARL(a);
					ParaTime += System.nanoTime() - startTime;
				}

				pTime = (double) ParaTime / (num * 1000000.0);
				relative = 0;
				if (quickTime > 0)
					relative = (100.0 * ParaTime / quickTime);
				println("   ParARLSort: " + pTime + " millisec, ParaARL/Quick:  " + relative + "%" + ", p.numThreads:"
						+ p.numThreads);

			} // end for

			println("\nParaSorting parameters, INSERT_MAX:" + ParaSortTS.INSERT_MAX + ", THREAD_MAX:"
					+ ParaSortTS.THREAD_MAX + ", ARL_SEQ_MIN:" + ParaSortTS.ARL_SEQ_MIN + "\n,  MIN_NUM_BIT:"
					+ ParaSortTS.MIN_NUM_BIT + " , MAX_NUM_BIT:" + ParaSortTS.MAX_NUM_BIT + ", QUICK_SEQ_MIN:"
					+ ParaSortTS.QUICK_SEQ_MIN + " , MAX_NUM_BIT:" + ParaSortTS.MAX_NUM_BIT + ", QUICK_SEQ_MIN:"
					+ ParaSortTS.QUICK_SEQ_MIN + ", numCore:" + numCore + ", maxNumThreads:" + maxNumThreads);

			p.exit();

		} // end else

	} // end main

} // end SortParallel
