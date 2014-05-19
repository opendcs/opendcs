/*
*  $Id$
*
*  $Log$
*  Revision 1.2  2010/08/17 04:39:05  gchen
*  Fix the bug with using the logarithmic algorithm when one of two rating table points has zero values.
*
*  Revision 1.1  2008/04/04 18:20:59  cvs
*  Added legacy code to repository
*
*  Revision 1.5  2007/07/17 15:12:43  mmaloney
*  dev
*
*  Revision 1.4  2004/08/24 14:31:28  mjmaloney
*  Added javadocs
*
*  Revision 1.3  2004/08/11 21:40:57  mjmaloney
*  Improved javadocs
*
*  Revision 1.2  2004/08/11 21:17:17  mjmaloney
*  dev
*
*  Revision 1.1  2004/06/30 20:01:51  mjmaloney
*  Isolated DECODES interface behind IDataCollection and ITimeSeries interfaces.
*
*/
package decodes.comp;

import java.text.NumberFormat;
import java.text.DecimalFormat;

/**
  Does a logarithmic interpolation between two points.
  Points can be set in the constructor or the setPoints method.
  The getY method may be called multiple times.
*/
public class LogInterp
{
	/** X of the lower of the two points. */
	private double x1;

	/** Y of the lower of the two points. */
	private double y1;

	/** X of the upper of the two points. */
	private double x2;

	/** Y of the upper of the two points. */
	private double y2;

	/** X-offset, used to flatten out the log-space. */
	private double off;

	/** Internally computed range of X. */
	private double rangeX;

	/** Internally computed range of Y. */
	private double rangeY;

	/** Computed Log of x1 */
	private double lx1;

	/** Computed Log of y1 */
	private double ly1;
	
	/** Overflow flag */
	private boolean overflowFlag;

	/** 
	  Constructs new logarithmic interpolator given X and Y range and offset. 
	  @param x1 X value of independent point
	  @param y1 Y value of independent point
	  @param x2 X value of dependent point
	  @param y2 Y value of dependent point
	  @param xOffset Log-space offset
	*/
	public LogInterp(double x1, double y1, double x2, double y2, double xOffset)
	{
		setPoints(x1, y1, x2, y2, xOffset);
	}

	/** 
	  Resets the interpolator with new end points. 
	  @param x1 the lower independent variable
	  @param y1 the upper independent variable
	  @param x2 the lower dependent variable
	  @param x2 the upper dependent variable
	  @param xOffset the offset
	*/
	public void setPoints(double x1, double y1, double x2, double y2, 
		double xOffset)
	{
		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;
		this.off = xOffset;
		
//GC Add on 2010/08/13
//Detect if there exists any overflow for (x1,y1) and (x2,y2) and
//use linear algorithm if overFlowFlag is set as true.
		overflowFlag = false;
		if ((y1 <= 0) || (y2 <= 0) || (x1-off <= 0) || (x2-off <= 0)) {
			overflowFlag = true;
		}

		// Do as much computation as possible up front.
		if (overflowFlag) {  //Using linear algorithm
			ly1 = y1;
			rangeY = y2 - ly1;
			lx1 = x1-off;
			rangeX = x2-off - lx1;
		}
		else {								//Using logarithmic algorithm
			ly1 = Math.log(y1);
			rangeY = Math.log(y2) - ly1;
			lx1 = Math.log(x1-off);
			rangeX = Math.log(x2-off) - lx1;
		}
	}

	/** 
	  @return computed Y given X.
	  @param x the independent variable
	*/
	public double getY(double x)
	{
    if (rangeX == 0) {
    	System.out.println("Error: rangeX = 0. Check if the rating table file has the same value for x.");
    	return -999999;
    }
		if (overflowFlag) {
			return (ly1 + ((rangeY * (x-off) - lx1)) / rangeX);
		}
		else {
			return Math.exp(ly1 + (rangeY * (Math.log(x-off) - lx1)) / rangeX);
		}
	}

// Test only method - Prints entire span in X increments of .01
	public void print()
	{
		NumberFormat nfx = new DecimalFormat("###.##");
		System.out.println("Interp from "+x1+","+y1 + " to " + x2 + "," + y2);
		for(double x = x1; x < x2; x += 0.01)
		{
			double y = getY(x);
			NumberFormat nfy = new DecimalFormat(
				y < 1.0 ? "###.##" : y < 10.0 ? "###.#" : "####");
			System.out.println("" + nfx.format(x) + ", " + nfy.format(y));
		}
	}


// Test main to print out various ranges
	public static void main(String args[])
		throws Exception
	{
		LogInterp li = new LogInterp(693.0, 99572.0, 694.0, 109186.0, 0.0);
		li.print();
		li = new LogInterp(694.0, 109186.0, 695.0, 118800.0, 0.0);
		li.print();
	}
}
