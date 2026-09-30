/*
* Where Applicable, Copyright 2026 OpenDCS Consortium and/or its contributors
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
package decodes.tsdb.procmonitor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import decodes.util.DecodesSettings;

/**
 * Displays a rolling two-hour history of the total CWMS computation queue.
 */
@SuppressWarnings("serial")
final class ComputationQueueChart extends JPanel
{
	static final long HISTORY_MILLIS = 2L * 60L * 60L * 1000L;
	static final long MINIMUM_DISPLAY_MILLIS = 5L * 60L * 1000L;
	private static final Dimension CHART_SIZE = new Dimension(220, 120);
	private final TimeSeries queueHistory =
		new TimeSeries("CCP Queue", Millisecond.class);

	ComputationQueueChart()
	{
		super(new BorderLayout());
		queueHistory.setMaximumItemAge(HISTORY_MILLIS);

		TimeZone displayTimeZone =
			TimeZone.getTimeZone(DecodesSettings.instance().guiTimeZone);
		SimpleDateFormat axisTimeFormat = new SimpleDateFormat("HH:mm");
		axisTimeFormat.setTimeZone(displayTimeZone);
		SimpleDateFormat tooltipTimeFormat = new SimpleDateFormat("HH:mm:ss");
		tooltipTimeFormat.setTimeZone(displayTimeZone);

		DateAxis timeAxis = new DateAxis("Time", displayTimeZone);
		timeAxis.setAutoRangeMinimumSize(MINIMUM_DISPLAY_MILLIS);
		timeAxis.setDateFormatOverride(axisTimeFormat);

		NumberAxis taskAxis = new NumberAxis("Tasks");
		taskAxis.setAutoRangeIncludesZero(true);
		taskAxis.setNumberFormatOverride(NumberFormat.getIntegerInstance());

		Font labelFont = taskAxis.getLabelFont().deriveFont(10.0f);
		Font tickFont = taskAxis.getTickLabelFont().deriveFont(9.0f);
		timeAxis.setLabelFont(labelFont);
		timeAxis.setTickLabelFont(tickFont);
		taskAxis.setLabelFont(labelFont);
		taskAxis.setTickLabelFont(tickFont);

		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
		renderer.setSeriesPaint(0, new Color(34, 104, 145));
		renderer.setToolTipGenerator(new StandardXYToolTipGenerator(
			"{1}: {2} tasks", tooltipTimeFormat,
			NumberFormat.getIntegerInstance()));

		XYPlot plot = new XYPlot(new TimeSeriesCollection(queueHistory),
			timeAxis, taskAxis, renderer);
		plot.setBackgroundPaint(Color.WHITE);
		plot.setDomainGridlinePaint(new Color(225, 225, 225));
		plot.setRangeGridlinePaint(new Color(225, 225, 225));

		JFreeChart chart = new JFreeChart(plot);
		chart.setBackgroundPaint(getBackground());
		ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(CHART_SIZE);
		chartPanel.setMinimumDrawWidth(0);
		chartPanel.setMinimumDrawHeight(0);
		chartPanel.setMouseZoomable(false);
		chartPanel.setDomainZoomable(false);
		chartPanel.setRangeZoomable(false);
		chartPanel.setPopupMenu(null);

		setBorder(BorderFactory.createEtchedBorder());
		add(chartPanel, BorderLayout.CENTER);
	}

	void addSample(long timestamp, long total)
	{
		queueHistory.addOrUpdate(new Millisecond(new Date(timestamp)),
			Long.valueOf(total));
	}

	int getSampleCount()
	{
		return queueHistory.getItemCount();
	}

	long getOldestSampleTime()
	{
		return queueHistory.getTimePeriod(0).getFirstMillisecond();
	}

	long getSampleValue(int index)
	{
		return queueHistory.getValue(index).longValue();
	}
}
