/*
*  $Id$
*
*  $Log$
*  Revision 1.2  2009/11/11 17:15:29  shweta
*  LRIT update
*
*  Revision 1.1  2008/04/04 18:21:16  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2006/02/20 22:01:04  mmaloney
*  dev
*
*  Revision 1.2  2004/05/18 22:52:41  mjmaloney
*  dev
*
*  Revision 1.1  2004/05/18 18:11:51  mjmaloney
*  dev
*
*/
package lritdcs;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.Iterator;
import java.util.StringTokenizer;

import lrgs.common.SearchCriteria;
import lrgs.common.DcpAddress;
import ilex.util.AsciiUtil;

public class SearchCritTimeDialog extends JDialog {
	JPanel panel1 = new JPanel();
	BorderLayout borderLayout1 = new BorderLayout();
	JPanel jPanel1 = new JPanel();
	JButton okButton = new JButton();
	FlowLayout flowLayout1 = new FlowLayout();
	JButton cancelButton = new JButton();
	JPanel jPanel2 = new JPanel();
	JLabel jLabel1 = new JLabel();
	JLabel whichScLabel = new JLabel();
	JPanel jPanel3 = new JPanel();
	JLabel jLabel2 = new JLabel();
	JTextField addressField = new JTextField();
	JLabel jLabel3 = new JLabel();
	JTextField channelField = new JTextField();
	JLabel jLabel4 = new JLabel();
	JTextField netlistField = new JTextField();
	FlowLayout flowLayout2 = new FlowLayout();
	GridBagLayout gridBagLayout1 = new GridBagLayout();
	JLabel jLabel5 = new JLabel();
	JLabel jLabel6 = new JLabel();
	JTextField sinceField = new JTextField();
	JTextField untilField = new JTextField();

	SearchCriteria origSC;
	SearchCriteria copySC;
	boolean okPressed;

	public SearchCritTimeDialog(Frame frame, String title, boolean modal) {
		super(frame, title, modal);
		try {
			jbInit();
			pack();
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
		origSC = null;
		okPressed = false;
	}

	public SearchCritTimeDialog() {
		this(null, "", false);
	}

	public SearchCritTimeDialog(Frame parent, String what, SearchCriteria sc)
	{
		this(parent, "", true);
		whichScLabel.setText(what);
		origSC = sc;
		copySC = new SearchCriteria(origSC);
		fillValues();
	}

	private void fillValues()
	{
		String s = copySC.getLrgsSince();
		sinceField.setText(s == null ? "" : s);
		s = copySC.getLrgsUntil();
		untilField.setText(s == null ? "" : s);

		StringBuffer sb = new StringBuffer();
		for(Iterator it = copySC.ExplicitDcpAddrs.iterator(); it.hasNext(); )
		{
			DcpAddress da = (DcpAddress)it.next();
			sb.append(da.toString() + " ");
		}
		addressField.setText(sb.toString());

		sb.setLength(0);
		if (copySC.channels != null)
		{
			for(int i=0; i<copySC.channels.length; i++)
				sb.append("" + copySC.channels[i] + " ");
		}
		channelField.setText(sb.toString());

		sb.setLength(0);
		for(Iterator it = copySC.NetlistFiles.iterator(); it.hasNext(); )
			sb.append((String)it.next() + " ");
		netlistField.setText(sb.toString());
	}

	private boolean getValues(SearchCriteria sc)
	{
		sc.clear();
		String t = "";
		String what = "";
		try
		{
			what = "Since";
			t = sinceField.getText().trim();
			sc.setLrgsSince(t);
			sc.setDapsSince(t);

			what = "Until";
			t = untilField.getText().trim();
			sc.setLrgsUntil(t);
			sc.setDapsUntil(t);

			what = "DCP Address";
			StringTokenizer st = new StringTokenizer(addressField.getText());
			while(st.hasMoreTokens())
			{
				t = st.nextToken();
				sc.addDcpAddress(new DcpAddress(t));
			}

			what = "GOES Channel";
			st = new StringTokenizer(channelField.getText());
			while(st.hasMoreTokens())
			{
				t = st.nextToken();
				int c = Integer.parseInt(t);
				sc.addChannelToken(t);
			}

			what = "Network List";
			st = new StringTokenizer(netlistField.getText());
			while(st.hasMoreTokens())
			{
				t = st.nextToken();
				sc.addNetworkList(t);
			}
			return true;
		}
		catch(NumberFormatException ex)
		{
			showError("Invalid " + what + " '" + t + "'");
			return false;
		}
	}

	private void jbInit() throws Exception {
		panel1.setLayout(borderLayout1);
		okButton.setPreferredSize(new Dimension(100, 23));
		okButton.setText("OK");
		okButton.addActionListener(new SearchCritTimeDialog_okButton_actionAdapter(this));
		jPanel1.setLayout(flowLayout1);
		flowLayout1.setHgap(20);
		flowLayout1.setVgap(10);
		cancelButton.setPreferredSize(new Dimension(100, 23));
		cancelButton.setText("Cancel");
		cancelButton.addActionListener(new SearchCritTimeDialog_cancelButton_actionAdapter(this));
		jLabel1.setVerifyInputWhenFocusTarget(true);
		jLabel1.setText("Search Criteria for ");
		whichScLabel.setText("High Priority DCP Messages");
		jPanel3.setBorder(BorderFactory.createEtchedBorder());
		jPanel3.setLayout(gridBagLayout1);
		jPanel3.setPreferredSize(new Dimension(600, 185));
		jLabel2.setText("DCP Addresses:");
		addressField.setToolTipText("Enter zero or more DCP addresses, space-separated.");
		addressField.setText("");
		jLabel3.setText("GOES Channels:");
		channelField.setToolTipText("Enter zero or more GOES channel numbers, space-separated.");
		channelField.setText("");
		jLabel4.setText("LRGS Network Lists:");
		netlistField.setToolTipText("Enter zero or more network lists that reside on your LRGS, space-separated.");
		netlistField.setText("");
		jPanel2.setPreferredSize(new Dimension(240, 35));
		jPanel2.setLayout(flowLayout2);
		flowLayout2.setAlignment(FlowLayout.CENTER);
		flowLayout2.setVgap(10);
		this.setTitle("Search Criteria Dialog");
		jLabel5.setText("Until:");
    jLabel6.setText("Since:");
    untilField.setText("");
    getContentPane().add(panel1);
		jPanel1.add(okButton, null);
		jPanel1.add(cancelButton, null);
		panel1.add(jPanel2, BorderLayout.NORTH);
		jPanel2.add(jLabel1, null);
		jPanel2.add(whichScLabel, null);
		panel1.add(jPanel3, BorderLayout.CENTER);
		jPanel3.add(addressField,		    new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(4, 0, 4, 46), 0, 0));
		jPanel3.add(channelField,	   new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(4, 0, 4, 46), 0, 0));
		jPanel3.add(netlistField,		  new GridBagConstraints(1, 4, 1, 1, 1.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(4, 0, 10, 46), 0, 0));
		jPanel3.add(jLabel2,	    new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(4, 15, 4, 0), 0, 0));
		jPanel3.add(jLabel3,	   new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(4, 15, 4, 0), 0, 0));
		jPanel3.add(jLabel4,	   new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(4, 15, 10, 0), 0, 0));
    jPanel3.add(jLabel5,   new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(4, 30, 4, 0), 0, 0));
    jPanel3.add(jLabel6,   new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(15, 0, 4, 0), 0, 0));
    jPanel3.add(sinceField,  new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(15, 0, 4, 46), 0, 0));
    jPanel3.add(untilField,  new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(4, 0, 4, 46), 0, 0));
    this.getContentPane().add(jPanel1, BorderLayout.SOUTH);
	}

	void okButton_actionPerformed(ActionEvent e)
	{
		// Test by trying to parse values into local copy first.
		if (getValues(copySC))
		{
			getValues(origSC);
			okPressed = true;
			closeDlg();
		}
	}

	void cancelButton_actionPerformed(ActionEvent e)
	{
		okPressed = false;
		closeDlg();
	}

	private void showError(String msg)
	{
		System.err.println(msg);
		JOptionPane.showMessageDialog(this,
			AsciiUtil.wrapString(msg, 60), "Error!", JOptionPane.ERROR_MESSAGE);
	}

	void closeDlg()
	{
		setVisible(false);
		dispose();
	}

	public boolean wasOk()
	{
		return okPressed;
	}
	
	public boolean isDialogEmpty()
	{
		if(sinceField.getText().trim().length()== 0 &
		untilField.getText().trim().length()== 0 &
		addressField.getText().trim().length()== 0 &
		netlistField.getText().trim().length()==0 &
		channelField.getText().trim().length()== 0 )
			return true;
		else return false;
		
	}
}

class SearchCritTimeDialog_okButton_actionAdapter implements java.awt.event.ActionListener {
	SearchCritTimeDialog adaptee;

	SearchCritTimeDialog_okButton_actionAdapter(SearchCritTimeDialog adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.okButton_actionPerformed(e);
	}
}

class SearchCritTimeDialog_cancelButton_actionAdapter implements java.awt.event.ActionListener {
	SearchCritTimeDialog adaptee;

	SearchCritTimeDialog_cancelButton_actionAdapter(SearchCritTimeDialog adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.cancelButton_actionPerformed(e);
	}
}
