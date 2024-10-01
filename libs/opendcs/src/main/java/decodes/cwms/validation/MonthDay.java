package decodes.cwms.validation;

/**
 * Seasons in CWMS are represented as month and day, independent of timezone
 */
public class MonthDay
	implements Comparable<MonthDay>
{
	private int month = 0; // Jan = 1
	private int day = 0;
	
	public MonthDay(int month, int day)
	{
		super();
		this.month = month;
		this.day = day;
	}

	@Override
	public int compareTo(MonthDay rhs)
	{
		int x = this.month - rhs.month;
		if (x != 0)
			return x;
		return this.day - rhs.day;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof MonthDay))
			return false;
		return compareTo((MonthDay)obj) == 0;
	}

	public int getMonth()
	{
		return month;
	}

	public void setMonth(int month)
	{
		this.month = month;
	}

	public int getDay()
	{
		return day;
	}

	public void setDay(int day)
	{
		this.day = day;
	}
}
