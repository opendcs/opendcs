package decodes.tsdb.groupedit;

import decodes.gui.TimeSeriesChart;
import decodes.gui.TimeSeriesChartFrame;
import decodes.gui.TimeSeriesLine;
import decodes.sql.DbKey;
import decodes.sql.PlatformListIO;
import decodes.tsdb.CTimeSeries;
import ilex.gui.JobDialog;
import ilex.util.AsciiUtil;
import ilex.util.Logger;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAmount;
import java.util.Calendar;
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
import org.slf4j.LoggerFactory;

/**
 * Displays a sorting-list of TimeSeries Data Descriptor objects in the
 * database.
 */
@SuppressWarnings("serial")
public class TsListPanel 
	extends JPanel implements TsListControllers
{
	private final static org.slf4j.Logger log = LoggerFactory.getLogger(TsListPanel.class);

	private String listTitle = "Time Series List";
	
	private BorderLayout borderLayout = new BorderLayout();
	private TsListControlsPanel controlsPanel;
	private JLabel jLabel1 = new JLabel();
	private TsListSelectPanel tsListSelectPanel;
	private String module = "TsListPanel";
	
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

		try
		{
			guiInit();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	/** Initializes GUI components. */
	private void guiInit() throws Exception
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
								Logger.instance().debug1(msg);
								dlg.addToProgress(msg);
								timeSeriesDAO.deleteTimeSeries(tsid);
							}
							catch (DbIoException ex)
							{
								String msg = "Error: Can not delete Time Series '"
									+ tsid.getUniqueString() + "': " + ex;
								dlg.addToProgress(msg);
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
			TimeSeriesDAI dao = theDb.makeTimeSeriesDAO();
			final TimeSeriesIdentifier[] tsids = tsListSelectPanel.getSelectedTSIDs();
			TimeSeriesChartFrame f = new TimeSeriesChartFrame(dao,tsids);
			f.setVisible(true);
			f.plot();
		}catch (Exception e)
		{
			log.error("Error reading time-series data",e);
		}

	}

	public Collection<String> getDistinctPart(String part)
	{
		return tsListSelectPanel.getDistinctPart(part);
	}
}
