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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.DateAxis;
import org.junit.jupiter.api.Test;

final class ComputationQueueChartTest
{
	private static final long START_TIME = 1_700_000_000_000L;

	@Test
	void retainsOnlySamplesFromPastTwoHours()
	{
		ComputationQueueChart chart = new ComputationQueueChart();
		chart.addSample(START_TIME, 10L);
		chart.addSample(START_TIME + ComputationQueueChart.HISTORY_MILLIS, 20L);

		assertEquals(2, chart.getSampleCount());

		long newestTime =
			START_TIME + ComputationQueueChart.HISTORY_MILLIS + 1L;
		chart.addSample(newestTime, 30L);

		assertEquals(2, chart.getSampleCount());
		assertEquals(START_TIME + ComputationQueueChart.HISTORY_MILLIS,
			chart.getOldestSampleTime());
		assertEquals(30L, chart.getSampleValue(1));
	}

	@Test
	void timeAxisExpandsWithAvailableHistory()
	{
		ComputationQueueChart chart = new ComputationQueueChart();
		ChartPanel chartPanel = (ChartPanel)chart.getComponent(0);
		DateAxis timeAxis =
			(DateAxis)chartPanel.getChart().getXYPlot().getDomainAxis();

		chart.addSample(START_TIME, 10L);
		chart.addSample(START_TIME + 30L * 60L * 1000L, 20L);

		assertTrue(timeAxis.isAutoRange());
		assertEquals(ComputationQueueChart.MINIMUM_DISPLAY_MILLIS,
			(long)timeAxis.getAutoRangeMinimumSize());
		assertTrue(timeAxis.getRange().getLength()
			< ComputationQueueChart.HISTORY_MILLIS);
		assertTrue(timeAxis.getRange().getLength() > 30L * 60L * 1000L);
	}

	@Test
	void replacesARepeatedTimestamp()
	{
		ComputationQueueChart chart = new ComputationQueueChart();
		chart.addSample(START_TIME, 10L);
		chart.addSample(START_TIME, 25L);

		assertEquals(1, chart.getSampleCount());
		assertEquals(25L, chart.getSampleValue(0));
	}
}
