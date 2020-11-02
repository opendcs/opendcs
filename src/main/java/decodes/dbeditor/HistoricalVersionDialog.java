/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.2  2008/06/26 15:01:20  cvs
*  Updates for HDB, and misc other improvements.
*
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.9  2008/01/11 22:14:45  mmaloney
*  Internationalization
*  ~Dan
*
*  Revision 1.8  2004/09/20 14:18:47  mjmaloney
*  Javadocs
*
*  Revision 1.7  2001/12/03 13:17:13  mike
*  Fixed dialog-size bug.
*
*  Revision 1.6  2001/09/14 21:18:15  mike
*  dev
*
*  Revision 1.5  2001/05/03 02:14:39  mike
*  dev
*
*/
package decodes.dbeditor;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.Date;
import java.util.ResourceBundle;

import decodes.db.*;
import decodes.gui.GuiDialog;

/**
Dialog called from PlatformEditPanel when user presses the button to
make a historical version.
*/
public class HistoricalVersionDialog extends GuiDialog 
{
	
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();
	
    JPanel jPanel1 = new JPanel();
    JLabel jLabel1 = new JLabel();
    JLabel jLabel2 = new JLabel();
    JTextField platformNameField = new JTextField();
    JPanel jPanel2 = new JPanel();
    JLabel jLabel3 = new JLabel();
    JTextField expirationDateField = new JTextField();
    JLabel jLabel4 = new JLabel();
    JTextField expirationTimeField = new JTextField();
    JPanel jPanel3 = new JPanel();
    FlowLayout flowLayout1 = new FlowLayout(FlowLayout.CENTER, 35, 10);
    JButton createButton = new JButton();
    JButton cancelButton = new JButton();

    FlowLayout flowLayout2 = new FlowLayout();
    GridBagLayout gridBagLayout1 = new GridBagLayout();
    BorderLayout borderLayout1 = new BorderLayout();
    JTextArea explanationField = new JTextArea();
    JLabel jLabel6 = new JLabel();
    JLabel jLabel7 = new JLabel();

	Platform currentVersion;
	HistoricalVersionController myController;

	/**
	  Constructor.
	  @param currentVersion The platform opened in the PlatformEditPanel
	  @param the owning panel.
	*/
    public HistoricalVersionDialog(Platform currentVersion,
		HistoricalVersionController ctl)
	{
        super(getDbEditFrame(), "", true);
		myController = ctl;
		this.currentVersion = currentVersion;

        try {
            jbInit();
            pack();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

	/** JBuilder-generated method to initialize the GUI components */
    void jbInit() throws Exception {
        this.setModal(true);
        this.setTitle(  
    			dbeditLabels.getString("HistoricalVersionDialog.Title"));
        this.getContentPane().setLayout(borderLayout1);
        jPanel1.setLayout(flowLayout2);
        jLabel1.setHorizontalAlignment(SwingConstants.CENTER);
        jLabel1.setText(  
    			dbeditLabels.getString("HistoricalVersionDialog.CreateLabel"));
        jLabel2.setText(  
    			dbeditLabels.getString("HistoricalVersionDialog.PlatformLabel"));
        platformNameField.setBackground(Color.white);
        platformNameField.setPreferredSize(new Dimension(130, 21));
        platformNameField.setEditable(false);
        platformNameField.setText(currentVersion.makeFileName());
        jLabel3.setText("   " +   
    			dbeditLabels.getString("HistoricalVersionDialog.ExpirationDateLabel"));
        jPanel2.setLayout(gridBagLayout1);
        jLabel4.setText("   "+   
    			dbeditLabels.getString("HistoricalVersionDialog.ExpirationTimeLabel"));
        jPanel3.setLayout(flowLayout1);
        createButton.setText(   
    			dbeditLabels.getString("HistoricalVersionDialog.CreateButton"));
        createButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                createButton_actionPerformed(e);
            }
        });
        cancelButton.setText(
        			genericLabels.getString("cancel"));
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancelButton_actionPerformed(e);
            }
        });
        explanationField.setPreferredSize(new Dimension(474, 105));
        explanationField.setWrapStyleWord(true);
        explanationField.setLineWrap(true);
        explanationField.setBorder(BorderFactory.createEtchedBorder());
        explanationField.setMinimumSize(new Dimension(474, 105));
        explanationField.setText(   
    			dbeditLabels.getString("HistoricalVersionDialog.Explanation"));
        explanationField.setEditable(false);
        jLabel6.setText(   
    			dbeditLabels.getString("HistoricalVersionDialog.DateFormat"));
        jLabel7.setHorizontalAlignment(SwingConstants.LEFT);
        jLabel7.setText(   
    			dbeditLabels.getString("HistoricalVersionDialog.TimeFormat"));
        jPanel2.setMinimumSize(new Dimension(486, 200));
        jPanel2.setPreferredSize(new Dimension(486, 200));
        this.getContentPane().add(jPanel1, BorderLayout.NORTH);
        jPanel1.add(jLabel1, null);
        jPanel1.add(jLabel2, null);
        jPanel1.add(platformNameField, null);
        this.getContentPane().add(jPanel2, BorderLayout.CENTER);
        jPanel2.add(jLabel4, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.5
            ,GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, new Insets(2, 0, 2, 2), 0, 0));
        jPanel2.add(expirationTimeField, new GridBagConstraints(1, 2, 2, 1, 1.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 0), 0, 0));
        jPanel2.add(expirationDateField, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 0), 0, 0));
        jPanel2.add(jLabel3, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(4, 0, 2, 2), 0, 0));
        jPanel2.add(explanationField, new GridBagConstraints(0, 0, 4, 1, 0.0, 0.5
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(4, 6, 10, 6), 0, 0));
        jPanel2.add(jLabel6, new GridBagConstraints(2, 1, 2, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 4, 0, 6), 0, 0));
        jPanel2.add(jLabel7, new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 4, 0, 2), 0, 0));
        this.getContentPane().add(jPanel3, BorderLayout.SOUTH);
        jPanel3.add(createButton, null);
        jPanel3.add(cancelButton, null);
    }

	/** 
	  Called when Create button is pressed. 
	  @param e ignored
	*/
    void createButton_actionPerformed(ActionEvent e)
	{
		try
		{
			String dateStr = expirationDateField.getText() + " " +
	    		expirationTimeField.getText();
			Date expiration = Constants.defaultDateFormat.parse(dateStr);
	    	myController.makeHistoricalVersion(expiration);
			closeDlg();
		}
		catch(Exception ex)
		{
			showError(ex.toString());
		}
    }

	/** Closes the dialog. */
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
		closeDlg();
    }
}
