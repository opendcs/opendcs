package lrgs.apistatus;
public final class QualityMeasurement
{
	public boolean containsData;
	public int numGood;
	public int numDropped;
	public int numRecovered;
	public QualityMeasurement(){}
	public QualityMeasurement(boolean containsData, int numGood, int numDropped, int numRecovered)
	{
		this.containsData = containsData;
		this.numGood = numGood;
		this.numDropped = numDropped;
		this.numRecovered = numRecovered;
	}
}
