/*
 *  $Id$
 */
package decodes.dbeditor;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.border.*;
import java.util.ResourceBundle;
import decodes.db.Site;
import decodes.gui.TopFrame;

/**
 * Dialog for selecting one or more sites. Used by both Db Editor and Platform
 * Wizard.
 */
@SuppressWarnings("serial")
public class SiteSelectDialog extends JDialog
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

	JButton selectButton = new JButton();
	JButton cancelButton = new JButton();

	JPanel listWrapperPanel = new JPanel(new BorderLayout());
	TitledBorder titledBorder1;
	SiteSelectPanel siteSelectPanel = null;
	private Site site;
	private boolean cancelled;

	/** Constructs new SiteSelectDialog */
	public SiteSelectDialog()
	{
		super(TopFrame.instance(), "", true);
		siteSelectPanel = new SiteSelectPanel();
		siteSelectPanel.setParentDialog(this);
		init();
	}

	public SiteSelectDialog(JPanel owner)
	{
		super(TopFrame.instance(), "", true);
		siteSelectPanel = new SiteSelectPanel();
		siteSelectPanel.setParentDialog(this);
		init();
	}

	public SiteSelectDialog(JFrame owner)
	{
		super(owner, "", true);
		siteSelectPanel = new SiteSelectPanel();
		siteSelectPanel.setParentDialog(this);
		init();
	}

	public SiteSelectDialog(JDialog owner)
	{
		super(owner, "", true);
		siteSelectPanel = new SiteSelectPanel();
		siteSelectPanel.setParentDialog(this);
		init();
	}

	/**
	 * Used by comp edit where 2 different owners must use the same site select
	 * panel.
	 */
	public SiteSelectPanel getSiteSelectPanel()
	{
		return siteSelectPanel;
	}

	public void setSiteSelectPanel(SiteSelectPanel ssp)
	{
		listWrapperPanel.remove(siteSelectPanel); // Remove old one
		siteSelectPanel = ssp;
		siteSelectPanel.setParentDialog(this);
		listWrapperPanel.add(siteSelectPanel, BorderLayout.CENTER);
	}

	private void init()
	{
		site = null;
		try
		{
			jbInit();
			getRootPane().setDefaultButton(selectButton);
			pack();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		cancelled = false;
	}

	/** Initialize GUI components. */
	void jbInit() throws Exception
	{
		this.setModal(true);
		this.setTitle(dbeditLabels.getString("SiteSelectDialog.title"));
		titledBorder1 = new TitledBorder(BorderFactory.createLineBorder(
			new Color(153, 153, 153), 2),
			dbeditLabels.getString("SiteSelectDialog.title"));
		Border border1 = BorderFactory.createCompoundBorder(titledBorder1,
			BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		JPanel mainPanel = new JPanel(new BorderLayout());
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
		selectButton.setText(genericLabels.getString("select"));
		selectButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				openPressed();
			}
		});
		cancelButton.setText(genericLabels.getString("cancel"));
		cancelButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				cancelButton_actionPerformed(e);
			}
		});
		buttonPanel.add(selectButton, null);
		buttonPanel.add(cancelButton, null);
		mainPanel.add(buttonPanel, BorderLayout.SOUTH);

		listWrapperPanel.setBorder(border1);
		getContentPane().add(mainPanel);
		mainPanel.add(listWrapperPanel, BorderLayout.CENTER);
		listWrapperPanel.add(siteSelectPanel, BorderLayout.CENTER);
	}

	/**
	 * Called when a double click on the selection
	 */

	void openPressed()
	{
		site = siteSelectPanel.getSelectedSite();
		closeDlg();
	}

	/** Closes dialog. */
	void closeDlg()
	{
		setVisible(false);
		dispose();
	}

	/**
	 * Called when Cancel button is pressed.
	 * 
	 * @param e
	 *            ignored
	 */
	void cancelButton_actionPerformed(ActionEvent e)
	{
		site = null;
		cancelled = true;
		closeDlg();
	}

	/** @return selected (single) site, or null if Cancel was pressed. */
	public Site getSelectedSite()
	{
		// Will return null if none selected
		return site;
	}

	/** @return selected (multiple) sites, or empty array if none. */
	public Site[] getSelectedSites()
	{
		if (cancelled)
			return new Site[0];
		return siteSelectPanel.getSelectedSites();
	}

	/**
	 * Called with true if multiple selection is to be allowed.
	 * 
	 * @param ok
	 *            true if multiple selection is to be allowed.
	 */
	public void setMultipleSelection(boolean ok)
	{
		siteSelectPanel.setMultipleSelection(ok);
	}

	public void clearSelection()
	{
		siteSelectPanel.clearSelection();
	}
}
