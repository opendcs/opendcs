package decodes.cwms.validation;

/**
 * Check to make sure rate of change did not exceed a specified range.
 */
public class RocPerHourCheck
{
	private char flag;
	private double rise;
	private double fall;
	
	/**
	 * Constructor
	 * @param flag One of flagQuestion or flagReject (see ValidationConstants)
	 * @param rise The upper rate-of-change limit
	 * @param fall The lower rate-of-change limit, expressed as a positive number
	 */
	public RocPerHourCheck(char flag, double fall, double rise)
	{
		super();
		this.flag = flag;
		this.fall = fall;
		this.rise = rise;
	}

	public char getFlag()
	{
		return flag;
	}

	public double getRise()
	{
		return rise;
	}

	public double getFall()
	{
		return fall;
	}

	public String toString()
	{
		return "ROC/HR " + flag + " " + fall + " " + rise;
	}

	public void setRise(double rise)
	{
		this.rise = rise;
	}

	public void setFall(double fall)
	{
		this.fall = fall;
	}


}
