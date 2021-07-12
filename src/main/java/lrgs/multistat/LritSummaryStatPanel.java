//package lrgs.multistat;
//
//import java.awt.*;
//import java.util.Date;
//import javax.swing.*;
//
//import lritdcs.LritDcsStatus;
//import lritdcs.LritDcsConfig;
//import lritdcs.LritReportGenerator;
//
//import javax.swing.JLabel;
//import java.awt.GridBagConstraints;
//import java.awt.Insets;
//
//public class LritSummaryStatPanel
//	extends JPanel
//{
//	BorderLayout borderLayout1 = new BorderLayout();
//	JPanel jPanel1 = new JPanel();
//	FlowLayout flowLayout1 = new FlowLayout();
//	public JTextField systemNameField = new JTextField();
//	JPanel jPanel2 = new JPanel();
//	JLabel ddsServerLabel = new JLabel();
//	StatusField ddsServerField = new StatusField();
//	JLabel systemStatusLabel = new JLabel();
//	StatusField systemStatusField = new StatusField();
//	JLabel systemTimeLabel = new JLabel();
//	StatusField systemTimeField = new StatusField();
//	JLabel lastMsgLabel = new JLabel();
//	StatusField lastMsgField = new StatusField();
//	JLabel msgsThisHourLabel = new JLabel();
//	StatusField msgsThisHourField = new StatusField();
//	JLabel filesThisHourLabel = new JLabel();
//	StatusField filesField = new StatusField();
//	JLabel queuedLabel = new JLabel();
//	StatusField queuedField = new StatusField();
//	JLabel domain2ALabel = new JLabel();
//	StatusField domain2AField = new StatusField();
//	JLabel alarmsLabel = new JLabel();
//	StatusField alarmsField = new StatusField();
//	GridBagLayout gridBagLayout1 = new GridBagLayout();
//	JLabel bottomFillerLabel = new JLabel();
//	private JLabel domain2BLabel = null;
//	private JLabel domain2CLabel = null;
//	private StatusField domain2BField = null;
//	private StatusField domain2CField = null;
//	private String lritPanelName;
//
//	public LritSummaryStatPanel()
//	{
//		try
//		{
//			jbInit();
//		}
//		catch (Exception ex)
//		{
//			ex.printStackTrace();
//		}
//	}
//
//	
//	
//	void jbInit()
//		throws Exception
//	{
//		GridBagConstraints gridBagConstraints31 = new GridBagConstraints(0, 9, 2, 1, 0.0, 1.0, GridBagConstraints.NORTH, GridBagConstraints.VERTICAL, new Insets(0, 0, 0, 0), 0, 0);
//		gridBagConstraints31.gridy = 11;
//		GridBagConstraints gridBagConstraints21 = new GridBagConstraints();
//		gridBagConstraints21.fill = GridBagConstraints.HORIZONTAL;
//		gridBagConstraints21.gridx = 1;
//		gridBagConstraints21.gridy = 9;
//		gridBagConstraints21.anchor = GridBagConstraints.WEST;
//		gridBagConstraints21.insets = new Insets(4, 0, 4, 10);
//		gridBagConstraints21.weightx = 1.0;
//		GridBagConstraints gridBagConstraints11 = new GridBagConstraints();
//		gridBagConstraints11.fill = GridBagConstraints.HORIZONTAL;
//		gridBagConstraints11.gridx = 1;
//		gridBagConstraints11.gridy = 8;
//		gridBagConstraints11.anchor = GridBagConstraints.WEST;
//		gridBagConstraints11.insets = new Insets(4, 0, 4, 10);
//		gridBagConstraints11.weightx = 1.0;
//		GridBagConstraints gridBagConstraints4 = new GridBagConstraints(1, 8, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(4, 0, 4, 10), 0, 0);
//		gridBagConstraints4.gridy = 10;
//		GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
//		gridBagConstraints2.gridx = 0;
//		gridBagConstraints2.insets = new Insets(4, 10, 4, 2);
//		gridBagConstraints2.gridy = 9;
//		GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
//		gridBagConstraints1.gridx = 0;
//		gridBagConstraints1.insets = new Insets(4, 10, 4, 2);
//		gridBagConstraints1.gridy = 8;
//		GridBagConstraints gridBagConstraints = new GridBagConstraints(0, 8, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, new Insets(4, 10, 4, 2), 0, 0);
//		gridBagConstraints.gridy = 10;
//		domain2CLabel = new JLabel();
//		domain2CLabel.setText("Domain 2C Host:");
//		domain2BLabel = new JLabel();
//		domain2BLabel.setText("Domain 2B Host:");
//		this.setLayout(borderLayout1);
//		jPanel1.setLayout(flowLayout1);
//		jPanel1.setMinimumSize(new Dimension(150, 50));
//		jPanel1.setPreferredSize(new Dimension(150, 50));
//		systemNameField.setFont(new java.awt.Font("Dialog", 1, 16));
//		systemNameField.setBorder(BorderFactory.createLoweredBevelBorder());
//		systemNameField.setBackground(new Color(236, 233, 216));
//		systemNameField.setMinimumSize(new Dimension(180, 32));
//		systemNameField.setPreferredSize(new Dimension(210, 32));
//		systemNameField.setToolTipText("Module type and host name.");
//		systemNameField.setEditable(false);
//		systemNameField.setText("LRIT: "
//			+ MultiStatConfig.instance().LritDisplayName);
//		systemNameField.setHorizontalAlignment(SwingConstants.CENTER);
//		jPanel2.setLayout(gridBagLayout1);
//		ddsServerLabel.setText("DDS Server:");
//		ddsServerField.setText("");
//		ddsServerField.setToolTipText("DDS server used to pull DCP messages");
//		systemStatusLabel.setText("System Status:");
//		systemStatusField.setText("Connect Error");
//		systemStatusField.setError();
//		systemStatusField.setToolTipText("Current system status");
//		systemTimeLabel.setText("System Time:");
//		systemTimeField.setText("");
//		systemTimeField.setToolTipText("Time reported by system");
//		lastMsgLabel.setText("Last Msg:");
//		lastMsgField.setText("");
//		lastMsgField.setToolTipText("Time last message received from server.");
//		msgsThisHourLabel.setToolTipText("");
//		msgsThisHourLabel.setText("Msgs (H/M/L):");
//		msgsThisHourField.setText("");
//		msgsThisHourField.setToolTipText(
//			"High/Medium/Low messages received this hour.");
//		filesThisHourLabel.setText("Files (H/M/L):");
//		filesField.setText("");
//
//		filesField.setForeground(Color.green);
//		filesField.setToolTipText("High/MediumLow Files sent this hour.");
//		queuedLabel.setToolTipText("");
//		queuedLabel.setText("Queued (H/M/L):");
//		queuedField.setText("");
//		queuedField.setToolTipText(
//			"High/Medium/Low files queued to send.");
//		domain2ALabel.setText("Domain 2A Host:");
//		domain2AField.setText("");
//		domain2AField.setToolTipText("Domain 2A server we are sending files to.");
//		alarmsLabel.setText("Alarms:");
//		alarmsField.setText("");
//		alarmsField.setOk();
//		alarmsField.setToolTipText("Alarms outstanding on this module.");
//		this.add(jPanel1, BorderLayout.NORTH);
//		jPanel1.add(systemNameField, null);
//		this.add(jPanel2, BorderLayout.CENTER);
//
//		jPanel2.add(systemStatusLabel, new GridBagConstraints(0, 0, 1, 1, 0.0,
//			0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(4,
//			10, 4, 2), 0, 0));
//		jPanel2.add(systemStatusField, new GridBagConstraints(1, 0, 1, 1, 1.0,
//			0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
//			new Insets(4, 0, 4, 10), 0, 0));
//		jPanel2.add(systemTimeLabel, new GridBagConstraints(0, 1, 1, 1, 0.0,
//			0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(4,
//			10, 4, 2), 0, 0));
//		jPanel2.add(systemTimeField, new GridBagConstraints(1, 1, 1, 1, 1.0,
//			0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
//			new Insets(4, 0, 4, 10), 0, 0));
//
//		jPanel2.add(ddsServerLabel, new GridBagConstraints(0, 2, 1, 1, 0.0,
//			0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(4,
//			10, 4, 2), 0, 0));
//		jPanel2.add(ddsServerField, new GridBagConstraints(1, 2, 1, 1, 1.0,
//			0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
//			new Insets(4, 0, 4, 10), 0, 0));
//		jPanel2.add(lastMsgLabel, new GridBagConstraints(0, 3, 1, 1, 0.0,
//			0.0
//			, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(4,
//			10, 4, 2), 0, 0));
//		jPanel2.add(lastMsgField, new GridBagConstraints(1, 3, 1, 1, 1.0,
//			0.0
//			, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
//			new Insets(4, 0, 4, 10), 0, 0));
//		jPanel2.add(msgsThisHourLabel, new GridBagConstraints(0, 4, 1, 1, 0.0,
//			0.0
//			, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(4,
//			10, 4, 2), 0, 0));
//		jPanel2.add(msgsThisHourField, new GridBagConstraints(1, 4, 1, 1, 1.0,
//			0.0
//			, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
//			new Insets(4, 0, 4, 10), 0, 0));
//		jPanel2.add(filesThisHourLabel, new GridBagConstraints(0, 5, 1, 1, 0.0,
//			0.0
//			, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(4,
//			10, 4, 2), 0, 0));
//		jPanel2.add(filesField, new GridBagConstraints(1, 5, 1, 1, 1.0,
//			0.0
//			, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
//			new Insets(4, 0, 4, 10), 0, 0));
//		jPanel2.add(queuedField, new GridBagConstraints(1, 6, 1, 1,
//			1.0, 0.0
//			, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
//			new Insets(4, 0, 4, 10), 0, 0));
//		jPanel2.add(queuedLabel, new GridBagConstraints(0, 6, 1, 1,
//			0.0, 0.0
//			, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(4,
//			10, 4, 2), 0, 0));
//		jPanel2.add(domain2ALabel, new GridBagConstraints(0, 7, 1, 1, 0.0, 0.0
//			, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(4,
//			10, 4, 2), 0, 0));
//		jPanel2.add(domain2AField, new GridBagConstraints(1, 7, 1, 1, 1.0, 0.0
//			, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
//			new Insets(4, 0, 4, 10), 0, 0));
//		jPanel2.add(alarmsLabel, gridBagConstraints);
//		jPanel2.add(alarmsField, gridBagConstraints4);
//		jPanel2.add(bottomFillerLabel, gridBagConstraints31);
//		jPanel2.add(domain2BLabel, gridBagConstraints1);
//		jPanel2.add(domain2CLabel, gridBagConstraints2);
//		jPanel2.add(getDomain2BField(), gridBagConstraints11);
//		jPanel2.add(getDomain2CField(), gridBagConstraints21);
//	}
//
//	public void update(LritDcsStatus status, int numAlarms)
//	{
//		systemStatusField.setText(status.status);
//		if (status.status.startsWith("R"))
//			systemStatusField.setOk();
//		else systemStatusField.setWarning();
//		systemTimeField.setText(
//			LritReportGenerator.columnDF.format(new Date(status.serverGMT)));
//		ddsServerField.setText(status.lastDataSource);
//		lastMsgField.setText(
//			LritReportGenerator.columnDF.format(
//				new Date(status.lastRetrieval)));
//		msgsThisHourField.setText(""
//			+ status.msgsThisHourHigh + " / "
//			+ status.msgsThisHourMedium + " / "
//			+ status.msgsThisHourLow);
//		filesField.setText(""
//			+ status.filesSentThisHourHigh + " / "
//			+ status.filesSentThisHourMedium + " / "
//			+ status.filesSentThisHourLow);
//		queuedField.setText(""
//			+ status.filesQueuedHigh + " / "
//			+ status.filesQueuedMedium + " / "
//			+ status.filesQueuedLow);
//		//domain2AField.setText(LritDcsConfig.instance().getDom2HostName());
//		
//		domain2AField.setText(status.domain2Ahost);
//		domain2BField.setText(status.domain2Bhost);
//		domain2CField.setText(status.domain2Chost);
//		alarmsField.setText("" + numAlarms);
//	}
//
//	/**
//	 * This method initializes domain2BField	
//	 * 	
//	 * @return com.ilexeng.rtstat.StatusField	
//	 */
//	private StatusField getDomain2BField() {
//		if (domain2BField == null) {
//			domain2BField = new StatusField();
//			domain2BField.setToolTipText("Domain 2B server we are sending files to.");
//			domain2BField.setText("");
//		}
//		return domain2BField;
//	}
//
//	/**
//	 * This method initializes domain2CField	
//	 * 	
//	 * @return com.ilexeng.rtstat.StatusField	
//	 */
//	private StatusField getDomain2CField() {
//		if (domain2CField == null) {
//			domain2CField = new StatusField();
//			domain2CField.setToolTipText("Domain 2C server we are sending files to.");
//			domain2CField.setText("");
//		}
//		return domain2CField;
//	}
//}
