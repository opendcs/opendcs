#
#  $Id$
#
#  $State$
#
#  $Log$
#  Revision 1.1  2008/04/04 18:21:08  cvs
#  Added legacy code to repository
#
#  Revision 1.21  2004/08/30 14:49:31  mjmaloney
#  Added javadocs
#
#  Revision 1.20  2003/10/20 20:22:55  mjmaloney
#  Database changes for DECODES 6.0
#
#  Revision 1.19  2002/05/19 00:22:19  mjmaloney
#  Deprecated decodes.db.TimeZone and decodes.db.TimeZoneList.
#  These are now replaced by the java.util.TimeZone class.
#
#  Revision 1.18  2001/06/16 20:25:55  mike
#  dev
#
#  Revision 1.17  2001/06/12 14:14:29  mike
#  dev
#
#  Revision 1.16  2001/04/13 16:46:26  mike
#  dev
#
#  Revision 1.15  2001/04/11 22:52:38  mike
#  dev
#
#  Revision 1.14  2001/04/09 20:56:42  mike
#  Made TopLevelParser usable as a DECODES file parser - not just a test class.
#
#  Revision 1.13  2001/03/23 21:12:34  mike
#  Collection classes are no longer static. They are accessed via
#  Database.getDb().collectionName.
#
#  Revision 1.12  2001/03/20 03:43:25  mike
#  Implement final parsers
#
#  Revision 1.11  2001/03/18 18:24:36  mike
#  Implemented PerformanceMeasurments objects & parsers.
#
#  Revision 1.10  2001/03/16 22:21:07  mike
#  Added NetworkLists & corresponding parsers.
#
#  Revision 1.9  2001/03/16 19:53:18  mike
#  Implemented XML parsers for routing specs
#
#  Revision 1.8  2001/01/20 02:53:48  mike
#  Added EqTable Parser
#
#  Revision 1.7  2001/01/13 17:22:48  mike
#  Added parsers for EngineeringUnits
#
#  Revision 1.6  2001/01/10 14:58:33  mike
#  Added EquationSpec classes & parsers.
#
#  Revision 1.5  2001/01/08 22:14:22  mike
#  created DataTypeEquivalenceListParser
#
#  Revision 1.4  2001/01/03 02:54:59  mike
#  dev
#
#  Revision 1.3  2000/12/31 23:12:50  mike
#  dev
#
#  Revision 1.2  2000/12/29 02:50:03  mike
#  dev
#
#  Revision 1.1  2000/12/28 14:01:19  mike
#  First working version.
#
#

JSRC = TopLevelParser.java \
	EnumValueParser.java EnumParser.java EnumListParser.java \
	XmlDbTags.java EquipmentModelParser.java UnitConverterParser.java \
	ConfigSensorParser.java DecodesScriptParser.java SiteParser.java \
	PlatformConfigParser.java PlatformParser.java PlatformSensorParser.java \
	ScriptSensorParser.java TransportMediumParser.java \
	DataTypeEquivalenceListParser.java \
	EngineeringUnitParser.java EngineeringUnitListParser.java \
	DataSourceParser.java RoutingSpecParser.java \
	NetworkListEntryParser.java NetworkListParser.java \
	RoundingRuleParser.java DataPresentationParser.java \
	PresentationGroupParser.java \
	DatabaseParser.java \
	PlatformListParser.java XmlDatabaseIO.java \
	CreatePlatformXref.java

JCLASS = ${JSRC:.java=.class}

all: ${JCLASS}

include ${DECODESSRCROOT}/Makefile.Common

depends:
	touch make.depend

install:

