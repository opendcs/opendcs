/*
 * This software was written by Cove Software, LLC ("COVE") under contract 
 * to the United States Government. 
 * 
 * No warranty is provided or implied other than specific contractual terms
 * between COVE and the U.S. Government
 * 
 * U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 */
package decodes.cwms.rating;

import hec.data.RatingException;
import ilex.util.AsciiUtil;
import ilex.util.EnvExpander;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.cwms.CwmsTimeSeriesDb;
import decodes.gui.TopFrame;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.DbIoException;

/**
 * Displays a sorting-list of Cwms Ratings in the Database
 */
@SuppressWarnings("serial")
public class CwmsRatingListPanel extends JPanel implements RatingController
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private BorderLayout borderLayout = new BorderLayout();
	private RatingControlsPanel controlsPanel;
	private JLabel panelTitle = new JLabel("CWMS Rating List");
	private CwmsRatingSelectPanel ratingSelectPanel;
	
	private TimeSeriesDb theDb = null;
	private TopFrame myFrame;
	static private JFileChooser fileChooser = new JFileChooser(
		EnvExpander.expand("$DECODES_INSTALL_DIR"));

	
	/** Constructor. */
	public CwmsRatingListPanel(TopFrame myFrame, TimeSeriesDb theDb)
		throws DbIoException
	{
		this.myFrame = myFrame;
		this.theDb = theDb;
		ratingSelectPanel = new CwmsRatingSelectPanel(theDb);
		ratingSelectPanel.setMultipleSelection(true);
		controlsPanel = new RatingControlsPanel(this);

		try
		{
			jbInit();
		}
		catch (Exception ex)
		{
			log.atError().setCause(ex).log("Unable to initialize GUI elements.");
		}
	}

	/** Initializes GUI components. */
	private void jbInit() throws Exception
	{
		this.setLayout(borderLayout);
		panelTitle.setHorizontalAlignment(SwingConstants.CENTER);
		this.add(controlsPanel, BorderLayout.SOUTH);
		this.add(panelTitle, BorderLayout.NORTH);
		this.add(ratingSelectPanel, BorderLayout.CENTER);
	}

	/** Called when the 'Delete' button is pressed. */
	public void deletePressed()
	{
		CwmsRatingRef rating = ratingSelectPanel.getSelectedRating();
		if (rating == null)
		{
			myFrame.showError("No rating selected!");
			return;
		}

		int ok = JOptionPane.showConfirmDialog(this, 
		AsciiUtil.wrapString("OK to delete rating '" + rating.toString() + "'?", 60));
		if (ok != JOptionPane.YES_OPTION)
			return;
		
		CwmsRatingDao crd = new CwmsRatingDao((CwmsTimeSeriesDb)theDb);
		try
		{
			crd.deleteRating(rating);
			refreshPressed();
		}
		catch (RatingException ex)
		{
			String msg = "Cannot delete rating '" + rating.toString()+ "': ";
			log.atError().setCause(ex).log(msg);
			myFrame.showError(msg);
		}
		finally
		{
			crd.close();
		}
	}

	/** Called when the 'Refresh' button is pressed. */
	public void refreshPressed()
	{
		try
		{
			ratingSelectPanel.refresh();
		}
		catch (DbIoException ex)
		{
			log.atError().setCause(ex).log("Unable to refresh ratings.");
		}
	}

	@Override
	public void exportXmlPressed()
	{
		CwmsRatingRef rating = ratingSelectPanel.getSelectedRating();
		if (rating == null)
			return;
		
		ExportDialog dlg = new ExportDialog(myFrame, theDb, rating);
		myFrame.launchDialog(dlg);
	}

	@Override
	public void importXmlPressed()
	{
		fileChooser.setSelectedFile(null);
		if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
			return;
		File f = fileChooser.getSelectedFile();
		CwmsRatingDao crd = new CwmsRatingDao((CwmsTimeSeriesDb)theDb);
		crd.setUseReference(false);
		BufferedReader rxIn = null;
		try
		{
			rxIn = new BufferedReader(new FileReader(f));
			StringBuilder xmlImage = new StringBuilder();
			String line = null;
			while ((line = rxIn.readLine()) != null)
				xmlImage.append(line + "\n");
			crd.importXmlToDatabase(xmlImage.toString());
		}
		catch (IOException ex)
		{
			myFrame.showError("Cannot read Rating from '" + f.getPath() + "': " + ex);
		}
		catch (RatingException ex)
		{
			myFrame.showError("Cannot write Rating to DB from '" + f.getPath() + "': " + ex);
		}
		finally
		{
			crd.close();
			if (rxIn != null)
				try { rxIn.close(); } catch(Exception ex) {}
		}
	}

	@Override
	public void searchUsgsPressed()
	{
		// TODO Auto-generated method stub
		
	}

}
