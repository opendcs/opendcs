package decodes.tsdb.groupedit;

import decodes.gui.TimeSeriesChart;
import decodes.gui.TimeSeriesChartFrame;
import decodes.gui.TimeSeriesLine;
import decodes.sql.DbKey;
import decodes.sql.PlatformListIO;
import decodes.tsdb.*;
import ilex.gui.JobDialog;
import ilex.util.AsciiUtil;
import ilex.util.Logger;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAmount;
import java.util.*;

import javax.swing.*;
import javax.swing.SwingUtilities;

import opendcs.dai.TimeSeriesDAI;
import decodes.cwms.CwmsTsId;
import decodes.gui.TopFrame;
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
			final TimeSeriesIdentifier[] tsids = tsListSelectPanel.getSelectedTSIDs();
			TimeSeriesChartFrame f = new TimeSeriesChartFrame(theDb,tsids);
			f.setVisible(true);
			f.plot();
		}catch (Exception e)
		{
			log.error("Error reading time-series data",e);
		}

	}

    @Override
    public void importts()
    {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Time Series Import Files");
        fileChooser.setMultiSelectionEnabled(true);

        // Set file filter for common import formats
        javax.swing.filechooser.FileNameExtensionFilter tsImportFilter =
            new javax.swing.filechooser.FileNameExtensionFilter(
                "TsImport Files (*.tsimport, *.txt)", "tsimport", "txt");
        javax.swing.filechooser.FileNameExtensionFilter allFilter =
            new javax.swing.filechooser.FileNameExtensionFilter("All Files", "*");

        fileChooser.addChoosableFileFilter(tsImportFilter);
        fileChooser.addChoosableFileFilter(allFilter);
        fileChooser.setFileFilter(tsImportFilter); // Set tsimport as default

        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION)
        {
            File[] selectedFilesArray = fileChooser.getSelectedFiles();

            // If no files selected, return
            if (selectedFilesArray.length == 0)
            {
                return;
            }

            try
            {
                decodes.util.DecodesSettings settings = decodes.util.DecodesSettings.instance();
                java.util.TimeZone tz = java.util.TimeZone.getTimeZone(settings.sqlTimeZone);

                // Scan all files to get all TSIDs
                Map<String, List<String>> foundTsIds = new HashMap<>();
                boolean found = false;
                for (File selectedFile : selectedFilesArray)
                {
                    String filePath = selectedFile.getAbsolutePath();
                    List<String> tsIds = new ArrayList<String>(TsImporter.scanForTsIds(filePath));
                    if (!tsIds.isEmpty()){
                        found = true;
                        foundTsIds.put(filePath, tsIds);
                    }
                }

                if (!found)
                {
                    JOptionPane.showMessageDialog(this,
                        "No time series identifiers found in the selected files.",
                        "No Data",
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // Show selection dialog
                TsImportMultiFileSelectionDialog selectionDialog = new TsImportMultiFileSelectionDialog(myFrame, foundTsIds);
                selectionDialog.setVisible(true);

                if (selectionDialog.isCancelled())
                {
                    return;
                }

                Map<String, List<String>> selectedTsIdsByFileName = selectionDialog.getSelectedTsIdsByFile();

                // Map file names back to full paths
                final Map<String, List<String>> filePathToSelectedTsIds = new HashMap<>();
                for (File file : selectedFilesArray)
                {
                    String fileName = file.getName();
                    if (selectedTsIdsByFileName.containsKey(fileName))
                    {
                        filePathToSelectedTsIds.put(file.getAbsolutePath(), selectedTsIdsByFileName.get(fileName));
                    }
                }

                // Collect all selected TSIDs for opening tabs later
                final Set<String> allSelectedTsIds = new HashSet<>();
                for (List<String> tsIds : filePathToSelectedTsIds.values())
                {
                    allSelectedTsIds.addAll(tsIds);
                }

                // Now import only the selected time series using TsImport
                final ilex.gui.JobDialog progressDlg = new ilex.gui.JobDialog(myFrame, "Importing Time Series", true);
                // Don't disable cancel button - we'll change it to "Done" when finished
                progressDlg.setCanCancel(true);

                SwingWorker<Integer, String> importWorker = new SwingWorker<Integer, String>()
                {
                    @Override
                    protected Integer doInBackground() throws Exception
                    {
                        publish("Importing " + allSelectedTsIds.size() + " time series from " + filePathToSelectedTsIds.size() + " file(s)");

                        int totalCount = 0;

                        // Process each file with its specific TSIDs
                        for (Map.Entry<String, List<String>> entry : filePathToSelectedTsIds.entrySet())
                        {
                            if (progressDlg.wasCancelled() || isCancelled())
                            {
                                break;
                            }

                            String filePath = entry.getKey();
                            List<String> tsIdsForThisFile = entry.getValue();
                            String fileName = new File(filePath).getName();

                            publish("Processing file: " + fileName);
                            publish("  Importing " + tsIdsForThisFile.size() + " time series...");

                            try
                            {
                                // Use TsImporter to import only the selected TSIDs from this specific file
                                int count = TsImporter.importTimeSeriesFile(theDb, filePath, tsIdsForThisFile,
                                    tz, settings.siteNameTypePreference);
                                totalCount += count;
                                publish("  Successfully imported " + count + " time series from " + fileName);
                            }
                            catch (Exception ex)
                            {
                                publish("  Error processing file " + fileName + ": " + ex.getMessage());
                                log.error("Error importing from file: " + fileName, ex);
                            }
                        }

                        publish("Successfully imported " + totalCount + " time series total.");
                        return totalCount;
                    }

                    @Override
                    protected void process(java.util.List<String> chunks)
                    {
                        // Add progress messages to the dialog
                        for (String message : chunks)
                        {
                            progressDlg.addToProgress(message);
                        }
                    }

                    @Override
                    protected void done()
                    {
                        try
                        {
                            Integer totalCount = get();

                            // Open each imported TSID in the GUI
                            for (String tsIdStr : allSelectedTsIds)
                            {
                                try
                                {
                                    // Get the TimeSeriesIdentifier from the database
                                    try (opendcs.dai.TimeSeriesDAI timeSeriesDAO = theDb.makeTimeSeriesDAO())
                                    {
                                        TimeSeriesIdentifier tsid = timeSeriesDAO.getTimeSeriesIdentifier(tsIdStr);

                                        // Check if this TSID is already being edited
                                        if (!myFrame.makeEditPaneActive(tsid))
                                        {
                                            // Open new edit panel for this TSID
                                            TsSpecEditPanel editPanel = new TsSpecEditPanel(myFrame);
                                            editPanel.setTsSpec((CwmsTsId)tsid);
                                            myFrame.addEditTab(editPanel, "" + tsid.getKey());
                                        }
                                    }
                                }
                                catch (Exception ex)
                                {
                                    log.atWarn().setCause(ex).log("Could not open edit panel for TSID '{}'", tsIdStr);
                                }
                            }
                        }
                        catch (Exception ex)
                        {
                            progressDlg.addToProgress("Error: " + ex.getMessage());
                            log.error("Error importing time series", ex);
                        }
                        finally
                        {
                            progressDlg.finishedJob();
                            progressDlg.setCanCancel(true); // Enable the "Done" button
                        }
                    }
                };

                importWorker.execute();
                myFrame.launchDialog(progressDlg);

                // Refresh the list to show newly imported time series
                refresh();
            }
            catch (Exception ex)
            {
                String msg = "Error reading import file";
                JOptionPane.showMessageDialog(this, msg + ": " + ex.getMessage(), "Import Error", JOptionPane.ERROR_MESSAGE);
                log.atError().setCause(ex).log(msg);
            }
        }
    }

	public Collection<String> getDistinctPart(String part)
	{
		return tsListSelectPanel.getDistinctPart(part);
	}
}
