package decodes.gui;

import decodes.tsdb.CTimeSeries;
import decodes.tsdb.TimeSeriesIdentifier;
import opendcs.dai.TimeSeriesDAI;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.Calendar;

public class TimeSeriesChartFrame extends JFrame
{
    private final static org.slf4j.Logger log = LoggerFactory.getLogger(TimeSeriesChartFrame.class);
    private final TimeSeriesDAI timeSeriesDAO;
    private final TimeSeriesIdentifier[] tsids;
    public TimeSeriesChartFrame(TimeSeriesDAI timeSeriesDAO, TimeSeriesIdentifier[] tsids )
    {
        this.timeSeriesDAO = timeSeriesDAO;
        this.tsids = tsids;
    }

    public void plot(){
        try
        {
            Calendar calendar = Calendar.getInstance();
            java.util.Date t2 = calendar.getTime();
            calendar.add(Calendar.DAY_OF_YEAR, -7);
            java.util.Date t1 = calendar.getTime();

            TimeSeriesChart chart = new TimeSeriesChart("", "time", "");
            for (TimeSeriesIdentifier tsid : tsids)
            {
                log.debug("reading '{}'", tsid.getUniqueString());
                CTimeSeries series = new CTimeSeries(tsid);
                timeSeriesDAO.fillTimeSeries(series, t1, t2);
                chart.addLine(new TimeSeriesLine(series));

            }
            JPanel chartPanel = chart.generateChart();
            chartPanel.setVisible(true);
            add(chartPanel);
            pack();
            setVisible(true);

        }catch(Exception e)
        {
            log.atError().log("Error plotting data ",e);
        }
    }

}
