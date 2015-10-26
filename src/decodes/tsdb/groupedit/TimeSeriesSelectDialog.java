/**
 * $Id$
 * 
 * $Log$
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * Revision 1.4  2012/08/01 16:55:58  mmaloney
 * dev
 *
 * Revision 1.3  2012/08/01 16:40:03  mmaloney
 * dev
 *
 * Revision 1.2  2011/02/05 20:29:44  mmaloney
 * runcompgui for group selection bug fixes.
 *
 * Revision 1.1  2011/02/03 20:00:23  mmaloney
 * Time Series Group Editor Mods
 *
 */
package decodes.tsdb.groupedit;

import ilex.util.LoadResourceBundle;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.util.DecodesSettings;


/**
Dialog for selecting one or more Data Descriptor.
Used by the TsGroupDefinitionPanel.
*/
@SuppressWarnings("serial")
public class TimeSeriesSelectDialog extends JDialog
{
	private JPanel panel1 = new JPanel();
	private JPanel jPanel1 = new JPanel();
	private FlowLayout flowLayout1 = new FlowLayout();
	private JButton selectButton = new JButton();
	private JButton cancelButton = new JButton();
	
	private BorderLayout borderLayout1 = new BorderLayout();
	private JPanel jPanel2 = new JPanel();
	private BorderLayout borderLayout2 = new BorderLayout();
//	private TitledBorder titledBorder1;
//	private Border border1;
	private TsListSelectPanel ddSelectPanel;
	private TimeSeriesIdentifier dd;
	private boolean cancelled;
	
	//Labels for internationalization
	private String dialogTitle;
	private String panelTitle;
	
	/** Constructs new TsDataDescriptorSelectDialog */
	public TimeSeriesSelectDialog(TimeSeriesDb tsdbIn, boolean fillAll)
	{
	  	super(TsDbGrpEditorFrame.instance(), "", true);
	  	init(tsdbIn, fillAll);
	}
	
	public void setTimeSeriesList(Collection<TimeSeriesIdentifier> ddsIn)
	{
		ddSelectPanel.setTimeSeriesList(ddsIn);
	}

	private void init(TimeSeriesDb tsDb, boolean fillAll)
	{
		dd = null;
		setAllLabels();
		ddSelectPanel = new TsListSelectPanel(tsDb, true, fillAll);
		if (fillAll)
			ddSelectPanel.refreshDataDescriptorList();
        try {
            jbInit();
			getRootPane().setDefaultButton(selectButton);
            pack();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
		cancelled = false;
	}

	private void setAllLabels()
	{
		ResourceBundle groupResources = LoadResourceBundle.getLabelDescriptions(
			"decodes/resources/groupedit",
			DecodesSettings.instance().language);
		ResourceBundle genericResources = LoadResourceBundle.getLabelDescriptions(
			"decodes/resources/generic",
			DecodesSettings.instance().language);

		dialogTitle = 
			groupResources.getString("TsDataDescriptorSelectDialog.dialogTitle");
		panelTitle = 
			groupResources.getString("TsDataDescriptorSelectDialog.panelTitle");
		selectButton.setText(genericResources.getString("select"));
		cancelButton.setText(genericResources.getString("cancel"));
	}
	
	/** Initialize GUI components. */
	void jbInit() throws Exception {
//        titledBorder1 = 
//        	new TitledBorder(BorderFactory.createLineBorder(
//        			new Color(153, 153, 153),2), dialogTitle);
//        border1 = BorderFactory.createCompoundBorder(titledBorder1,
//        			BorderFactory.createEmptyBorder(5,5,5,5));
        panel1.setLayout(borderLayout1);
        jPanel1.setLayout(flowLayout1);
        selectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectButton_actionPerformed(e);
            }
        });
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancelButton_actionPerformed(e);
            }
        });
        flowLayout1.setHgap(35);
        flowLayout1.setVgap(10);
        this.setModal(true);
        this.setTitle(panelTitle);
        jPanel2.setLayout(borderLayout2);
//        jPanel2.setBorder(border1);
        getContentPane().add(panel1);
        panel1.add(jPanel1, BorderLayout.SOUTH);
        jPanel1.add(selectButton, null);
        jPanel1.add(cancelButton, null);
        panel1.add(jPanel2, BorderLayout.CENTER);
        jPanel2.add(ddSelectPanel, BorderLayout.CENTER);
    }

	/** 
	  Called when Select button is pressed. 
	  @param e ignored
	*/
    void selectButton_actionPerformed(ActionEvent e)
	{
    	dd = ddSelectPanel.getSelectedDataDescriptor();
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
		dd = null;
		cancelled = true;
		closeDlg();
    }

	/** @return selected (single) data descriptor, or null if Cancel was pressed. */
	public TimeSeriesIdentifier getSelectedDD()
	{
		// Will return null if none selected
		return dd;
	}

	/** @return selected (multiple) data descriptor, or empty array if none. */
	public TimeSeriesIdentifier[] getSelectedDataDescriptors()
	{
		if (cancelled)
			return new TimeSeriesIdentifier[0];
		return ddSelectPanel.getSelectedDataDescriptors();
	}
	
	public void setSelectedTS(TimeSeriesIdentifier tsid)
	{
		ddSelectPanel.setSelection(tsid);
	}

	/** 
	  Called with true if multiple selection is to be allowed. 
	  @param ok true if multiple selection is to be allowed.
	*/
	public void setMultipleSelection(boolean ok)
	{
		ddSelectPanel.setMultipleSelection(ok);
	}
	
	public void refresh()
	{
		ddSelectPanel.refreshDataDescriptorList();
	}
	
	public void clearSelection()
	{
		ddSelectPanel.clearSelection();
	}
}
