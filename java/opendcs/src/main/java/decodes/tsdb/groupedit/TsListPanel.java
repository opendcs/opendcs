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
package decodes.tsdb.groupedit;

import decodes.gui.TimeSeriesChartFrame;
import ilex.gui.JobDialog;
import ilex.util.AsciiUtil;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import opendcs.dai.TimeSeriesDAI;
import decodes.cwms.CwmsTsId;
import decodes.gui.TopFrame;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TimeSeriesIdentifier;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Displays a sorting-list of TimeSeries Data Descriptor objects in the
 * database.
 */
@SuppressWarnings("serial")
public class TsListPanel extends JPanel implements TsListControllers
{
	private final static Logger log = OpenDcsLoggerFactory.getLogger();

	private String listTitle = "Time Series List";

	private BorderLayout borderLayout = new BorderLayout();
	private TsListControlsPanel controlsPanel;
	private JLabel jLabel1 = new JLabel();
	private TsListSelectPanel tsListSelectPanel;

	private TimeSeriesDb theDb = null;
	private TsListFrame myFrame;

	/** Constructor. */
	public TsListPanel(TsListFrame myFrame, TimeSeriesDb theDb)
	{
		this.myFrame = myFrame;
		this.theDb = theDb;
		tsListSelectPanel = new TsListSelectPanel(theDb, true, true);
		tsListSelectPanel.setMultipleSelection(true);
		controlsPanel = new TsListControlsPanel(this);

		guiInit();

	}

	/** Initializes GUI components. */
	private void guiInit()
	{
		this.setLayout(borderLayout);
		jLabel1.setHorizontalAlignment(SwingConstants.CENTER);
		jLabel1.setText(listTitle);
		this.add(controlsPanel, BorderLayout.SOUTH);
		this.add(jLabel1, BorderLayout.NORTH);
		this.add(tsListSelectPanel, BorderLayout.CENTER);

		tsListSelectPanel.tsIdListTable.addMouseListener(
			new MouseAdapter()
			{
				public void mouseClicked(MouseEvent e)
				{
					if (e.getClickCount() == 2)
					{
						openPressed();
					}
				}
			});

	}

	/** @return type of entity that this panel edits. */
	public String getEntityType()
	{
		return "TimeSeriesList";
	}

	/** Called when the 'Open' button is pressed. */
	public void openPressed()
	{
		TimeSeriesIdentifier tsid = tsListSelectPanel.getSelectedTSID();
		if (tsid == null)
		{
			myFrame.showError("Select Time Series ID in list, then press Open.");
			return;
		}
		TimeSeriesDb tsdb = myFrame.getTsDb();
		if (tsdb.isCwms() || tsdb.isOpenTSDB())
		{
			// before constructing a new one. See if this tsid is already being edited.
			if (!myFrame.makeEditPaneActive(tsid))
			{
				TsSpecEditPanel editPanel = new TsSpecEditPanel(myFrame);
				editPanel.setTsSpec((CwmsTsId)tsid);
				myFrame.addEditTab(editPanel, "" + tsid.getKey());
			}
		}
		else
			myFrame.showError("Open not implemented.");
	}

	/** Called when the 'New' button is pressed. */
	public void newPressed()
	{
		TsListNewDialog dlg = new TsListNewDialog(myFrame, this, theDb);

		TimeSeriesDb tsdb = myFrame.getTsDb();
		if (tsdb.isCwms() || tsdb.isOpenTSDB())
		{
			CwmsTsId tsid = (CwmsTsId)tsdb.makeEmptyTsId();
			TsSpecEditPanel editPanel = new TsSpecEditPanel(myFrame);
			editPanel.setTsSpec(tsid);
			myFrame.addEditTab(editPanel, "new-TSID");
		}
		else
		{
			// If a TSID is selected, use it to 'seed' the dialog.
			TimeSeriesIdentifier tsids[] = tsListSelectPanel.getSelectedTSIDs();
			if (tsids != null && tsids.length > 0)
				dlg.fillValues(tsids[0]);
			myFrame.launchDialog(dlg);

			if (dlg.okPressed)
			{
				tsListSelectPanel.addTsDd(dlg.getTsidCreated());
			}
		}
	}

	/** Called when the 'Delete' button is pressed. */
	public void deletePressed()
	{
		final TimeSeriesIdentifier tsids[] =
			tsListSelectPanel.getSelectedTSIDs();

		if (tsids == null || tsids.length == 0)
			return;

		String msg = "" + tsids.length + " Time Series Selected. "
			+ " All data and references for these time series will be "
			+ "permanently deleted. Are you sure?";

		int ok = JOptionPane.showConfirmDialog(this,
			AsciiUtil.wrapString(msg, 60));
		if (ok != JOptionPane.YES_OPTION)
			return;

		final JobDialog dlg =
			new JobDialog(myFrame, "Deleting Time Series", true);
		dlg.setCanCancel(true);

		Thread backgroundJob =
			new Thread()
			{
				public void run()
				{
					try { sleep(2000L); } catch(InterruptedException ex) {}

					TimeSeriesDAI timeSeriesDAO = theDb.makeTimeSeriesDAO();
					try
					{
						for(TimeSeriesIdentifier tsid : tsids)
						{
							if (dlg.wasCancelled())
								break;
							// Make sure there are no external dependencies like a computation
							// or an alarm assertion.
							try
							{
								String msg = "Deleting " + tsid.getUniqueString();
								log.debug(msg);
								dlg.addToProgress(msg);
								timeSeriesDAO.deleteTimeSeries(tsid);
							}
							catch (DbIoException ex)
							{
								String msg = "Error: Can not delete Time Series '{}'";
								log.atError().setCause(ex).log(msg);
								dlg.addToProgress(msg.replace("{}", tsid.getUniqueString()) + ": " + ex);
							}
						}
					}
					finally { timeSeriesDAO.close(); }
					if (dlg.wasCancelled())
						dlg.addToProgress("Cancelled.");
					else
					{
						dlg.addToProgress("All done.");
						dlg.finishedJob();
					}
				}
			};
		backgroundJob.start();
		TopFrame.instance().launchDialog(dlg);

		refresh();
	}

	/** Called when the 'Refresh' button is pressed. */
	public void refresh()
	{
		tsListSelectPanel.refreshTSIDList();
	}

	@Override
	public void plot()
	{
		try
		{
			final TimeSeriesIdentifier[] tsids = tsListSelectPanel.getSelectedTSIDs();
			TimeSeriesChartFrame f = new TimeSeriesChartFrame(theDb,tsids);
			f.setVisible(true);
			f.plot();
		}
		catch (Exception ex)
		{
			log.atError().setCause(ex).log("Error reading time-series data");
		}

	}

	public Collection<String> getDistinctPart(String part)
	{
		return tsListSelectPanel.getDistinctPart(part);
	}
}
