package sorts;

public interface Sortable {

	public int[] sort(int[] in);

	public int[] sortThreaded(int version, int[] in, int threads) throws Exception;
}
