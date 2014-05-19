/*
* $Id$
*
* $Log$
* Revision 1.1  2010/12/22 16:55:48  mmaloney
* udpated for groups
*
* Revision 1.2  2010/11/28 21:05:44  mmaloney
* Refactoring for CCP Time-Series Groups
*
* Revision 1.1  2008/06/30 14:10:48  cvs
* lots of changes
*
*/
package decodes.tsdb;

import decodes.util.CmdLineArgs;


/**
Makes (overwrites) the CP_COMP_DEPENDS table.
*/
public class MakeCpCompDepends extends TsdbAppTemplate
{
	public MakeCpCompDepends()
	{
		super(null);
	}

	/**
	 * Overrides to add test-specific arguments.
	 */
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
	}

	public static void main(String args[])
		throws Exception
	{
		TsdbAppTemplate tp = new MakeCpCompDepends();
		tp.execute(args);
	}

	protected void runApp()
		throws Exception
	{
		theDb.makeCpCompDepends();
	}
}
