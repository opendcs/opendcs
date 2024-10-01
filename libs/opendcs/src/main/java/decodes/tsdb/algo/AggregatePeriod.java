/**
 * 
 */
package decodes.tsdb.algo;

import java.util.Date;

/**
 * Holds the begin/end bounds for an aggregate period.
 */
public class AggregatePeriod
{
	private Date begin = null;
	private Date end = null;
	
	public AggregatePeriod(Date begin, Date end)
	{
		this.begin = begin;
		this.end = end;
	}

	/**
     * @return the begin
     */
    public Date getBegin()
    {
    	return begin;
    }

	/**
     * @param begin the begin to set
     */
    public void setBegin(Date begin)
    {
    	this.begin = begin;
    }

	/**
     * @return the end
     */
    public Date getEnd()
    {
    	return end;
    }

	/**
     * @param end the end to set
     */
    public void setEnd(Date end)
    {
    	this.end = end;
    }
}
