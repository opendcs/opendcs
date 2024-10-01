/**
 * $Id$
 * 
 * Copyright 2015 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * 
 * $Log$
 */
package decodes.cwms.validation.gui;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;

import decodes.cwms.validation.Screening;
import decodes.gui.TopFrame;
import decodes.tsdb.TimeSeriesDb;

public class ScreeningEditFrame extends TopFrame
{
	private JTabbedPane mainTabbedPane = null;
	private JLabel statusBar = new JLabel("");
	private ScreeningIdListTab screeningIdListTab = null;
	private TsidAssignTab tsidAssignTab = null;
	private TimeSeriesDb theDb = null;
	
	public ScreeningEditFrame(TimeSeriesDb theDb)
	{
		super();
		this.theDb = theDb;
		setTitle("Screening Editor");
		guiInit();
		pack();
		
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(
			new WindowAdapter()
			{
				public void windowClosing(WindowEvent e)
				{
					closeFrame();
				}
			});
		trackChanges("ScreeningEditor");

	}
	
	private void guiInit()
	{
		JPanel contentPane = (JPanel)getContentPane();
		contentPane.setLayout(new BorderLayout());
		mainTabbedPane = new JTabbedPane();
		contentPane.add(statusBar, BorderLayout.SOUTH);
		contentPane.add(mainTabbedPane, BorderLayout.CENTER);

		screeningIdListTab = new ScreeningIdListTab(this);
		mainTabbedPane.add(screeningIdListTab, "Screening IDs");
		
		tsidAssignTab = new TsidAssignTab(this);
		mainTabbedPane.add(tsidAssignTab, "TS Assignments");
	}
	
	private void closeFrame()
	{
		if (checkForOpenEditPanels())
			return;
		if (exitOnClose) 
			System.exit(0);
		else
			dispose();
	}

	/**
	 * Checks to see if any edit panels are open that need to be saved.
	 * If so, an Error Dialog is shown and the panel is made visible.
	 * @return true if open edit panels exist, false if OK to exit.
	 */
	private boolean checkForOpenEditPanels()
	{
		// TODO Auto-generated method stub
		return false;
	}

	public static void main(String[] args)
	{
		ScreeningEditFrame theFrame = new ScreeningEditFrame(null);
		theFrame.setVisible(true);
	}

	public TimeSeriesDb getTheDb()
	{
		return theDb;
	}

	public TsidAssignTab getTsidAssignTab()
	{
		return tsidAssignTab;
	}

	public ScreeningIdListTab getScreeningIdListTab()
	{
		return screeningIdListTab;
	}

	/**
	 * Called to open a ScreeningEditTab for the passed screening.
	 * If a tab for this screening is already open, bring it to the fore.
	 * Otherwise create a new one and add it to the tabbed pane.
	 * @param scr
	 */
	public void open(Screening scr)
	{
		for(int idx = 2; idx < mainTabbedPane.getTabCount(); idx++)
		{
			ScreeningEditTab editTab = (ScreeningEditTab)mainTabbedPane.getComponentAt(idx);
			if (scr == editTab.getScreening())
			{
				mainTabbedPane.setSelectedComponent(editTab);
				return;
			}
		}
		ScreeningEditTab editTab = new ScreeningEditTab(this);
		editTab.setScreening(scr);
		mainTabbedPane.add(editTab, scr.getScreeningName());
		mainTabbedPane.setSelectedComponent(editTab);
	}

	public void setTabLabel(ScreeningEditTab screeningEditTab, String newName)
	{
		for(int idx = 2; idx < mainTabbedPane.getTabCount(); idx++)
			if (screeningEditTab == mainTabbedPane.getComponentAt(idx))
			{
				mainTabbedPane.setTitleAt(idx, newName);
				return;
			}
	}

	public void closeScreening(ScreeningEditTab screeningEditTab)
	{
		mainTabbedPane.remove(screeningEditTab);
		screeningIdListTab.refresh();
		mainTabbedPane.setSelectedIndex(0);
	}

}
