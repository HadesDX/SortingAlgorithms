package sorts;

import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * ****************( ver 20 december 2011)
 * *********************************************** A full parallel sorting
 * algorithm - Parallel Ataptive Left Radix (PALR) i.e. no step is O(n) - all
 * are O(n/numCores) or less) This is Tread Safe Copyright: Arne Maus, Univ of
 * Oslo, arnem@ifi.uio.no, 2011 This code is avaiable on BSD License (full text
 * see below)
 **********************************************************************************/
public class ParaSortTS {
	CyclicBarrier waiting, finished, barrier;
	int[] a;
	int numThreads, maxTråder;
	Thread[] threads;

	static final int INSERT_MAX = 40; // any value below this, use Insertion sort in '
	static final int THREAD_MAX = 32; // Big hyperthreaded machines beform better with fewer threads
	static final int ARL_SEQ_MIN = 150000; // any value below this ARL sequential sort
	// static final int ARL_SEQ_MIN = 0; // any value below this ARL sequential sort
	static final int MIN_NUM_BIT = 6; // min num of bits used by ARL-sorting
	static final int MAX_NUM_BIT = 9; // max num of bits used by ARL-sorting
	static final int QUICK_SEQ_MIN = 50000; // any value below this ARL sequential sort

	int[] localMax; // common datastructure for finding max
	int[][] allBorders; // All border arrays from ARLsort , one pass
	int[] bucketSize; // find start of where to copy back 'b' in a.
	volatile boolean moreSort = true;

	/**
	 * konstructor, initier variable mm
	 **************************************************/
	ParaSortTS(int numThreads) {
		// numThreads = Runtime.getRuntime().availableProcessors();
		this.numThreads = numThreads;
		this.maxTråder = numThreads;
		if (numThreads > 1) {
			threads = new Thread[numThreads];
			waiting = new CyclicBarrier(numThreads + 1); // +1, også main
			finished = new CyclicBarrier(numThreads + 1); // +1, også main
			barrier = new CyclicBarrier(numThreads);

			allBorders = new int[numThreads][]; // need borders to copy
			bucketSize = new int[numThreads + 1];
			localMax = new int[numThreads];

			// start threads
			for (int i = 0; i < numThreads; i++) {
				threads[i] = new Thread(new Worker(numThreads, i));
				threads[i].start();
			}
		}

	} // end kontruktør

	public void endPool() {
		if (threads != null) {
			for (int i = 0; i < threads.length; i++) {
				threads[i].stop();
			}
		}
	}

	/**
	 * konstructor, initier variable mm
	 **************************************************/
	ParaSortTS() {
		numThreads = Runtime.getRuntime().availableProcessors();
		if (numThreads > THREAD_MAX)
			numThreads *= 0.7;
		this.maxTråder = numThreads;

		waiting = new CyclicBarrier(numThreads + 1); // +1, også main
		finished = new CyclicBarrier(numThreads + 1); // +1, også main
		barrier = new CyclicBarrier(numThreads);

		allBorders = new int[numThreads][]; // need borders to copy
		bucketSize = new int[numThreads + 1];
		localMax = new int[numThreads];

		// start threads
		for (int i = 0; i < numThreads; i++) {
			new Thread(new Worker(numThreads, i)).start();
		}

	} // end kontruktør

	/**
	 * sort a with numThreads, intial setup uf barriers and threads
	 ****************************************************************/
	synchronized void paraARL(int[] a) {
		this.a = a;
		numThreads = maxTråder;

		if (a.length < ARL_SEQ_MIN || numThreads < 2) {
			// shorter arrays than say 5000 should be sortet sequentially
			if (a.length < INSERT_MAX) {
				insertSort(a, 0, a.length - 1);
			} else {
				ARLsort(a);
			}
		} else {

			try { // start all treads
				waiting.await();
			} catch (Exception e) {
				return;
			}

			try { // wait for all treads to complete
				finished.await();
			} catch (Exception e) {
				return;
			}

		} // end else

	} // end

	/**
	 * Sort a as fast as possible in parallel if a.length > ARL_SEQ_MIN elements
	 * Copyright (c) 2000,2009, Arne Maus, Dept. of Informatics, Univ. of Oslo,
	 * Norway. All rights reserved.
	 * 
	 * Redistribution and use in source and binary forms, with or without
	 * modification, are permitted provided that the following conditions are met:
	 * Redistributions of source code must retain the above copyright notice, this
	 * list of conditions and the following disclaimer. Redistributions in binary
	 * form must reproduce the above copyright notice, this list of conditions and
	 * the following disclaimer in the documentation and/or other materials provided
	 * with the distribution. Neither the name of the <organization> nor the names
	 * of its contributors may be used to endorse or promote products derived from
	 * this software without specific prior written permission.
	 * 
	 * THIS SOFTWARE IS PROVIDED BY ARNE MAUS, DEPT. OF INFORMATICS, UNIV. OF OSLO,
	 * NORWAY ''AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
	 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
	 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL ARNE MAUS, BE LIABLE FOR
	 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
	 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
	 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
	 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
	 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
	 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
	 ********************************************************************/
	void partSortARL(int[] a, int threadIndex) {

		// if we reduce number of threads for a shorter problem
		if (threadIndex < numThreads) {

			// this tread is responsable for :
			// Radix-sorting first a[len*(num)..len*(num+1) -1] with k bits (2-12)
			// then sort all data on the (2**k/N)*(num) bit values for the k bits
			int[] b;

			int maxV = 0;
			long startl = ((long) a.length * (long) threadIndex) / numThreads; // start of section to sort
			long endl = ((long) a.length * ((long) threadIndex + 1)) / numThreads - 1; // end of section to sort
																						// inclusive
			if (endl + a.length / numThreads > a.length)
				endl = a.length - 1;
			// possible rounding error, last t end includes this

			int end = (int) endl, start = (int) startl;
			// a) find max in a[start..end] - this thread
			for (int i = start; i <= end; i++) {
				if (a[i] > maxV)
					maxV = a[i];
			}
			localMax[threadIndex] = maxV;

			// 1) barrier, wait for all to complete max calculation
			// --------------------------------------------------------
			try {
				barrier.await();
			} catch (InterruptedException ex) {
				System.out.println(" 1workerEX 1 , threadIndex" + threadIndex);
				return;
			} catch (BrokenBarrierException ex) {
				System.out.println(" 2workerEX 2 , threadIndex" + threadIndex);
				return;
			}
			// -----------------------------------------------------------------

			// b) find largest of all max values.
			for (int i = 0; i < numThreads; i++) {
				if (localMax[i] > maxV)
					maxV = localMax[i]; // find global max
			}

			// c) Sort first digitradix-sort 1/numThreads'th part of the array:a[start..end]
			int leftBitNo = 0, max = maxV;
			while (maxV > 1) {
				// find leftBitNo
				leftBitNo++;
				maxV = maxV >> 1;
			}

			allBorders[threadIndex] = oneDigitARL(a, start, end, max); // full sort a[start..end]

			// 2) barrier wait for all to sort their part
			// --------------------------------------------
			try {
				barrier.await();
			} catch (InterruptedException ex) {
				System.out.println(" 3workerEX 1 , threadIndex" + threadIndex);
				return;
			} catch (BrokenBarrierException ex) {
				System.out.println(" 4workerEX 2 , threadIndex" + threadIndex);
				return;
			}
			// -----------------------------------------------------------------
			// start
			// d) sum up from all treads the where to read the values of
			// the k bits this tread shall sort 0 m numbers
			// d.1) find num bits sorted on
			int bLen = allBorders[threadIndex].length - 2, bbLen = bLen; //
			int numBit = 0;
			while (bbLen > 0) {
				bbLen = bbLen >> 1;
				numBit++;
			} // find how many bits; border.length <= 2**numBits-1

			// d.2) Find which bit values in border is to be sorted by this thread
			int numValues = bLen / numThreads;

			int startVIndex = 0, endVIndex = 0; // this Thread responsable for [startVIndex..endVIndex>
			startVIndex = (threadIndex) * numValues;
			// where+1 to find start of sort area
			endVIndex = (threadIndex + 1) * numValues - 1; // where to find start +1
			if (numThreads - 1 == threadIndex) {
				// last thread
				endVIndex = bLen;
			}

			// d.3) sum bordervalues to num to sort for this thread
			int numElem = 0, bStart = 0, bEnd = 0;
			for (int i = 0; i < numThreads; i++) {
				bStart = allBorders[i][startVIndex];
				bEnd = allBorders[i][endVIndex + 1];
				numElem += bEnd - bStart;
			}
			bucketSize[threadIndex + 1] = numElem;

			;
			// e) b = new int [m], copy these elements to b[]
			b = new int[numElem];

			int bIndex = 0, oldBIndex = 0;

			leftBitNo = leftBitNo - numBit; // = -1 if finished

			// copy elemts to b
			for (int bit = startVIndex; bit <= endVIndex; bit++) {
				oldBIndex = bIndex;

				for (int i = 0; i < numThreads; i++) {
					int elemStart = allBorders[i][bit], elemStop = allBorders[i][bit + 1];

					for (int k = elemStart; k < elemStop; k++) {
						b[bIndex++] = a[k];
					} // end copy

				} // end all threads

				int len = bIndex - oldBIndex + 1;

				// f) sort fully this part of b[] , all same bits in leftBitNo -numbit
				if (leftBitNo >= 0 && (len > 1)) {
					// more than 1 element in this section
					if (len < INSERT_MAX)
						insertSort(b, oldBIndex, bIndex - 1);
					else
						sortARLwithBorders(b, oldBIndex, bIndex - 1, leftBitNo);

				} // end sort these bits in left part

			} // end all bits for this thread

			// 3) barrier wait for all to have moved their part to b
			// --------------------------------------------------------
			try {
				barrier.await();
			} catch (InterruptedException ex) {
				System.out.println(" 3workerEX 1 , threadIndex" + threadIndex);
				return;
			} catch (BrokenBarrierException ex) {
				System.out.println(" 4workerEX 2 , threadIndex" + threadIndex);
				return;
			}
			// -----------------------------------------------------------------

			// g)n copy back b[] fully sorted in its propper place in a[]
			int aIndex = bucketSize[threadIndex];
			for (int i = 1; i < threadIndex; i++)
				aIndex += bucketSize[i]; // find start of copy in 'a'

			System.arraycopy(b, 0, a, aIndex, b.length);

			b = null;

		} // end this thread is participating in current sorting

	} // end partSortARL

	/** Terminate infinite loop */
	synchronized void exit() {
		moreSort = false;
		try { // start all treads
			waiting.await();
		} catch (Exception e) {
			return;
		}
	} // end exit

	class Worker implements Runnable {
		int numThreads, threadIndex;

		Worker(int numThreads, int threadIndex) {
			this.numThreads = numThreads; // number of Cores
			this.threadIndex = threadIndex; // sorting thread number 0,1,..

		}

		public void run() {
			do {

				try { // wait on all other threads + main
					waiting.await();
				} catch (Exception e) {
					return;
				}

				if (moreSort) {
					partSortARL(a, threadIndex);

					try { // wait on all other threads + main
						finished.await();
					} catch (Exception e) {
						return;
					}
				}

			} while (moreSort);
		} // end run

	} // end *** class Worker ***

	// -----------------------------(sequential algorithms ARL & Insert +
	// utilities)------------------------

	/** Interface method to ARL sort, finds highest bit set, sort all */
	public static void ARLsort(int[] a) {
		// ARL Sort from a[left] 'up to and including' a[right]
		int max = a[0];
		for (int i = 1; i < a.length; i++)
			if (a[i] > max)
				max = a[i];

		int leftBitNo = 0;
		// find highest bit set
		while (max > 1) {
			max >>= 1;
			leftBitNo++;
		}
		sortARLwithBorders(a, 0, a.length - 1, leftBitNo);

	} // end ARLsort

	/**
	 * ARL sort with border array, The 2002 version Adaptive Left Radix with
	 * Insert-sort as a subalgorithm. Sorts positive integers from a[start] 'up to
	 * and including' a[end] on bits: leftBitNo, leftBitNo-1,..,leftBitNo -numBit+1
	 * (31..0) Uses only internal moves by shifting along permutation cycles <br>
	 * UNSTABLE
	 *
	 * @Author: Arne Maus, Dept.of Informatics,Univ. of Oslo, 2000-2009 Copyright
	 *          (c) 2000,2009, Arne Maus, Dept. of Informatics, Univ. of Oslo,
	 *          Norway. All rights reserved.
	 */
	static void sortARLwithBorders(int a[], int start, int end, int leftBitNo) {
		int i, lim, temp, t1, t2, mask, rBitNo, numBit, newNum, nextbox, adr2, k, k2, num = end - start + 1;

		int[] point, border;

		// System.out.println(" traad:"+threadIndex+", fullARL : start::"+start+ ",
		// end:"+end+", leftBitNo:"+leftBitNo);

		// adaptive part - adjust numBit : number of bits to sort on in this pass
		// a) adapts to bits left to sort to sort AND cache-size level 1 (8-32KB)
		numBit = Math.min(leftBitNo + 1, MAX_NUM_BIT);
		// b) adapts to 'sparse' distribution
		while ((1 << (numBit - 1)) > num && numBit > MIN_NUM_BIT)
			numBit--;
		if (numBit == leftBitNo)
			numBit++; // ewaitingually, do the last bit

		// sort on leftmost 'numBits' starting at bit no: leftBitNo
		// setting constants
		rBitNo = leftBitNo - numBit + 1;
		lim = 1 << numBit;
		mask = (lim - 1) << rBitNo;
		point = new int[lim + 1];
		border = new int[lim + 1]; // ** Modification (+2) for parallel

		// sort on 'numBit' bits, from: leftBitNo'to 'rBitNo+1' in a[start..end]

		// c) count-scan 'numBit' bits
		for (i = start; i <= end; i++)
			point[(a[i] & mask) >> rBitNo]++;

		t2 = point[0];
		border[0] = point[0] = start;

		for (i = 1; i <= lim; i++) {
			// d) point [i] points to where bundle 'i' starts, stopvalue in borders[lim-1]
			t1 = t2;
			t2 = point[i];
			border[i] = point[i] = point[i - 1] + t1;
		}

		border[lim] = end + 1; // 'wrong' value to stop next loop

		int currentBox = 0, pos = start;

		// find next element to move in permtation cycles
		// skip cycles of length =1

		while (point[currentBox] == border[currentBox + 1])
			currentBox++;

		while (currentBox < lim) {
			// find next cycle, skip (most)cycles of length =1
			pos = point[currentBox];
			k = a[pos];

			// start of new permutation cycle
			adr2 = point[(k & mask) >> rBitNo]++;

			if (adr2 > pos) {
				// permuttion cycle longer than 1 element
				do {
					k2 = a[adr2];
					// central loop
					a[adr2] = k;
					adr2 = point[(k2 & mask) >> rBitNo]++;
					k = k2;
				} while (adr2 > pos);

				a[pos] = k;

			} // end perm cycle

			// find box where to find start of new permutation cycle
			while (currentBox < lim && point[currentBox] == border[currentBox + 1])
				currentBox++;

		} // end more to sort

		leftBitNo = leftBitNo - numBit;

		if (leftBitNo >= 0) {
			// more to sort - recursively
			t2 = start;
			for (i = 0; i < lim; i++) {
				t1 = t2;
				t2 = point[i];
				newNum = t2 - t1;

				// call each cell if more than one number
				if (newNum > 1) {
					if (newNum <= INSERT_MAX) {
						insertSort(a, t1, t2 - 1);
					} else {
						sortARLwithBorders(a, t1, t2 - 1, leftBitNo);
					}
				} // if newNum > 1
			} // end for
		} // end if leftBitNo

	}// end sortAfRLwithBorders

	/**
	 * oneDigitARL sort with border array, The 2002 version Adaptive Left Radix with
	 * Insert-sort as a subalgorithm. Sorts positive integers from a[start] 'up to
	 * and including' a[end] on bits: leftBitNo, leftBitNo-1,..,leftBitNo -numBit+1
	 * (31..0) Uses only internal moves by shifting along permutation cycles <br>
	 * UNSTABLE
	 *
	 * @Author: Arne Maus, Dept.of Informatics,Univ. of Oslo, 2000-2011 Copyright
	 *          (c) 2000,2011, Arne Maus, Dept. of Informatics, Univ. of Oslo,
	 *          Norway. All rights reserved.
	 */
	static int[] oneDigitARL(int a[], int start, int end, int max) {
		int i, lim, temp, t1, t2, mask, rBitNo, numBit, newNum, leftBitNo = 0, maxV = max, nextbox, adr2, k, k2,
				num = end - start + 1;

		int[] point, border;

		// find highest bit set (>1 7.09.10 - old:>0)
		while (maxV > 1) {
			maxV >>= 1;
			leftBitNo++;
		}

		// limit numBit
		numBit = Math.min(leftBitNo + 1, MAX_NUM_BIT); // first call different

		// sort on leftmost 'numBits' starting at bit no: leftBitNo
		// setting constants
		rBitNo = leftBitNo - numBit + 1;
		lim = max >> rBitNo; // old: 1 << numBit;
		// mask = (lim - 1)<< rBitNo;
		mask = (1 << (leftBitNo + 1)) - 1;
		point = new int[lim + 1];
		border = new int[lim + 2]; // ** Modification (+2) for parallel

		// sort on 'numBit' bits, from: leftBitNo'to 'rBitNo+1' in a[start..end]

		// c) count-scan 'numBit' bits
		for (i = start; i <= end; i++)
			point[(a[i] & mask) >> rBitNo]++;

		t2 = point[0];
		border[0] = point[0] = start;

		for (i = 1; i <= lim; i++) {
			// d) point [i] points to where bundle 'i' starts, stopvalue in borders[lim-1]
			t1 = t2;
			t2 = point[i];
			border[i] = point[i] = point[i - 1] + t1;
		}

		border[lim + 1] = end + 1; // 'stop' value for next loop

		int currentBox = 0, pos = start;

		// find next element to move in permtation cycles
		// skip cycles of length =1

		while (point[currentBox] == border[currentBox + 1])
			currentBox++;

		while (currentBox < lim) {
			// find next cycle, skip (most)cycles of length =1
			pos = point[currentBox];
			k = a[pos];

			// start of new permutation cycle
			adr2 = point[(k & mask) >> rBitNo]++;

			if (adr2 > pos) {
				// permuttion cycle longer than 1 element
				do {
					k2 = a[adr2];
					// central loop
					a[adr2] = k;
					adr2 = point[(k2 & mask) >> rBitNo]++;
					k = k2;
				} while (adr2 > pos);

				a[pos] = k;

			} // end perm cycle

			// find box where to find start of new permutation cycle
			while (currentBox < lim && point[currentBox] == border[currentBox + 1])
				currentBox++;

		} // end more to sort

		return border; // used by caller

	}// ---- end oneDigitARL

	/** sorts a [left .. right] by Insertion sort alg. Sub-alg for short segments */
	private static void insertSort(int a[], int left, int right) {
		int i, k, t;
		;

		for (k = left; k < right; k++) {
			if (a[k] > a[k + 1]) {
				t = a[k + 1];
				i = k;

				while (i >= left && a[i] > t) {
					a[i + 1] = a[i];
					i--;
				}
				a[i + 1] = t;
			}
		}
	} // end insertSort

	/** Returns a random filled (Uniform 0:n-1) array */
	static int[] getArray(int n) {
		Random r = new Random(123789 + n);
		int[] a = new int[n];
		for (int i = 0; i < n; i++)
			a[i] = r.nextInt(n);
		return a;
	} // end getArray

	/** simple test if sorted array */
	static void sortTest(int[] a, int[] fasit) {
		for (int i = 0; i < a.length; i++)
			if (a[i] != fasit[i]) {
				if (i > 0)
					System.out.print("- sort-error;, a[" + (i - 1) + "]:" + a[i - 1]);
				System.out.print(", a[" + i + "]:" + a[i]);
				System.out.println(", a[" + (i + 1) + "]:" + a[i + 1]);
				if (i > 0)
					System.out.print("-  fasit[" + (i - 1) + "]:" + fasit[i - 1]);
				System.out.println(", fasit[" + i + "]:" + fasit[i] + ", fasit[" + (i + 1) + "]:" + fasit[i + 1]);
				while (i == i)
					; // Stop output, terminate with ctrl-C
			}
	} // end sortTest

} // end class ParaSort