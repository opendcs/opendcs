package decodes.tsdb;

import decodes.cwms.resevapcalc.MetComputation;
import decodes.db.Constants;
import decodes.db.Site;
import decodes.db.SiteName;
import ilex.var.TimedVariable;
import ilex.var.Variable;
import opendcs.dai.TimeSeriesDAI;
import org.opendcs.utils.FailableResult;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Represents a collection of water temperature profiles.
 * This class provides methods to initialize and manage a collection of water temperature profiles,
 * including loading profiles from a database, setting profiles at a specific time step, and saving profiles.
 */

final public class WaterTempProfiles
{

    /**
     * The time series
     */
    private DataCollection tseries;

    private double startDepth;
    private double increment;

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(WaterTempProfiles.class.getName());

    /**
     * Constructor -- builds an empty collection with a null handle.
     */
    public WaterTempProfiles(double start, double incr)
    {
        tseries = new DataCollection();
        startDepth = start;
        increment = incr;
    }

    public WaterTempProfiles(ArrayList<CTimeSeries> profiles, double start, double incr) throws DuplicateTimeSeriesException
    {
        tseries = new DataCollection();
        for (CTimeSeries data : profiles)
        {
            tseries.addTimeSeries(data);
        }
        startDepth = start;
        increment = incr;
    }

    //Initialize WaterTempProfiles with profiles from time slice previous to Until in database DAO connection
    public WaterTempProfiles(TimeSeriesDAI timeSeriesDAO, String resID, String wtpId, Date since, Date until, double start, double incr) throws DbCompException, DbIoException
    {
        tseries = new DataCollection();
        startDepth = start;
        increment = incr;
        boolean loading = true;
        double currentDepth = startDepth;
        TimeSeriesIdentifier tsid;
        try
        {
            tsid = timeSeriesDAO.getTimeSeriesIdentifier(wtpId);
        }
        catch (DbIoException | NoSuchObjectException ex)
        {
            throw new DbCompException("Failed to load timeSeries id: " + wtpId, ex);
        }

        while (loading)
        {
            try
            {
                TimeSeriesIdentifier newtsid = tsid.copyNoKey();
                Site newsite = new Site();
                newsite.copyFrom(tsid.getSite());
                SiteName strsite = newsite.getName(Constants.snt_CWMS);

                DecimalFormat decimalFormat = new DecimalFormat("000.0");
                String formattedNumber = decimalFormat.format(currentDepth).replace(".", ",");
                strsite.setNameValue(resID + "-D" + formattedNumber + "m");
                newtsid.setSite(strsite.site);
                newtsid.setSiteName(strsite.getDisplayName());
                FailableResult<TimeSeriesIdentifier, TsdbException> check = timeSeriesDAO.findTimeSeriesIdentifier(newtsid.getUniqueString());
                if (check.isSuccess())
                {
                    CTimeSeries cts = timeSeriesDAO.makeTimeSeries(check.getSuccess());
                    int n = timeSeriesDAO.fillTimeSeries(cts, since, until, true, true, true);
                    if (n == 0)
                    {
                        loading = false;
                    }
                    else
                    {
                        try
                        {
                            tseries.addTimeSeries(cts);
                        }
                        catch (DuplicateTimeSeriesException ex)
                        {
                            throw new DbCompException(ex.getMessage(), ex);
                        }
                    }
                    currentDepth += increment;
                }
                else if (check.getFailure() instanceof NoSuchObjectException)
                {
                    loading = false;
                }
                else
                {
                    throw new DbIoException("failed to find time series from database with TSID: " + newtsid.getUniqueString(), check.getFailure());
                }
            }
            catch (BadTimeSeriesException | NoSuchObjectException ex)
            {
                throw new DbIoException("error retrieving water temp profile data for time series", ex);
            }
        }

    }

    //Saves double[] of WTP to WTP object at time step, Stores data in CTimesSeries sets Flag  of timedVariable to T0_WRITE.
    public void setProfiles(double[] wtp, Date CurrentTime, String wtpTsId, String reservoirId, Double zeroElevation, Double Elev, TimeSeriesDAI timeSeriesDAO) throws DbCompException
    {
        double currentDepth = startDepth;
        for (int i = 0; i < wtp.length && currentDepth + (zeroElevation * 0.3048) <= Elev; i++)
        {
            if (i + 1 > tseries.size())
            {
                try
                {
                    TimeSeriesIdentifier tsid = timeSeriesDAO.getTimeSeriesIdentifier(wtpTsId);
                    TimeSeriesIdentifier newTSID = tsid.copyNoKey();

                    Site newsite = new Site();
                    newsite.copyFrom(newTSID.getSite());
                    SiteName strsite = newsite.getName(Constants.snt_CWMS);

                    DecimalFormat decimalFormat = new DecimalFormat("000.0");
                    String formattedNumber = decimalFormat.format(currentDepth).replace(".", ",");
                    strsite.setNameValue(reservoirId + "-D" + formattedNumber + "m");
                    newsite.addName(strsite);
                    newTSID.setSite(newsite);
                    newTSID.setSiteName(strsite.getDisplayName());

                    CTimeSeries CTProfile;
                    FailableResult<TimeSeriesIdentifier, TsdbException> check = timeSeriesDAO.findTimeSeriesIdentifier(newTSID.getUniqueString());
                    if (check.isSuccess())
                    {
                        CTProfile = timeSeriesDAO.makeTimeSeries(check.getSuccess());
                    }
                    else if (check.getFailure() instanceof NoSuchObjectException)
                    {
                        timeSeriesDAO.createTimeSeries(newTSID);
                        CTProfile = new CTimeSeries(newTSID);
                    }
                    else
                    {
                        throw new DbIoException("Database failed when attempting to find TSID: " + newTSID.getUniqueString(), check.getFailure());
                    }

                    TimedVariable newTV = new TimedVariable(new Variable(wtp[i]), CurrentTime);
                    newTV.setFlags(VarFlags.TO_WRITE);
                    CTProfile.addSample(newTV);
                    tseries.addTimeSeries(CTProfile);
                }
                catch (Exception ex)
                {
                    throw new DbCompException("failed to create new timeSeriesID " + wtpTsId + " at depth Meters:" + currentDepth, ex);
                }
            }
            else
            {
                CTimeSeries CTProfile = tseries.getTimeSeriesAt(i);
                TimedVariable newTV = new TimedVariable(new Variable(wtp[i]), CurrentTime);
                newTV.setFlags(VarFlags.TO_WRITE);
                CTProfile.addSample(newTV);
            }
            currentDepth += increment;
        }
    }

    public void SaveProfiles(TimeSeriesDAI timeSeriesDAO)
    {
        for (CTimeSeries tsery : tseries.getAllTimeSeries())
        {
            try
            {
                timeSeriesDAO.saveTimeSeries(tsery);
            }
            catch (Exception ex)
            {
                LOGGER.error("Error saving water temperature profile data", ex);
            }
        }
    }

    public DataCollection getTimeSeries()
    {
        return tseries;
    }


}
