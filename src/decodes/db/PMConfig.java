/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.6  2004/08/25 19:31:15  mjmaloney
*  Added javadocs & deprecated unused code.
*
*  Revision 1.5  2001/04/21 20:19:23  mike
*  Added read & write methods to all DatabaseObjects
*
*  Revision 1.4  2001/04/18 13:18:51  mike
*  First complete XmlDatabaseIO interface.
*
*  Revision 1.3  2001/03/23 20:22:53  mike
*  Collection classes are no longer static monostate. Access them through
*  the current database (Database.getDb().collectionName)
*
*  Revision 1.2  2001/03/18 18:24:35  mike
*  Implemented PerformanceMeasurments objects & parsers.
*
*  Revision 1.1  2001/03/17 18:53:06  mike
*  Created PMConfig & PMConfigList (performance measurements decoders)
*
*/
package decodes.db;

/**
  Wrapper for PlatformConfig that decodes header information containing
  "Performance Measurments"
*/
//public class PMConfig extends PlatformConfig
//{
//	// Raw data
//	public String mediumType;
//
//	public PMConfig(String mediumType)
//	{
//		super();
//		this.mediumType = mediumType;
//		Database.getDb().pMConfigList.add(this);
//	}
//
//	/**
//	  Makes a string containing the suitable for use as a filename.
//	*/
//	public String makeFileName()
//	{
//		StringBuffer ret = new StringBuffer(mediumType);
//		for(int i=0; i<ret.length(); i++)
//			if (Character.isWhitespace(ret.charAt(i)))
//				ret.setCharAt(i, '-');
//		return ret.toString();
//	}
//
//	public void read()
//		throws DatabaseException
//	{
//		myDatabase.getDbIo().readPMConfig(this);
//	}
//
//	public void write()
//		throws DatabaseException
//	{
//		myDatabase.getDbIo().writePMConfig(this);
//	}
//}
//
