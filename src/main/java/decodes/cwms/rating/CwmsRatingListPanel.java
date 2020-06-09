/*
 * $Id$
 * 
 * $Log$
 * Revision 1.3  2017/02/16 14:41:26  mmaloney
 * Close CwmsRatingDao in final block.
 *
 * Revision 1.2  2016/09/29 18:54:36  mmaloney
 * CWMS-8979 Allow Database Process Record to override decodes.properties and
 * user.properties setting. Command line arg -Dsettings=appName, where appName is the
 * name of a process record. Properties assigned to the app will override the file(s).
 *
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * Revision 1.6  2012/11/12 18:59:48  mmaloney
 * refresh after delete
 *
 * Revision 1.5  2012/11/09 16:22:13  mmaloney
 * Rating GUI Delete Feature
 * Rating Import Merge Feature
 *
 * Revision 1.4  2012/10/30 18:40:52  mmaloney
 * dev
 *
 * Revision 1.3  2012/10/30 17:54:06  mmaloney
 * dev
 *
 * Revision 1.2  2012/10/30 15:46:37  mmaloney
 * dev
 *
 * Revision 1.1  2012/10/30 01:59:27  mmaloney
 * First cut of rating GUI.
 * 
 * This software was written by Cove Software, LLC ("COVE") under contract 
 * to the United States Government. 
 * 
 * No warranty is provided or implied other than specific contractual terms
 * between COVE and the U.S. Government
 * 
 * Copyright 2016 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * All rights reserved.
 */
package decodes.cwms.rating;

import hec.data.RatingException;
import ilex.gui.JobDialog;
import ilex.util.AsciiUtil;
import ilex.util.EnvExpander;
import ilex.util.Logger;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

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
			ex.printStackTrace();
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
			String msg = "Cannot delete rating '" + rating.toString()
				+ "': " + ex;
			Logger.instance().failure(msg);
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
		catch (DbIoException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
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
