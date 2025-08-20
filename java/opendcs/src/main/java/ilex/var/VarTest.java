/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:10  cvs
*  Added legacy code to repository
*
*  Revision 1.5  2004/08/30 14:50:36  mjmaloney
*  Javadocs
*
*  Revision 1.4  2001/09/18 00:55:57  mike
*  First working DateDelegate.
*
*  Revision 1.3  2001/09/09 17:38:30  mike
*  Added CharDelegate & support functions.
*
*  Revision 1.2  2000/11/24 22:20:57  mike
*  Added support for boolean variables.
*
*  Revision 1.1  2000/11/17 14:14:21  mike
*  dev
*
*
*/
package ilex.var;

import java.util.Date;

/**
Various hard-coded tests for Variable class.
*/
public class VarTest
{
	/**
	* @param args
	*/
	public static void main( String[] args )
	{
		try
		{
			Variable intv = new Variable(5678);
			Variable strv = new Variable("String Variable");
			Variable flv  = new Variable(1.234);
			Variable blv  = new Variable(true);
			Variable chv  = new Variable('G');
			Variable dv  = new Variable(new Date());

			System.out.println("intv = " + intv);
			System.out.println("strv = " + strv);
			System.out.println("flv = " + flv);
			System.out.println("blv = " + blv);
			System.out.println("intv + strv = " + intv.plus(strv));
			System.out.println("intv + flv = " + intv.plus(flv));
			System.out.println("strv + intv = " + strv.plus(intv));
			System.out.println("strv + flv = " + strv.plus(flv));
			System.out.println("flv + intv = " + flv.plus(intv));
			System.out.println("flv + strv = " + flv.plus(strv));
			System.out.println("chv = '" + chv + "'");
			System.out.println("dv = '" + dv + "'");
		}
		catch(VariableException ve)
		{
			System.err.println(ve);
			ve.printStackTrace();
		}
	}
}


