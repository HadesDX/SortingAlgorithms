package sorts.time;

import java.util.ArrayList;

public class Runs {
	ArrayList<Long> runs;

	public Runs(int maxRuns) {
		runs = new ArrayList<>(maxRuns);
		for (int i = 0; i < maxRuns; i++) {
			runs.add(-1l);
		}
	}

	public void setRun(int index, long runTime) {
		this.runs.set(index, runTime);
	}

	public double avTime() {
		return runs.stream().mapToLong(Long::longValue).average().orElse(-1);
	}

	public ArrayList<Long> getRuns() {
		return runs;
	}

	public void setRuns(ArrayList<Long> runs) {
		this.runs = runs;
	}

	@Override
	public String toString() {
		return "Runs [runs=" + runs + "]";
	}

}
