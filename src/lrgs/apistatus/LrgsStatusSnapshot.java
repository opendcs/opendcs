package lrgs.apistatus;
public final class LrgsStatusSnapshot
{
	public int lrgsTime;
	public short currentHour;
	public int primaryMissingCount;
	public int totalRecoveredCount;
	public int totalGoodCount;
	public lrgs.apistatus.DownLink[] downLinks;
	public lrgs.apistatus.QualityMeasurement[] qualMeas;
	public lrgs.apistatus.ArchiveStatistics arcStats;
	public lrgs.apistatus.AttachedProcess[] attProcs;
	public LrgsStatusSnapshot(){}
	public LrgsStatusSnapshot(int lrgsTime, short currentHour, int primaryMissingCount, int totalRecoveredCount, int totalGoodCount, lrgs.apistatus.DownLink[] downLinks, lrgs.apistatus.QualityMeasurement[] qualMeas, lrgs.apistatus.ArchiveStatistics arcStats, lrgs.apistatus.AttachedProcess[] attProcs)
	{
		this.lrgsTime = lrgsTime;
		this.currentHour = currentHour;
		this.primaryMissingCount = primaryMissingCount;
		this.totalRecoveredCount = totalRecoveredCount;
		this.totalGoodCount = totalGoodCount;
		this.downLinks = downLinks;
		this.qualMeas = qualMeas;
		this.arcStats = arcStats;
		this.attProcs = attProcs;
	}
}
