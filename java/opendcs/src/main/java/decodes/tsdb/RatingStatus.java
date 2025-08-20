/*
*  $Id$
*/
package decodes.tsdb;

/**
 * Holds a double precision point-pair and a status string.
 * This is used to return the results when a rating is done inside a database
 * procedure.
 */
public class RatingStatus
{
	/**
	 * The dependent variable
	 */
	public double dep;
	
	/**
	 * The independent variable
	 */
	public double indep;
	
	/**
	 * The status returned from DB
	 */
	public String status;
	
	/**
	 * Constructs a rating point with i=independent and d=dependent variables.
	* @param i independent value
	* @param d dependent value
	 */
	public RatingStatus( double i, double d )
	{
		dep = d;
		indep = i;
		status = null;
	}
}
