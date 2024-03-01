package decodes.tsdb;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.slf4j.helpers.Util.getCallingClass;

import java.util.ArrayList;

import opendcs.dai.AlgorithmDAI;
import opendcs.dai.ComputationDAI;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.SiteDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dai.TsGroupDAI;
import ilex.cmdline.*;
import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.SiteName;
import decodes.util.CmdLineArgs;
import decodes.sql.DbKey;
import decodes.tsdb.xml.*;

/**
This is the Import program to read an xml file of comp meta data and
import it into the TSDB.
*/
public class ImportComp extends TsdbAppTemplate
{
    private final Logger log = LoggerFactory.getLogger(getCallingClass());
    private StringToken xmlFileArgs;
    private BooleanToken createTimeSeries;
    private SiteDAI siteDAO = null;
    private BooleanToken noOverwriteArg = new BooleanToken("o",
        "Do not overwrite records with matching name.", "", TokenOptions.optSwitch, false);

    //=======================================================================
    /**
     * Constructor called from main method after parsing arguments.
     */
    public ImportComp()
    {
        super("import.log");
        setSilent(true);
    }

    public void addCustomArgs(CmdLineArgs cmdLineArgs)
    {
        createTimeSeries = new BooleanToken("C", "create parms as needed",
            "", TokenOptions.optSwitch, false);
        cmdLineArgs.addToken(createTimeSeries);
        cmdLineArgs.addToken(noOverwriteArg);

        xmlFileArgs = new StringToken("", "xml-file",
            "", TokenOptions.optArgument | TokenOptions.optMultiple|
            TokenOptions.optRequired, "");
        cmdLineArgs.addToken(xmlFileArgs);
    }

    /**
     * The run method.
     */
    public void runApp( )
    {
        siteDAO = theDb.makeSiteDAO();
        LoadingAppDAI loadingAppDao = theDb.makeLoadingAppDAO();
        AlgorithmDAI algorithmDao = theDb.makeAlgorithmDAO();
        ComputationDAI computationDAO = theDb.makeComputationDAO();
        TsGroupDAI groupDAO = theDb.makeTsGroupDAO();

        try
        {
            CompXio cx = new CompXio("ImportComp", theDb);
            for (int i=0; i<xmlFileArgs.NumberOfValues(); i++)
            {
                String fn = xmlFileArgs.getValue(i);
                ArrayList<CompMetaData> metadata;
                try
                {
                    metadata = cx.readFile(fn);
                }
                catch(DbXmlException ex)
                {
                    log.atWarn()
                       .setCause(ex)
                       .log("Could not parse '{}'", fn);
                    continue;
                }

                //Write the TS groups from the metadata into the tmpTsGrpsList
                ArrayList<TsGroup> tmpTsGrpsList = new ArrayList<TsGroup>();
                for(CompMetaData mdobj: metadata)
                {
                    if (mdobj instanceof TsGroup)
                    {
                        tmpTsGrpsList.add((TsGroup)mdobj);
                    }
                }

                //Reorder the tmpTsGrpsList and
                //put all subgroups of a TS group in front of it
                ArrayList<TsGroup> tsGrpsList = sortTsGroupList(tmpTsGrpsList);

                //Write the TS groups into the DB
                if (tsGrpsList != null)
                {
                    for (TsGroup g : tsGrpsList)
                    {
                        // Lookup the time series unique string
                        lookupObject(g, LookupObjectType.TsUniqStr, groupDAO);

                        // Lookup the site ID
                        lookupObject(g, LookupObjectType.SiteId, groupDAO);

                        // Lookup the subgroup ID
                        lookupObject(g, LookupObjectType.InclSubgrp, groupDAO);
                        lookupObject(g, LookupObjectType.ExclSubgrp, groupDAO);
                        lookupObject(g, LookupObjectType.IntsSubgrp, groupDAO);

                        // Write each TS group into the DB
                        try
                        {
                            TsGroup existingGrp = groupDAO.getTsGroupByName(g.getGroupName());
                            if (existingGrp != null)
                            {
                                if (noOverwriteArg.getValue())
                                {
                                    log.info("Skipping group '{}' because a group with "
                                           + "that name already exists in your database.",g.getGroupName());

                                    continue;
                                }
                                g.setGroupId(existingGrp.getGroupId());
                            }
                            log.info("Importing group '{}'", g.getGroupName());
                            groupDAO.writeTsGroup(g);
                        }
                        catch (DbIoException ex)
                        {
                            log.atWarn()
                               .setCause(ex)
                               .log("Could not import object of type '{}' of name '{}'",
                                    g.getObjectType(), g.getObjectName());
                        }
                    }

                    //Import the app infos, the computations, and the algorithms
                    for(CompMetaData mdobj : metadata)
                    {
                        try
                        {
                            if (mdobj instanceof CompAppInfo)
                            {
                                CompAppInfo cai = (CompAppInfo)mdobj;
                                if (noOverwriteArg.getValue())
                                {
                                    try
                                    {
                                        loadingAppDao.getComputationApp(cai.getAppName());
                                        // If it doesn't throw NoSuchObject, that means it exists.
                                        log.info("Skipping process '{}' because a process with that name "
                                               + "already exists in your database.", cai.getAppName());
                                        continue;
                                    }
                                    catch(NoSuchObjectException ex) {}
                                }
                                log.info("Importing process '{}'", cai.getAppName());
                                loadingAppDao.writeComputationApp(cai);
                            }
                            else if (mdobj instanceof DbComputation)
                            {
                                DbComputation comp = (DbComputation)mdobj;
                                for(Iterator<DbCompParm> dcpi = comp.getParms(); dcpi.hasNext();)
                                {
                                    DbCompParm parm = dcpi.next();
                                    try
                                    {
                                        // Lookup the Site
                                        DbKey siteId = Constants.undefinedId;
                                        for(SiteName sn : parm.getSiteNames())
                                        {
                                            if ((siteId = siteDAO.lookupSiteID(sn)) != Constants.undefinedId)
                                            {
                                                break;
                                            }
                                        }
                                        if (siteId == Constants.undefinedId)
                                        {
                                            log.debug("Parm {} No site, assuming dynamic.", parm.getRoleName());
                                            continue;
                                        }
                                        parm.setSiteId(siteId);

                                        // Lookup the Data Type
                                        DataType dt = parm.getDataType();
                                        String dtCode = dt != null ? dt.getCode() : "";
                                        parm.setDataType(dt);

                                        // Lookup the Time Series
                                        try
                                        {
                                            theDb.setParmSDI(parm, siteId, dtCode);
                                        }
                                        catch (NoSuchObjectException ex)
                                        {
                                            log.info("Time Series for parm '{}' doesn't exist.", parm.getRoleName());
                                            if (!createTimeSeries.getValue())
                                            {
                                                log.warn("... and the -C (create TS) flag was not used.");
                                                throw ex;
                                            }
                                        }
                                        // get preferred name if one is provided.
                                        String nm = comp.getProperty(parm.getRoleName() + "_tsname");
                                        if (createTimeSeries.getValue())
                                        {
                                            TimeSeriesIdentifier tsid =
                                                theDb.transformTsidByCompParm(null, parm,
                                                    true, true, nm);
                                        }
                                    }
                                    catch(NoSuchObjectException ex)
                                    {
                                        String msg = "Computation '{}' problem resolving parameter {}";
                                        log.atWarn()
                                           .setCause(ex)
                                           .log(msg, comp.getName(), parm.getRoleName());
                                    }
                                    catch(BadTimeSeriesException ex)
                                    {
                                        if (!comp.hasGroupInput())
                                        {
                                            String msg = "Non-Group Computation '{}' problem resolving parameter {}";
                                                log.atWarn()
                                                .setCause(ex)
                                                .log(msg, comp.getName(), parm.getRoleName());
                                        }
                                    }
                                }
                                //Get the TS group ID
                                String tsGrpName = comp.getGroupName();
                                if (tsGrpName != null)
                                comp.setGroupId(groupDAO.getTsGroupByName(tsGrpName).getGroupId());

                                if (noOverwriteArg.getValue())
                                {
                                    try
                                    {
                                        computationDAO.getComputationByName(comp.getName());
                                        // If it doesn't throw NoSuchObject, that means it exists.
                                        log.info("Skipping computation '{}' because a computation with "
                                               + "that name already exists in your database.", comp.getName());
                                        continue;
                                    }
                                    catch(NoSuchObjectException ex) {}
                                }

                                log.info("Importing computation '{}'", comp.getName());
                                computationDAO.writeComputation(comp);
                            }
                            else if (mdobj instanceof DbCompAlgorithm)
                            {
                                DbCompAlgorithm algo = (DbCompAlgorithm)mdobj;
                                if (noOverwriteArg.getValue())
                                {
                                    try
                                    {
                                        algorithmDao.getAlgorithmId(algo.getName());
                                        // If it doesn't throw NoSuchObject, that means it exists.
                                        log.info("Skipping algorithm '{}' because an algorithm with "
                                               + "that name already exists in your database.", algo.getName());
                                        continue;
                                    }
                                    catch(NoSuchObjectException ex) {}
                                }

                                log.info("Importing algorithm '{}'", algo.getName());
                                algorithmDao.writeAlgorithm(algo);
                            }
                        }
                        catch(DbIoException ex)
                        {
                            String msg = "Could not import {} {}";
                            log.atWarn()
                               .setCause(ex)
                               .log(msg, mdobj.getObjectType(), mdobj.getObjectName());
                        }
                    }
                }   
            }
        }
        finally
        {
            groupDAO.close();
            siteDAO.close();
            algorithmDao.close();
            loadingAppDao.close();
            computationDAO.close();
        }
    }

    /**
     * Sort the TS Group List with searching subgroups for each TS group and
     * putting them in the front of each referring TS group.
     *
     * @param tsGrpsList: a TsGroup array list for sorting
     * @return ArrayList<TsGroup>: sorted TsGroup array list
     */
    protected ArrayList<TsGroup> sortTsGroupList(ArrayList<TsGroup> theTsGrpList)
    {
        if ((theTsGrpList == null) || (theTsGrpList.size() == 0))
        {
            return null;
        }

        ArrayList<TsGroup> retTsGrpList = new ArrayList<TsGroup>();
        ArrayList<TsGroup> searchTsGrpList = new ArrayList<TsGroup>();

        for (TsGroup tsGrp: theTsGrpList)
        {
            searchTsGrpList.clear();
            addTheTSGroup(tsGrp, theTsGrpList, retTsGrpList, searchTsGrpList);
        }

        theTsGrpList.clear();
        searchTsGrpList.clear();
        return retTsGrpList;
    }

     /**
      * Add a TS group object with its subgroups into retTsGrpList
      *
      * @param tsGrp
      * @param theTsGrpList
      * @param retTsGrpList
      * @param searchTsGrpList
      */
    private void addTheTSGroup(TsGroup tsGrp, ArrayList<TsGroup> theTsGrpList,
                               ArrayList<TsGroup> retTsGrpList, ArrayList<TsGroup> searchTsGrpList)
    {
        //tsGrp is null
        if (tsGrp == null)
        {
            return;
        }

        //tsGrp is found in retTsGrpList, so no need to add this object.
        if (findTsGroup(tsGrp, retTsGrpList) != null)
        {
            return;
        }

        TsGroup theFoundTsGrp = findTsGroup(tsGrp, theTsGrpList);
        //tsGrp is not found in theTsGrpList
        if (theFoundTsGrp == null)
        {
            return;
        }

        //If tsGrp appears in the searchTsGrpList, stop recursion
        if (searchTsGrpList != null)
        {
            if (findTsGroup(theFoundTsGrp, searchTsGrpList) != null)
            {
                return;
            }
            else
            {
                searchTsGrpList.add(theFoundTsGrp);
            }
        }

        //tsGrp is found in theTsGrpList, so do the following
        //Add theFoundTsGrp with its included subgroups into retTsGrpList
        for (TsGroup g: theFoundTsGrp.getIncludedSubGroups())
        {
            addTheTSGroup(g, theTsGrpList, retTsGrpList, searchTsGrpList);
        }

        //Add theFoundTsGrp with its excluded subgroups into retTsGrpList
        for (TsGroup g: theFoundTsGrp.getExcludedSubGroups())
        {
            addTheTSGroup(g, theTsGrpList, retTsGrpList, searchTsGrpList);
        }

        //Add theFoundTsGrp with its excluded subgroups into retTsGrpList
        for (TsGroup g: theFoundTsGrp.getIntersectedGroups())
        {
            addTheTSGroup(g, theTsGrpList, retTsGrpList, searchTsGrpList);
        }

        retTsGrpList.add(theFoundTsGrp);
    }

    private TsGroup findTsGroup(TsGroup tsGrp, ArrayList<TsGroup> theTsGrpList)
    {
        if ((tsGrp == null) || (theTsGrpList == null) || (theTsGrpList.size() == 0))
        {
            return null;
        }

        for (TsGroup g: theTsGrpList)
        {
            if (g.getGroupName().equals(tsGrp.getGroupName()))
            {
                return g;
            }
        }

        return null;
    }

    private enum LookupObjectType {TsUniqStr, SiteId, InclSubgrp, ExclSubgrp, IntsSubgrp };

    /**
     * Lookup if a certain object exists in the DB. If not, ignore it within the imported TS group
     *
     * @param tsGrp: TS group needs to be expanded for certain object
     * @param lookupObjType: a certain object type, TsUniqStr  - time series unique string;
     *                                              SiteId     - site ID;
     *                                              InclSubgrp - included subgroup
     *                                              ExclSubgrp - excluded subgroup
     */
    protected void lookupObject(TsGroup tsGrp, LookupObjectType lookupObjType, TsGroupDAI groupDAO)
    {
        ArrayList<Object> objList = new ArrayList<Object>();
        switch (lookupObjType)
        {
            case TsUniqStr:
            {
                for(String strObj: tsGrp.getTsMemberIDList())
                {
                    objList.add(strObj);
                }
                break;
            }
            case SiteId: 
            {
                for(String strObj: tsGrp.getSiteNameList())
                {
                    objList.add(strObj);
                }
                break;
            }
            case InclSubgrp:
            {
                for(TsGroup subGrp: tsGrp.getIncludedSubGroups())
                {
                    objList.add(subGrp);
                }
                break;
            }
            case ExclSubgrp:
            {
                for(TsGroup subGrp: tsGrp.getExcludedSubGroups())
                {
                    objList.add(subGrp);
                }
                break;
            }
            case IntsSubgrp:
            {
                for(TsGroup subGrp: tsGrp.getIntersectedGroups())
                {
                    objList.add(subGrp);
                }
                break;
            }
        }

        String msgStr;
        for(Object obj: objList)
        {
            TimeSeriesDAI timeSeriesDAO = null;
            try
            {
                switch (lookupObjType)
                {
                    case TsUniqStr:
                    {
                        timeSeriesDAO = theDb.makeTimeSeriesDAO();
                        msgStr = " time series unique string does not exist.";
                        TimeSeriesIdentifier objId =
                            timeSeriesDAO.getTimeSeriesIdentifier((String)obj);
                        if (objId != null)
                        {
                            tsGrp.addTsMember(objId);
                        }
                        else
                        {
                            System.out.println((String)obj + msgStr);
                        }
                        break;
                    }
                    case SiteId:
                    {
                        msgStr = "  site does not exist.";
                        DbKey objId = siteDAO.lookupSiteID((String)obj);
                        if (objId != Constants.undefinedId)
                        {
                            tsGrp.addSiteId(objId);
                        }
                        else
                        {
                            System.out.println((String)obj + msgStr);
                        }
                      break;
                    }
                    case InclSubgrp:
                    {
                        msgStr = " subgroup does not exist.";
                        TsGroup objId = groupDAO.getTsGroupByName(((TsGroup)obj).getGroupName());
                        if (objId != null)
                        {
                            ((TsGroup)obj).setGroupId(objId.getGroupId());
                        }
                        else
                        {
                            System.out.println(((TsGroup)obj).getGroupName() + msgStr);
                        }
                        break;
                    }
                    case ExclSubgrp:
                    {
                        msgStr = " subgroup does not exist.";
                        TsGroup objId = groupDAO.getTsGroupByName(((TsGroup)obj).getGroupName());
                        if (objId != null)
                        {
                            ((TsGroup)obj).setGroupId(objId.getGroupId());
                        }
                        else
                        {
                            System.out.println(((TsGroup)obj).getGroupName() + msgStr);
                        }
                        break;
                    }
                }
            }
            catch (Exception ex)
            {
                log.atWarn()
                   .setCause(ex)
                   .log("Error during object lookup for group.");
            }
            finally
            {
                if (timeSeriesDAO != null)
                {
                    timeSeriesDAO.close();
                }
            }
        }
    }

    /**
     * The main method.
     * @param args command line arguments.
     */
    public static void main( String[] args )
        throws Exception
    {
        // Call run method directly. For multi threaded executive, we would
        // create a thread and start it.
        ImportComp app = new ImportComp();
        app.execute(args);
    }
}
