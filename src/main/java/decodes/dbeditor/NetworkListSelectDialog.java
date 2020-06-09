/*
*	$Id$
*
*	$Log$
*	Revision 1.3  2008/09/26 20:49:02  mjmaloney
*	Added <all> and <production> network lists
*	
*	Revision 1.2  2008/09/26 14:56:54  mjmaloney
*	Added <all> and <production> network lists
*	
*	Revision 1.1  2008/04/04 18:21:01  cvs
*	Added legacy code to repository
*	
*	Revision 1.5  2008/01/24 16:41:56  mmaloney
*	fixed files for internationalization
*	
*	Revision 1.4  2008/01/14 14:56:43  mmaloney
*	dev
*	
*	Revision 1.3  2004/09/20 14:18:48  mjmaloney
*	Javadocs
*	
*	Revision 1.2  2004/07/05 14:30:05  mjmaloney
*	Added network list selection dialog.
*	
*/
package decodes.dbeditor;

import java.util.Iterator;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.ResourceBundle;

import ilex.util.LoadResourceBundle;

import decodes.db.Database;
import decodes.db.NetworkList;

/**
Dialog for selecting network list names.
*/
public class NetworkListSelectDialog extends JDialog 
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

	JPanel panel1 = new JPanel();
	BorderLayout borderLayout1 = new BorderLayout();
	JPanel jPanel1 = new JPanel();
	JButton okButton = new JButton();
	FlowLayout flowLayout1 = new FlowLayout();
	JButton cancelButton = new JButton();
	JPanel jPanel2 = new JPanel();
	//JLabel jLabel1 = new JLabel();
	JPanel jPanel3 = new JPanel();
	JLabel jLabel2 = new JLabel();
	FlowLayout flowLayout2 = new FlowLayout();
	JComboBox networkListCombo = new JComboBox();

	private boolean _isOK = false;

	/** 
	  Constructor.
	  @param frame the parent frame
	  @param title the Title of this dialog
	  @param modal true if this is a modal dialog
	*/
	public NetworkListSelectDialog(Frame frame, String title, boolean modal) 
	{
		super(frame, title, modal);
		try {
			jbInit();
			pack();
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}

		fillValuesFromDatabase();
	}

	/** No Args constructor for JBuilder */
	public NetworkListSelectDialog() {
		this(null, "", false);
	}

	/** JBuilder-generated method to initialize GUI components. */
	private void jbInit() throws Exception {
		panel1.setLayout(borderLayout1);
		this.setModal(true);
		this.setTitle(
			dbeditLabels.getString("NetworkListSelectDialog.title"));
		okButton.setOpaque(true);
		okButton.setText(
			genericLabels.getString("OK"));
		okButton.addActionListener(new NetworkListSelectDialog_okButton_actionAdapter(this));
		jPanel1.setLayout(flowLayout1);
		flowLayout1.setHgap(20);
		cancelButton.setText(
			genericLabels.getString("cancel"));
		cancelButton.addActionListener(new NetworkListSelectDialog_cancelButton_actionAdapter(this));
		jLabel2.setText(
			dbeditLabels.getString("NetworkListSelectDialog.selectListPrompt"));
		jPanel3.setLayout(flowLayout2);
		networkListCombo.setPreferredSize(new Dimension(200, 19));
		getContentPane().add(panel1);
		panel1.add(jPanel1, BorderLayout.SOUTH);
		jPanel1.add(okButton, null);
		jPanel1.add(cancelButton, null);
		panel1.add(jPanel2, BorderLayout.NORTH);
		//jPanel2.add(jLabel1, null);
		panel1.add(jPanel3, BorderLayout.CENTER);
		jPanel3.add(jLabel2, null);
		jPanel3.add(networkListCombo, null);
	}

	private void fillValuesFromDatabase()
	{
        networkListCombo.addItem("<all>");
        networkListCombo.addItem("<production>");
		for(Iterator it = Database.getDb().networkListList.getList().iterator();
            it.hasNext(); )
        {
            NetworkList nl = (NetworkList)it.next();
			StringBuffer sb = new StringBuffer(nl.name);
			sb.append(' ');
			for(int i=sb.length(); i<16; i++)
				sb.append(' ');
			sb.append(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("NetworkListSelectDialog.numSites"),
					nl.size()));
            networkListCombo.addItem(sb.toString());
        }
	}

	/** 
	  Called when OK button is pressed. 
	  @param e ignored
	*/
	void okButton_actionPerformed(ActionEvent e) 
	{
		_isOK = true;
		closeDlg();
	}

	/** 
	  Called when Cancel button is pressed. 
	  @param e ignored
	*/
	void cancelButton_actionPerformed(ActionEvent e) 
	{
		_isOK = false;
		closeDlg();
	}

    /** Closes the dialog */
    void closeDlg()
    {
        setVisible(false);
        dispose();
    }
                                                                                
    /** @return true if dialog OK button was pressed. */
    public boolean okPressed() { return _isOK; }
                                                                                
    /** @return the selected network list name. */
    public String getSelection()
    {
        String s = (String)networkListCombo.getSelectedItem();
        if (s.equals("<all>") || s.equals("<production>"))
        	return s;
        int idx = s.indexOf(' ');
		if (idx != -1)
			return s.substring(0,idx);
		else return null;
    }
                                                                                
	/** 
	  Called prior to showing dialog to exclude names already selected. 
	  @param nlName the name to exclude.
	*/
    public void exclude(String nlName)
    {
		for(int i=0; i<networkListCombo.getItemCount(); i++)
		{
			String s = (String)networkListCombo.getItemAt(i);
			int idx = s.indexOf(' ');
			if (idx != -1 && s.substring(0,idx).equalsIgnoreCase(nlName))
			{
				networkListCombo.removeItemAt(i);
				return;
		    }
		}
    }

	/** @return the number of items in the combo. */
    public int count()
    {
        return networkListCombo.getItemCount();
    }

}

class NetworkListSelectDialog_okButton_actionAdapter implements java.awt.event.ActionListener {
	NetworkListSelectDialog adaptee;

	NetworkListSelectDialog_okButton_actionAdapter(NetworkListSelectDialog adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.okButton_actionPerformed(e);
	}
}

class NetworkListSelectDialog_cancelButton_actionAdapter implements java.awt.event.ActionListener {
	NetworkListSelectDialog adaptee;

	NetworkListSelectDialog_cancelButton_actionAdapter(NetworkListSelectDialog adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.cancelButton_actionPerformed(e);
	}
}
