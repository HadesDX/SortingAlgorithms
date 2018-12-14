package sorts.time;

import java.util.ArrayList;

public class CoresRun {
	public ArrayList<Runs> cores;

	public CoresRun(int maxCores, int maxRuns) {
		cores = new ArrayList<>(maxCores);
		for (int i = 0; i < maxCores; i++) {
			cores.add(new Runs(maxRuns));
		}
	}

	public void setRun(int cores, int run, long runTime) {
		this.cores.get(cores).setRun(run, runTime);
	}

	public ArrayList<Double> getAverages() {
		ArrayList<Double> av = new ArrayList<>(cores.size());
		for (int i = 0; i < cores.size(); i++) {
			av.add(cores.get(i).avTime());
		}
		return av;
	}

	public ArrayList<Runs> getCores() {
		return cores;
	}

	public void setCores(ArrayList<Runs> cores) {
		this.cores = cores;
	}

	@Override
	public String toString() {
		return "CoresRun [cores=" + cores + "]";
	}

}
