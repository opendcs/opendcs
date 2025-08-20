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
*  Revision 1.3  2004/08/30 14:50:35  mjmaloney
*  Javadocs
*
*  Revision 1.2  2000/11/24 16:00:18  mike
*  dev
*
*  Revision 1.1  2000/11/22 15:14:17  mike
*  dev
*
*/
package ilex.var;

/**
Test class for named variables.
*/
public class NVarTest
{
	/**
	* Performs several hard-coded tests.
	* @param args ignored.
	*/
	public static void main( String[] args )
	{
		try
		{
			NamedVariable intv = new NamedVariable("intv", 5678);
			NamedVariable strv = new NamedVariable("strv", "String Variable");
			NamedVariable flv  = new NamedVariable("flv", 1.234);
	
			System.out.println("intv = " + intv);
			System.out.println("strv = " + strv);
			System.out.println("flv = " + flv);
			System.out.println("intv + strv = " + intv.plus(strv));
			System.out.println("intv + flv = " + intv.plus(flv));
			System.out.println("strv + intv = " + strv.plus(intv));
			System.out.println("strv + flv = " + strv.plus(flv));
			System.out.println("flv + intv = " + flv.plus(intv));
			System.out.println("flv + strv = " + flv.plus(strv));

			NamedVariableList nvl = new NamedVariableList();
			nvl.add(intv);
			nvl.add(strv);
			nvl.add(flv);
			System.out.println("nvl: " + nvl);
		}
		catch(VariableException ve)
		{
			System.err.println(ve);
			ve.printStackTrace();
		}
	}
}


