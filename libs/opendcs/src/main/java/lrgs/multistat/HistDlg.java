/*
* $Id: HistDlg.java,v 1.1 2016/02/04 20:46:50 mmaloney Exp $
*/
package lrgs.multistat;

import java.awt.*;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import decodes.gui.SortingListTable;

public class HistDlg
		extends JDialog 
{
	private JPanel panel1 = new JPanel();
	private BorderLayout borderLayout1 = new BorderLayout();
	private JPanel jPanel1 = new JPanel();
	private JLabel jLabel1 = new JLabel();
	private JPanel jPanel2 = new JPanel();
	private BorderLayout borderLayout2 = new BorderLayout();
	private JScrollPane jScrollPane1 = new JScrollPane();
	private JTable alarmTable;
	private JPanel jPanel3 = new JPanel();
	private JButton okButton = new JButton();

	public HistDlg(Frame owner, CancelledAlarmList model) 
	{
		super(owner, "Alarm History", false);
		alarmTable = new SortingListTable(model,
			new int[] { 6, 19, 12, 10, 6, 6, 41 });
		alarmTable.getSelectionModel().setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION);
		this.setSize(new Dimension(900, 400));
		this.setPreferredSize(new Dimension(900, 400));
		try 
		{
			jbInit();
			pack();
		}
		catch (Exception exception) 
		{
			exception.printStackTrace();
		}
	}

	private void jbInit() throws Exception 
	{
		panel1.setLayout(borderLayout1);
		jLabel1.setFont(new java.awt.Font("Dialog", Font.BOLD, 13));
		jLabel1.setText("Alarm History");
		this.setTitle("Alarm History");
		jPanel2.setLayout(borderLayout2);
		okButton.setText(" Ok ");
		okButton.addActionListener(
			new AlarmHistoryDialog_okButton_actionAdapter(this));
		getContentPane().add(panel1);
		jPanel1.add(jLabel1);
		panel1.add(jPanel2, java.awt.BorderLayout.CENTER);
		jPanel2.add(jScrollPane1, java.awt.BorderLayout.CENTER);
		jScrollPane1.getViewport().add(alarmTable);
		panel1.add(jPanel1, java.awt.BorderLayout.NORTH);
		panel1.add(jPanel3, java.awt.BorderLayout.SOUTH);
		jPanel3.add(okButton);
	}

	public void okButton_actionPerformed(ActionEvent e) 
	{
		setVisible(false);
	}
}

class AlarmHistoryDialog_okButton_actionAdapter
		implements ActionListener 
	{
	private HistDlg adaptee;
	AlarmHistoryDialog_okButton_actionAdapter(HistDlg adaptee) 
	{
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) 
	{
		adaptee.okButton_actionPerformed(e);
	}
}
