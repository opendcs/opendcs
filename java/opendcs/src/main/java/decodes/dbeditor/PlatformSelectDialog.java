/*
*  $Id$
*/
package decodes.dbeditor;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.border.*;

import java.util.ArrayList;
import java.util.ResourceBundle;

import ilex.util.LoadResourceBundle;
import decodes.db.Platform;
import decodes.db.Site;
import decodes.gui.TopFrame;

/**
Dialog for selecting one or more platforms.
Used by both Db Editor for import/export and for network list building.
*/
public class PlatformSelectDialog extends JDialog
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

    private JPanel panel1 = new JPanel();
    private JPanel jPanel1 = new JPanel();
    private FlowLayout flowLayout1 = new FlowLayout();
    private JButton selectButton = new JButton();
    private JButton cancelButton = new JButton();
    private BorderLayout borderLayout1 = new BorderLayout();
    private JPanel jPanel2 = new JPanel();
    private BorderLayout borderLayout2 = new BorderLayout();
    private TitledBorder titledBorder1;
    private Border border1;
    private PlatformSelectPanel selectPanel;
	private Platform plat;
	private boolean cancelled;

	public PlatformSelectDialog(JFrame owner, String mediumType)
	{
        super(owner, "", true);
		init(mediumType);
    }
	
	public PlatformSelectDialog(Site site, String mediumType )
    {
        super(TopFrame.instance(), "", true);
        selectPanel = new PlatformSelectPanel(this, site, mediumType);
       // init(mediumType);
        selectPanel.setParentDialog(this);
		plat = null;
        try 
		{
            jbInit();
			getRootPane().setDefaultButton(selectButton);
            pack();
        }
        catch(Exception ex) 
		{
            ex.printStackTrace();
        }
		cancelled = false;
    }

	public PlatformSelectDialog(JDialog owner, String mediumType)
	{
        super(owner, "", true);
		init(mediumType);
	}

	private void init(String mediumType)
	{
		selectPanel = new PlatformSelectPanel(mediumType);
		selectPanel.setParentDialog(this);
		plat = null;
        try 
		{
            jbInit();
			getRootPane().setDefaultButton(selectButton);
            pack();
        }
        catch(Exception ex) 
		{
            ex.printStackTrace();
        }
		cancelled = false;
	}

	
	/** Initialize GUI components. */
	void jbInit() throws Exception 
	{
        titledBorder1 = 
			new TitledBorder(BorderFactory.createLineBorder(
				new Color(153, 153, 153),2), 
				dbeditLabels.getString("PlatformSelectDialog.title"));
        border1 = BorderFactory.createCompoundBorder(
			titledBorder1,BorderFactory.createEmptyBorder(5,5,5,5));
        panel1.setLayout(borderLayout1);
        jPanel1.setLayout(flowLayout1);
        selectButton.setText(
			genericLabels.getString("select"));
        selectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectButton_actionPerformed(e);
            }
        });
        cancelButton.setText(
			genericLabels.getString("cancel"));
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancelButton_actionPerformed(e);
            }
        });
        flowLayout1.setHgap(35);
        flowLayout1.setVgap(10);
        this.setModal(true);
        this.setTitle(
			dbeditLabels.getString("PlatformSelectDialog.title"));
        jPanel2.setLayout(borderLayout2);
        jPanel2.setBorder(border1);
        getContentPane().add(panel1);
        panel1.add(jPanel1, BorderLayout.SOUTH);
        jPanel1.add(selectButton, null);
        jPanel1.add(cancelButton, null);
        panel1.add(jPanel2, BorderLayout.CENTER);
        jPanel2.add(selectPanel, BorderLayout.CENTER);
    }

	/**
	Called when selection is double clicked
*/

	public void openPressed() {
		plat = selectPanel.getSelectedPlatform();
		closeDlg();

	}

	/** 
	  Called when Select button is pressed. 
	  @param e ignored
	*/
    void selectButton_actionPerformed(ActionEvent e)
	{
		plat = selectPanel.getSelectedPlatform();
		closeDlg();
    }

	/** Closes dialog. */
	void closeDlg()
	{
		setVisible(false);
		dispose();
	}

	/** 
	  Called when Cancel button is pressed. 
	  @param e ignored
	*/
    void cancelButton_actionPerformed(ActionEvent e)
	{
		plat = null;
		cancelled = true;
		closeDlg();
    }

	/** @return selected (single) platform, or null if Cancel was pressed. */
	public Platform getSelectedPlatform()
	{
		// Will return null if none selected
		return plat;
	}

	/** @return selected (multiple) platforms, or empty array if none. */
	public Platform[] getSelectedPlatforms()
	{
		if (cancelled)
			return new Platform[0];
		return selectPanel.getSelectedPlatforms();
	}

	/** 
	  Called with true if multiple selection is to be allowed. 
	  @param ok true if multiple selection is to be allowed.
	*/
	public void setMultipleSelection(boolean ok)
	{
		selectPanel.setMultipleSelection(ok);
	}
}
