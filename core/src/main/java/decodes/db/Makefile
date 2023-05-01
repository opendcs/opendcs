#
#  $Id$
#
#  $Source$
#
#  $State$
#
#  $Log$
#  Revision 1.1  2008/04/04 18:21:00  cvs
#  Added legacy code to repository
#
#  Revision 1.38  2004/08/27 18:41:31  mjmaloney
#  Platwiz work
#
#  Revision 1.37  2003/11/15 20:06:53  mjmaloney
#  Removed DbConvert -- obsolete.
#
#  Revision 1.36  2003/10/20 20:22:53  mjmaloney
#  Database changes for DECODES 6.0
#
#  Revision 1.35  2002/09/19 17:18:02  mjmaloney
#  SQL dev.
#
#  Revision 1.34  2002/09/19 12:17:28  mjmaloney
#  SQL Updates.
#
#  Revision 1.33  2002/09/08 19:43:22  mjmaloney
#  Updates for 5.2
#
#  Revision 1.32  2002/08/26 04:53:45  chris
#  Major SQL Database I/O development.
#
#  Revision 1.31  2002/06/24 03:39:37  chris
#  Initial changes in support of the SQL database functionality.
#
#  Revision 1.30  2002/06/15 23:32:52  chris
#  Added javadoc
#
#  Revision 1.29  2002/05/18 21:04:38  mjmaloney
#  Deprecated decodes.db.TimeZone. Replaced by java.util.TimeZone.
#
#  Revision 1.28  2001/11/23 21:18:22  mike
#  dev
#
#  Revision 1.27  2001/11/21 21:19:06  mike
#  Implemented working DbInstall
#
#  Revision 1.26  2001/08/24 19:44:27  mike
#  Added Netlist2Lrgs
#
#  Revision 1.25  2001/08/12 17:36:54  mike
#  Slight architecture change for unit converters. The UnitConverterDb objects
#  are now full-fledged DatabaseObjects and not derived from UnitConverter.
#  This necessitated changes to DB parsing code and prepareForExec code.
#
#  Revision 1.24  2001/04/06 10:45:48  mike
#  SecurityException moved to util
#
#  Revision 1.23  2001/04/06 10:44:36  mike
#  dev
#
#  Revision 1.22  2001/04/01 17:23:25  mike
#  Added DataSourceList. HotBackupGroup moved to decoder package.
#
#  Revision 1.21  2001/04/01 17:01:10  mike
#  *** empty log message ***
#
#  Revision 1.20  2001/03/23 22:07:04  mike
#  Implemented PlatformConfigList & added it to Database class.
#
#  Revision 1.19  2001/03/23 20:22:53  mike
#  Collection classes are no longer static monostate. Access them through
#  the current database (Database.getDb().collectionName)
#
#  Revision 1.18  2001/03/23 20:09:25  mike
#  Collection classes are no longer monostate static collections.
#
#  Revision 1.17  2001/03/21 21:34:40  mike
#  Created SiteList. Add methods to Site constructor to add itself to SiteList.
#  Also, make name list private within a site. Add methods for defining new
#  name and retrieving names.
#
#  Revision 1.16  2001/03/20 22:55:56  mike
#  Implemented DatabaseArchive & parsers.
#
#  Revision 1.15  2001/03/20 03:43:24  mike
#  Implement final parsers
#
#  Revision 1.14  2001/03/18 18:24:35  mike
#  Implemented PerformanceMeasurments objects & parsers.
#
#  Revision 1.13  2001/03/17 18:53:06  mike
#  Created PMConfig & PMConfigList (performance measurements decoders)
#
#  Revision 1.12  2001/03/16 19:53:10  mike
#  Implemented XML parsers for routing specs
#
#  Revision 1.11  2001/03/15 21:36:35  mike
#  dev
#
#  Revision 1.10  2001/01/20 02:54:00  mike
#  dev
#
#  Revision 1.9  2001/01/13 14:59:33  mike
#  Implemented EU Conversions
#
#  Revision 1.8  2001/01/12 21:57:47  mike
#  dev
#
#  Revision 1.7  2001/01/12 21:53:38  mike
#  Renamed UnitConverter to UnitConverterDb
#
#  Revision 1.6  2001/01/12 15:38:20  mike
#  dev
#
#  Revision 1.5  2001/01/10 14:58:31  mike
#  Added EquationSpec classes & parsers.
#
#  Revision 1.4  2001/01/08 15:38:37  mike
#  dev
#
#  Revision 1.3  2000/12/29 02:42:52  mike
#  Created.
#
#  Revision 1.2  2000/12/28 14:02:35  mike
#  These moved to decodes/xml package.
#
#  Revision 1.1  2000/12/21 14:31:27  mike
#  Created.
#
#


# MJM 20020907 - Removed the PJAVA stuff
#
# CFM: Here are the "pre-java" files -- those that need to be run through
# the C-preprocessor.
# 
# PJSRC =  \
#     DataPresentation.pjava \
#     DataSource.pjava \
#     DataType.pjava \
#     DecodesScript.pjava \
#     Enum.pjava \
#     EnumValue.pjava \
#     EquipmentModel.pjava \
#     NetworkList.pjava \
#     Platform.pjava \
#     PlatformConfig.pjava \
#     PresentationGroup.pjava \
#     RoutingSpec.pjava \
#     Site.pjava \
#     UnitConverterDb.pjava
# 
# PJAVA = $(PJSRC:.pjava=.java)
# PJCLASS = $(PJSRC:.pjava=.class)
# 

JSRC = \
    Database.java \
	UnitConverterDb.java \
    CompositeConverter.java \
    ConfigSensor.java \
    Constants.java \
    DatabaseException.java \
    DatabaseIO.java \
    DatabaseObject.java \
    DataSourceList.java \
    DataTypeSet.java \
    DbInstall.java \
    EngineeringUnit.java \
    EngineeringUnitList.java\
    EnumList.java \
    FormatStatement.java \
    IncompleteDatabaseException.java \
    InvalidDatabaseException.java \
    LinearConverter.java \
    MakeUnitsTable.java \
    MarkProduction.java \
    Netlist2Lrgs.java \
    NetworkListEntry.java \
    NetworkListList.java \
    NoConversionException.java \
    NullConverter.java \
    PlatformConfigList.java \
    PlatformList.java \
    PlatformSensor.java \
    Poly5Converter.java \
    PresentationGroupList.java \
    RoundingRule.java \
    RoutingSpecList.java \
    ScriptSensor.java \
    SiteList.java \
    SiteName.java \
    TransportMedium.java \
    UnitConverter.java \
    UnitConverterSet.java \
    UsgsStdConverter.java \
    ValueAlreadyDefinedException.java \
    ValueNotFoundException.java \
	DataPresentation.java \
	DataSource.java \
	DataType.java \
	DecodesScript.java \
	Enum.java \
	EnumValue.java \
	EquipmentModel.java \
	EquipmentModelList.java \
	NetworkList.java \
	Platform.java \
	PlatformConfig.java \
	PresentationGroup.java \
	RoutingSpec.java \
	Site.java \
	IdDatabaseObject.java \
	IdRecordList.java


JCLASS = ${JSRC:.java=.class}

all: ${JCLASS}

include ${DECODESSRCROOT}/Makefile.Common

depends:
	touch make.depend

install:

