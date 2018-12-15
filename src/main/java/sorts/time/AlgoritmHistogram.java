package sorts.time;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;

public class AlgoritmHistogram {
	private ArrayList<String> algoritm;
	private ArrayList<CoresRun> algRuns;
	private String dataSetType;
	private long dataSetSize;

	public AlgoritmHistogram(long dataSetSize, String dataSetType, int maxAlgs, int maxCores, int maxRuns) {
		this.dataSetSize = dataSetSize;
		this.dataSetType = dataSetType;
		algoritm = new ArrayList<>(maxAlgs);
		algRuns = new ArrayList<>(maxAlgs);
		for (int i = 0; i < maxAlgs; i++) {
			algoritm.add("");
		}
		for (int i = 0; i < maxAlgs; i++) {
			algRuns.add(new CoresRun(maxCores + 1, maxRuns));
		}
	}

	public void setAlgName(int alg, String name) {
		algoritm.set(alg, name);
	}

	public void addRun(int alg, int cores, int run, int runTime) {
		algRuns.get(alg).setRun(cores, run, runTime);
	}

	public ArrayList<String> getAlgoritm() {
		return algoritm;
	}

	public void setAlgoritm(ArrayList<String> algoritm) {
		this.algoritm = algoritm;
	}

	public ArrayList<CoresRun> getAlgRuns() {
		return algRuns;
	}

	public void setAlgRuns(ArrayList<CoresRun> algRuns) {
		this.algRuns = algRuns;
	}

	public String getDataSetType() {
		return dataSetType;
	}

	public void setDataSetType(String dataSetType) {
		this.dataSetType = dataSetType;
	}

	public long getDataSetSize() {
		return dataSetSize;
	}

	public void setDataSetSize(long dataSetSize) {
		this.dataSetSize = dataSetSize;
	}

	public void toTable(OutputStream w) throws IOException {

		byte[] dss = (dataSetSize + "").getBytes();
		byte[] dst = dataSetType.getBytes();
		for (int alg = 0; alg < algoritm.size(); alg++) {
			CoresRun c = algRuns.get(alg);
			byte[] algs = (alg + "").getBytes();
			for (int cores = 0; cores < c.getCores().size(); cores++) {
				Runs r = c.getCores().get(cores);
				for (int run = 0; run < r.getRuns().size(); run++) {
					w.write(dst); // Data set type
					w.write(',');
					w.write(dss); // Data set size
					w.write(',');
					w.write(algoritm.get(alg).getBytes()); // Algorithm name
					w.write(',');
					w.write(algs); // Cores
					w.write(',');
					w.write(r.getRuns().get(run).toString().getBytes()); // Run
					w.write('\n');
				}
			}

		}
	}

	public void toTable(Writer w) throws IOException {

		String dss = dataSetSize + "";
		for (int alg = 0; alg < algoritm.size(); alg++) {
			CoresRun c = algRuns.get(alg);
			for (int cores = 0; cores < c.getCores().size(); cores++) {
				String cs = cores + "";
				Runs r = c.getCores().get(cores);
				for (int run = 0; run < r.getRuns().size(); run++) {
					w.write(dataSetType); // Data set size
					w.write(',');
					w.write(dss); // Data set size
					w.write(',');
					w.write(algoritm.get(alg)); // Algorithm name
					w.write(',');
					w.write(cs); // Cores
					w.write(',');
					w.write(r.getRuns().get(run).toString()); // Run
					w.write('\n');
				}
			}

		}
	}

	public void toTableStepByStep(String baseNameOut, int alg, int cores) throws IOException {
		String dss = dataSetSize + "";
		CoresRun c = algRuns.get(alg);
		String cs = cores + "";
		Runs r = c.getCores().get(cores);
		File f = new File(baseNameOut);
		f.mkdirs();
		try (FileWriter fw = new FileWriter(baseNameOut + File.separator + baseNameOut + "_" + dataSetType + "_" + dss
				+ "_" + algoritm.get(alg) + "@" + cores + ".csv"); //
				BufferedWriter w = new BufferedWriter(fw)) {
			for (int run = 0; run < r.getRuns().size(); run++) {
				w.write(dataSetType); // Data set size
				w.write(',');
				w.write(dss); // Data set size
				w.write(',');
				w.write(algoritm.get(alg)); // Algorithm name
				w.write(',');
				w.write(cs); // Cores
				w.write(',');
				w.write(r.getRuns().get(run).toString()); // Run
				w.write('\n');
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String toString() {
		return "AlgoritmHistogram [algoritm=" + algoritm + ", algRuns=" + algRuns + "]";
	}
}
