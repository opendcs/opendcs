package lrgs.rtstat;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.ResourceBundle;

import javax.swing.JDialog;
import javax.swing.JPanel;
import java.awt.*;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.*;

import ilex.util.AuthException;
import lrgs.db.Outage;
import lrgs.db.LrgsConstants;
import decodes.gui.SortingListTable;
import decodes.gui.GuiDialog;

public class OutageMaintenanceDialog
	extends GuiDialog
{
	private static ResourceBundle labels = 
		RtStat.getLabels();
	private static ResourceBundle genericLabels = 
		RtStat.getGenericLabels();
	private JPanel mainPanel = new JPanel();
	private BorderLayout mainPanelLayout = new BorderLayout();
	private JPanel southButtonPanel = new JPanel();
	private JButton closeButton = new JButton();
	private FlowLayout southFlowLayout = new FlowLayout();
	private JPanel northButtonPanel = new JPanel();
	private FlowLayout northFlowLayout = new FlowLayout();
	private JButton refreshButton = new JButton();
	private JPanel centerPanel = new JPanel();
	private BorderLayout borderLayout1 = new BorderLayout();
	private JPanel eastPanel = new JPanel();
	private JButton retryButton = new JButton();
	private GridBagLayout gridBagLayout1 = new GridBagLayout();
	private JButton deleteButton = new JButton();
	private JPanel tablePanel = new JPanel();
	private BorderLayout borderLayout2 = new BorderLayout();
	private JScrollPane tableScrollPane = new JScrollPane();
	private OutageTableModel tableModel = new OutageTableModel();
	private SortingListTable outageTable = new SortingListTable(tableModel,
		tableModel.columnWidths);

	private DdsClientIf clientIf = null;

	public OutageMaintenanceDialog(Frame owner)
	{
		this(owner, labels.getString("OutageMaintenanceDialog.title"), true);
	}

	public OutageMaintenanceDialog(Frame owner, String title, boolean modal)
	{
		super(owner, title, modal);
		try
		{
			setDefaultCloseOperation(DISPOSE_ON_CLOSE);
			jbInit();
			mainPanel.setPreferredSize(new Dimension(800, 350));
			pack();
		}
		catch (Exception exception)
		{
			exception.printStackTrace();
		}
	}

	/**
	 * Called prior to launching the dialog, this method populates the table.
	 */
	public void startDialog(DdsClientIf clientIf, ArrayList<Outage> data)
	{
		this.clientIf = clientIf;
		tableModel.setData(data);
	}

	private void jbInit()
		throws Exception
	{
		mainPanel.setLayout(mainPanelLayout);
		closeButton.setPreferredSize(new Dimension(100, 23));
		closeButton.setText(genericLabels.getString("close"));
		closeButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				closeButton_actionPerformed();
			}
		});
		southButtonPanel.setLayout(southFlowLayout);
		southFlowLayout.setVgap(10);
		northButtonPanel.setLayout(northFlowLayout);
		northFlowLayout.setAlignment(FlowLayout.LEFT);
		northFlowLayout.setVgap(10);
		refreshButton.setPreferredSize(new Dimension(120, 23));
		refreshButton.setText(labels.getString(
				"OutageMaintenanceDialog.refreshList"));
		refreshButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				refreshButton_actionPerformed();
			}
		});
		centerPanel.setLayout(borderLayout1);
		retryButton.setPreferredSize(new Dimension(100, 23));
		retryButton.setText(labels.getString(
				"OutageMaintenanceDialog.retry"));
		retryButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				retryButton_actionPerformed();
			}
		});
		eastPanel.setLayout(gridBagLayout1);
		deleteButton.setPreferredSize(new Dimension(100, 23));
		deleteButton.setText(genericLabels.getString("delete"));
		deleteButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				deleteButton_actionPerformed();
			}
		});
		tablePanel.setLayout(borderLayout2);
		tableScrollPane.setHorizontalScrollBarPolicy(JScrollPane.
			HORIZONTAL_SCROLLBAR_NEVER);
		tablePanel.setBorder(BorderFactory.createLoweredBevelBorder());
		this.setTitle(labels.getString("OutageMaintenanceDialog.lrgsTitle"));
		getContentPane().add(mainPanel);
		mainPanel.add(southButtonPanel, java.awt.BorderLayout.SOUTH);
		southButtonPanel.add(closeButton);
		northButtonPanel.add(refreshButton);
		mainPanel.add(centerPanel, java.awt.BorderLayout.CENTER);
		centerPanel.add(eastPanel, java.awt.BorderLayout.EAST);
		mainPanel.add(northButtonPanel, java.awt.BorderLayout.NORTH);
		eastPanel.add(retryButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
			, GridBagConstraints.NORTH, GridBagConstraints.NONE,
			new Insets(10, 5, 5, 5), 0, 0));
		eastPanel.add(deleteButton, new GridBagConstraints(0, 1, 1, 1, 0.0, 1.0
			, GridBagConstraints.NORTH, GridBagConstraints.NONE,
			new Insets(5, 5, 5, 5), 0, 0));
		centerPanel.add(tablePanel, java.awt.BorderLayout.CENTER);
		tablePanel.add(tableScrollPane, java.awt.BorderLayout.CENTER);
		tableScrollPane.getViewport().add(outageTable);
	}

	public void closeButton_actionPerformed()
	{
		setVisible(false);
	}

	public void refreshButton_actionPerformed()
	{
		try
		{
			ArrayList<Outage> data = clientIf.getOutages();
			tableModel.setData(data);
		}
		catch(AuthException ex)
		{
			showError(labels.getString(
				"OutageMaintenanceDialog.cannotGetOutagesErr") + ex);
		}
	}

	public void retryButton_actionPerformed()
	{
		try
		{
			ArrayList<Outage> toAssert = new ArrayList<Outage>();
			int n = tableModel.getRowCount();
			for(int i=0; i<n; i++)
				if (outageTable.isRowSelected(i))
				{
					Outage otg = (Outage)tableModel.getRowObject(i);
					otg.setStatusCode(LrgsConstants.outageStatusActive);
					toAssert.add(otg);
				}
			if (toAssert.size() == 0)
				return;
			clientIf.assertOutages(toAssert);
			refreshButton_actionPerformed();
		}
		catch(AuthException ex)
		{
			showError(labels.getString(
					"OutageMaintenanceDialog.cantAssertOutagesErr") + ex);
		}
	}

	public void deleteButton_actionPerformed()
	{
		try
		{
			ArrayList<Outage> toAssert = new ArrayList<Outage>();
			int n = tableModel.getRowCount();
			for(int i=0; i<n; i++)
				if (outageTable.isRowSelected(i))
				{
					Outage otg = (Outage)tableModel.getRowObject(i);
					otg.setStatusCode(LrgsConstants.outageStatusDeleted);
					toAssert.add(otg);
				}
			if (toAssert.size() == 0)
				return;
			clientIf.assertOutages(toAssert);
			refreshButton_actionPerformed();
		}
		catch(AuthException ex)
		{
			showError(labels.getString(
			"OutageMaintenanceDialog.cantAssertOutagesErr") + ex);
		}
	}
}
