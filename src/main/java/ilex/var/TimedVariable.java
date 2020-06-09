/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*/
package ilex.var;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
* TimedVariable extends the functionality of ilex.var.Variable by adding
* a time-stamp.
*/
public class TimedVariable extends Variable
{
	private Date timeStamp;
	private static DateFormat dateFormat;
	private int lineNum;

	static
	{
		dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	}

	/**
	* Sets the static DateFormat for String conversions.
	* @param df the DateFormat
	*/
	public static void setDateFormat( DateFormat df ) { dateFormat = df; }

	/** Default constructor. */
	public TimedVariable( )
	{
		super();
		timeStamp = new Date(0L);
		lineNum = 0;
	}
	
	/** Convenience constructor. Most often used this way. */
	public TimedVariable(Date t, double v, int f)
	{
		super(v);
		timeStamp = t;
		setFlags(f);
	}

	/**
	* Copy constructor.
	* @param nv the variable to copy
	*/
	public TimedVariable( TimedVariable nv )
	{
		super(nv);
		timeStamp = new Date(nv.timeStamp.getTime());
	}

	/**
	* Constructs timed variable from simple variable.
	* @param v the variable
	*/
	public TimedVariable( Variable v )
	{
		super(v);
		timeStamp = new Date(0L);
	}

	/**
	* Constructs timed variable from simple variable and a Date.
	* @param v the variable
	* @param d the Date
	*/
	public TimedVariable(Variable v, Date d)
	{
		super(v);
		timeStamp = d;
	}

	/**
	* Constructs a new byte variable
	* @param v the value
	*/
	public TimedVariable( byte v )
	{
		this(new Variable(v));
	}

	/** Constructs a new byte array variable */
/*
	public TimedVariable(byte[] v)
	{
		this(new Variable(v));
	}
*/

	/**
	* Constructs a new integer variable
	* @param v the value
	*/
	public TimedVariable( int v )
	{
		this(new Variable(v));
	}

	/** Constructs a new integer array variable */
/*
	public TimedVariable(int v[])
	{
		this(new Variable(v));
	}
*/

	/**
	* Constructs a new long integer variable
	* @param v the value
	*/
	public TimedVariable( long v )
	{
		this(new Variable(v));
	}

	/** Constructs a new long integer array variable */
/*
	public TimedVariable(long[] v)
	{
		this(new Variable(v));
	}
*/

	/**
	* Constructs a new floating point variable
	* @param v the value
	*/
	public TimedVariable( float v )
	{
		this(new Variable(v));
	}

	/** Constructs a new float array variable */
/*
	public TimedVariable(float[] v)
	{
		this(new Variable(v));
	}
*/

	/**
	* Constructs a new double variable
	* @param v the value
	*/
	public TimedVariable( double v )
	{
		this(new Variable(v));
	}

	/** Constructs a new double array variable */
/*
	public TimedVariable(double[] v)
	{
		this(new Variable(v));
	}
*/

	/**
	* Constructs a new string variable
	* @param v the value
	*/
	public TimedVariable( String v )
	{
		this(new Variable(v));
	}

	/**
	* @return
	*/
	public Date getTime( ) { return timeStamp; }

	/**
	* Sets the time stamp for this variable.
	* @param time the time stamp
	*/
	public void setTime( Date time ) 
	{
		if (time == timeStamp)
			return;
		timeStamp.setTime(time.getTime()); 
	}

	/**
	* @return true if the time-stamp for this variable has been set.
	*/
	public boolean timeIsSet( ) { return timeStamp.getTime() != 0L; }

	/**
	* @return a string of the form "date: value".
	*/
	public String toString( )
	{
		return timeString() + ": " + valueString();
	}

	/**
	* @return value as a String.
	*/
	public String valueString( )
	{
		return super.toString();
	}

	/**
	* @return timestamp as a string.
	*/
	public String timeString( )
	{
		return dateFormat.format(timeStamp);
	}

	/**
	 * Sets the line number for this variable.
	 * Line number is used by decodes to store the source line number
	 * within a message or file.
	 * @param lineNum the line number.
	 */
	public void setLineNumber(int lineNum) { this.lineNum = lineNum; }

	/**
	 * @return the line number for this variable (0 means not set).
	 */
	public int getLineNumber() { return lineNum; }
}

