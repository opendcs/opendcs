package decodes.decoder;

public class SampleComparator implements java.util.Comparator<TSSample>
{
	public boolean descending = false;
	
	public int compare(TSSample ts1, TSSample ts2)
	{
		long r = ts1.tv.getTime().getTime() - ts2.tv.getTime().getTime();
		int ret = r < 0L ? -1 : r > 0L ? 1 : 0;
		return descending ? -ret : ret;
	}

	public boolean equals(Object obj)
	{
		return obj == this;
	}
}
