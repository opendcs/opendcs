package decodes.tsdb.groupedit;

import ilex.gui.JobDialog;
import ilex.util.AsciiUtil;

import java.awt.BorderLayout;
import java.util.ResourceBundle;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import opendcs.dai.TimeSeriesDAI;

import decodes.gui.TopFrame;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TimeSeriesIdentifier;

/**
 * Displays a sorting-list of TimeSeries Data Descriptor objects in the
 * database.
 */
public class TsListPanel extends JPanel implements TsListControllers
{
	// Titles, Labels defined here for internationalization
	private String listTitle = "Time Series List";
//	private String openErrorMsg;
//	private String openErrorMsgEx;
//	private String deleteErrorMsg;
	private String editTabOpenError
		= "There is an editor tab open for this time-series. Close first.";
//	private String deleteConfirmMsg;
//	private String deleteConfirmMsg2;
//	private String tabNameUnknown;
//	private String deleteTsValMsg1;
//	private String deleteTsValMsg2;
//	private String alarmDeleteMsg;
	
	private BorderLayout borderLayout = new BorderLayout();
	private TsListControlsPanel controlsPanel;
	private JLabel jLabel1 = new JLabel();
	private TsListSelectPanel tsListSelectPanel;
//	private TsDbEditorFrame parent;
	private ResourceBundle labelDescriptions;
	private String module = "TsListPanel";
	
	private TimeSeriesDb theDb = null;
	private TopFrame myFrame;
	
	/** Constructor. */
	public TsListPanel(TopFrame myFrame, TimeSeriesDb theDb)
	{
		this.myFrame = myFrame;
		this.theDb = theDb;
		tsListSelectPanel = new TsListSelectPanel(theDb, true, true);
		tsListSelectPanel.setMultipleSelection(true);
//		parent = null;
		controlsPanel = new TsListControlsPanel(this);
//		labelDescriptions = labelDescriptionsIn;

		try
		{
			jbInit();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	/**
	 * Sets the parent frame object. Each list panel needs to know this.
	 * 
	 * @param parent
	 *            the TsDbEditorFrame
	 */
//	public void setParent(TsDbEditorFrame parent)
//	{
//		this.parent = parent;
//	}

	/** Initializes GUI components. */
	private void jbInit() throws Exception
	{
		this.setLayout(borderLayout);
		jLabel1.setHorizontalAlignment(SwingConstants.CENTER);
		jLabel1.setText(listTitle);
		this.add(controlsPanel, BorderLayout.SOUTH);
		this.add(jLabel1, BorderLayout.NORTH);
		this.add(tsListSelectPanel, BorderLayout.CENTER);
	}

	/** @return type of entity that this panel edits. */
	public String getEntityType()
	{
		return "TimeSeriesList";
	}

	/** Called when the 'Open' button is pressed. */
	public void openPressed()
	{
		myFrame.showError("Open not implemented.");
	}

	/** Called when the 'New' button is pressed. */
	public void newPressed()
	{
		myFrame.showError("New not implemented.");
	}

	/** Called when the 'Delete' button is pressed. */
	public void deletePressed()
	{
		final TimeSeriesIdentifier tsids[] = 
			tsListSelectPanel.getSelectedDataDescriptors();

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
								dlg.addToProgress("Deleting " + tsid.getUniqueString());
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
		tsListSelectPanel.refreshDataDescriptorList();
	}

	
	/**
	 * Verify is the given site name and param name combination
	 * exists in the current list or not
	 * 
	 * @param siteName
	 * @param paramName
	 * @return true if the site name and paramName combination
	 * exitst in the list, false otherwise
	 */
//	public boolean ddExistsInList(String siteName, String paramName)
//	{
//		return tsListSelectPanel.ddExistsInList(siteName, paramName);
//	}
	
	/**
	 * Make sure we do not have this combination in the DB already.
	 * 
	 * @param siteId
	 * @param dataTypeId
	 * @param intervalCode
	 * @param statisticsCode
	 * @return true if found a record with the save values, false othewise.
	 */
//	public boolean verifyConstraints(int siteId, int dataTypeId, 
//							String intervalCode, String statisticsCode)
//	{
//		return tsListSelectPanel.verifyConstraints(siteId, dataTypeId, 
//												intervalCode, statisticsCode);
//	}
}
