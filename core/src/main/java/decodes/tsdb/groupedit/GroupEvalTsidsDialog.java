package decodes.tsdb.groupedit;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JPanel;

import decodes.gui.GuiDialog;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;

public class GroupEvalTsidsDialog extends GuiDialog
{

	public GroupEvalTsidsDialog(Frame parent, TimeSeriesDb tsdb, ArrayList<TimeSeriesIdentifier> tsids)
	{
		super(parent, "Time Series Group Members", true);
		guiInit(tsdb, tsids);
		trackChanges("GroupMembers");
	}
	
	private void guiInit(TimeSeriesDb tsdb, ArrayList<TimeSeriesIdentifier> tsids)
	{
		TsListSelectPanel tlsp = new TsListSelectPanel(tsdb, true, false);
		tlsp.setTimeSeriesList(tsids);
		JPanel jp = new JPanel(new BorderLayout());
		jp.add(tlsp, BorderLayout.CENTER);
		JPanel south = new JPanel(new FlowLayout());
		JButton okButton = new JButton("OK");
		okButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					okPressed();
				}
			});
		south.add(okButton);
		jp.add(south, BorderLayout.SOUTH);
		getContentPane().add(jp);
		pack();
	}

	protected void okPressed()
	{
		setVisible(false);
		dispose();
	}
}
