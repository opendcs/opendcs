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
package decodes.gui;

import decodes.tsdb.CTimeSeries;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import opendcs.dai.TimeSeriesDAI;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import javax.swing.*;
import java.util.Calendar;

public class TimeSeriesChartFrame extends JFrame
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
    private final TimeSeriesDAI timeSeriesDAO;
    private final TimeSeriesIdentifier[] tsids;
    public TimeSeriesChartFrame(TimeSeriesDb db, TimeSeriesIdentifier[] tsids )
    {
        this.timeSeriesDAO = db.makeTimeSeriesDAO();
        this.tsids = tsids;
    }

    public void plot()
    {
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

        }
        catch(Exception ex)
        {
            log.atError().setCause(ex).log("Error plotting data.");
        }
    }

}
