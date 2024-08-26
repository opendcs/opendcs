package decodes.tsdb;

import decodes.cwms.CwmsTsId;
import decodes.db.Constants;
import decodes.db.Site;
import decodes.db.SiteName;
import ilex.var.TimedVariable;
import opendcs.dai.TimeSeriesDAI;
import org.opendcs.utils.FailableResult;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class WaterTempProfiles{

    private TimeSeriesDAI timeSeriesDAO = null;

    /** The time series */
    public DataCollection tseries;

    private double startDepth;
    private double increment;

    /** Constructor -- builds an empty collection with a null handle. */
    public WaterTempProfiles(TimeSeriesDAI DAO, double start, double incr){
        tseries = new DataCollection();

        timeSeriesDAO = DAO;
        startDepth = start;
        increment = incr;
    }
    public WaterTempProfiles(ArrayList<CTimeSeries> profiles, TimeSeriesDAI DAO, double start, double incr){
        tseries = new DataCollection();
        for (CTimeSeries data : profiles){
            try {
                tseries.addTimeSeries(data);
            } catch (DuplicateTimeSeriesException e) {
                throw new RuntimeException(e);
            }
        }
        timeSeriesDAO = DAO;
        startDepth = start;
        increment = incr;
    }

    public WaterTempProfiles(TimeSeriesDAI DAO,String resID, String wtpId, Date since, Date until, double start, double incr) throws DbIoException, NoSuchObjectException {
        tseries = new DataCollection();
        timeSeriesDAO = DAO;
        startDepth = start;
        increment = incr;
        boolean loading = true;
        double currentDepth = startDepth;
        TimeSeriesIdentifier tsid;
        tsid = timeSeriesDAO.getTimeSeriesIdentifier(wtpId);

        while(loading){
            try {
                TimeSeriesIdentifier newtsid = tsid.copyNoKey();
                Site newsite =  new Site();
                newsite.copyFrom(tsid.getSite());
                SiteName strsite = newsite.getName(Constants.snt_CWMS);

                DecimalFormat decimalFormat = new DecimalFormat("000.0");
                String formattedNumber = decimalFormat.format(currentDepth).replace(".", ",");
                strsite.setNameValue(resID+"-D"+formattedNumber+"m");
                newtsid.setSite(strsite.site);
                newtsid.setSiteName(strsite.getDisplayName());
                FailableResult<TimeSeriesIdentifier, TsdbException> check = timeSeriesDAO.findTimeSeriesIdentifier(newtsid.getUniqueString());
                if(check.isSuccess()) {
                    CTimeSeries cts = timeSeriesDAO.makeTimeSeries(check.getSuccess());
//                    int n = timeSeriesDAO.fillTimeSeries(cts, since, until);
                    TimedVariable n = timeSeriesDAO.getPreviousValue(cts, until);
                    if (n == null) {
                        loading = false;
                    } else {
                        try {
                            tseries.addTimeSeries(cts);
                        } catch (DuplicateTimeSeriesException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    currentDepth += increment;
                }
                else if(check.getFailure() instanceof NoSuchObjectException){
                    loading = false;
                }
                else{
                    throw new DbIoException("failed to load time series from database", check.getFailure());
                }
            }
            catch (BadTimeSeriesException ex)
            {
                throw new DbIoException("error retrieving data for time series", ex);
            }
        }

    }

    public void SaveProfiles(){
        for (CTimeSeries tsery : tseries.getAllTimeSeries()) {
            try {
                timeSeriesDAO.saveTimeSeries(tsery);
            } catch (Exception ex) {
                String msg = "Error saving water temperature profile data: " + ex;
                //warning(msg);
                System.err.print(msg);
                ex.printStackTrace(System.err);
            }
        }
    }



}
