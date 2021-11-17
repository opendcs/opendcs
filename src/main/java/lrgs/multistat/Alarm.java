package lrgs.multistat;

import java.util.Date;

public class Alarm
{
	public String priority;
	public String source;
	public String date;
	public String module;
	public int alarmNum;
	public String text;
	public boolean isInstantaneous;
	public Date cancelledOn;
	public String cancelledBy;
	public int assertionCount;

	public Alarm(String priority, String source, String date, String module,
		int alarmNum, String text, boolean isInstantaneous)
	{
		this.priority = priority;
		this.source = source;
		this.date = date;
		this.module = module;
		this.alarmNum = alarmNum;
		this.text = text;
		this.isInstantaneous = isInstantaneous;
		this.cancelledOn = null;
		this.cancelledBy = null;
		this.assertionCount = 1;
	}

	public String toString()
	{
		if (isEmpty()) return "";
		return priority + " " + source + " " + date + " " 
			+ module + ":" + alarmNum + " " + text;
	}

	public boolean isEmpty()
	{
		return priority.length() == 0;
	}

}
