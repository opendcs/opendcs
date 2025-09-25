/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
* 
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations 
* under the License.
*/
package decodes.tsdb;

import decodes.db.Constants;
import decodes.db.Site;
import decodes.db.SiteName;
import ilex.var.TimedVariable;
import ilex.var.Variable;
import opendcs.dai.TimeSeriesDAI;
import org.opendcs.utils.FailableResult;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

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
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
    /**
     * The time series
     */
    private DataCollection tseries;

    private double startDepth;
    private double increment;

    

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
    public WaterTempProfiles(TimeSeriesDAI timeSeriesDAO, String wtpId, Date since, Date until, double start, double incr) throws DbCompException, DbIoException
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

        String resID = tsid.getSiteName();

        int index = resID.lastIndexOf("-D");
        if (index != -1) {
            resID = resID.substring(0, index);
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
    public void setProfiles(double[] wtp, Date CurrentTime, String wtpTsId, Double zeroElevation, Double Elev, TimeSeriesDAI timeSeriesDAO) throws DbCompException
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

                    String reservoirId = tsid.getSiteName();

                    int index = reservoirId.lastIndexOf("-D");
                    if (index != -1) {
                        reservoirId = reservoirId.substring(0, index);
                    }

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
                        CTProfile = tseries.getTimeSeriesByTsidKey(check.getSuccess());
                        if (CTProfile == null)
                        {
                            CTProfile = timeSeriesDAO.makeTimeSeries(check.getSuccess());
                            tseries.addTimeSeries(CTProfile);
                        }
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
                log.atError().setCause(ex).log("Error saving water temperature profile data");
            }
        }
    }

    public void append(WaterTempProfiles wtp, Date appendTime, TimeSeriesDAI timeSeriesDAO)
    {
        if (tseries == null || wtp == null || wtp.getTimeSeries() == null)
        {
            return;
        }

        for (CTimeSeries tsery : wtp.getTimeSeries().getAllTimeSeries())
        {
            int idx = tsery.findNextIdx(appendTime);
            if (idx == -1)
            {
                break;
            }
            CTimeSeries existingSeries = tseries.getTimeSeriesByUniqueSdi(tsery.getSDI());
            if (existingSeries != null)
            {
                existingSeries.addSample(tsery.sampleAt(idx));
            }
            else
            {
                try
                {
                    CTimeSeries tseryCopy = timeSeriesDAO.makeTimeSeries(tsery.getTimeSeriesIdentifier());
                    tseryCopy.addSample(tsery.sampleAt(idx));
                    tseries.addTimeSeries(tseryCopy);
                }
                catch (DuplicateTimeSeriesException | NoSuchObjectException | DbIoException  ex)
                {
                    log.atError().setCause(ex).log("Error appending water temperature profile data");
                }
            }
        }
    }

    public DataCollection getTimeSeries()
    {
        return tseries;
    }


}
