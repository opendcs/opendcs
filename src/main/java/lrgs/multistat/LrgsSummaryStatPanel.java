package lrgs.multistat;

import java.awt.*;

import javax.swing.*;
import javax.swing.border.*;

import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Date;

import lrgs.lrgsmain.LrgsInputInterface;
import lrgs.statusxml.LrgsStatusSnapshotExt;
import lrgs.apistatus.AttachedProcess;
import lrgs.apistatus.DownLink;

/**
 * Shows a summary of LRGS status variables.
 */
public class LrgsSummaryStatPanel extends JPanel
{
	BorderLayout borderLayout1 = new BorderLayout();
	JPanel jPanel1 = new JPanel();
	FlowLayout flowLayout1 = new FlowLayout();
	JTextField systemNameField = new JTextField();
	JPanel jPanel2 = new JPanel();
	JLabel datasourceLabel = new JLabel();
	StatusField dataSourceField = new StatusField();
	JLabel systemStatusLabel = new JLabel();
	StatusField systemStatusField = new StatusField();
	JLabel systemTimeLabel = new JLabel();
	StatusField systemTimeField = new StatusField();
	JLabel numClientsLabel = new JLabel();
	StatusField numClientsField = new StatusField();
	StatusField msgsThisHourField = new StatusField();
	JLabel alarmsLabel = new JLabel();
	StatusField alarmsField = new StatusField();
	GridBagLayout gridBagLayout1 = new GridBagLayout();
	JLabel bottomFillerLabel = new JLabel();
	Border border1;

	private static SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd HH:mm:ss");
	static
	{
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	public LrgsSummaryStatPanel()
	{
		try
		{
			jbInit();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	void jbInit() throws Exception
	{
		border1 = BorderFactory.createBevelBorder(BevelBorder.LOWERED, Color.white, Color.white, new Color(
			115, 114, 105), new Color(165, 163, 151));
		this.setLayout(borderLayout1);
		jPanel1.setLayout(flowLayout1);
		jPanel1.setMinimumSize(new Dimension(150, 50));
		jPanel1.setPreferredSize(new Dimension(150, 50));
		systemNameField.setFont(new java.awt.Font("Dialog", 1, 16));
		systemNameField.setForeground(Color.black);
		systemNameField.setBorder(border1);
		systemNameField.setBackground(new Color(236, 233, 216));
		systemNameField.setMinimumSize(new Dimension(200, 32));
		systemNameField.setPreferredSize(new Dimension(180, 32));
		systemNameField.setToolTipText("Module type and host name.");
		systemNameField.setEditable(false);
		systemNameField.setHorizontalAlignment(SwingConstants.CENTER);
		jPanel2.setLayout(gridBagLayout1);
		datasourceLabel.setText("Data Source:");
		dataSourceField.setToolTipText("Sources for this LRGS");
		systemStatusLabel.setText("System Status:");
		systemStatusField.setToolTipText("Status reported by this module.");
		systemTimeLabel.setText("System Time:");
		systemTimeField.setToolTipText("Time reported by system.");
		numClientsLabel.setText("Num Clients:");
		numClientsField.setToolTipText("Number of clients currently being serviced.");
		msgsThisHourField.setToolTipText("Good / Parity-Err Messages Received this Hour");
		alarmsLabel.setText("Alarms:");
		alarmsField.setToolTipText("Number of outstanding alarms on this system.");
		this.setToolTipText("");
		this.add(jPanel1, BorderLayout.NORTH);
		jPanel1.add(systemNameField, null);
		this.add(jPanel2, BorderLayout.CENTER);
		
		int row = 0;
		jPanel2.add(datasourceLabel, 
			new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
				GridBagConstraints.NONE, new Insets(4, 10, 4, 2), 0, 0));
		jPanel2.add(dataSourceField, 
			new GridBagConstraints(1, row, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
				GridBagConstraints.HORIZONTAL, new Insets(4, 0, 4, 10), 0, 0));

		row++;
		jPanel2.add(systemStatusLabel, 
			new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
				GridBagConstraints.NONE, new Insets(4, 10, 4, 2), 0, 0));
		jPanel2.add(systemStatusField, 
			new GridBagConstraints(1, row, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
				GridBagConstraints.HORIZONTAL, new Insets(4, 0, 4, 10), 0, 0));
	
		row++;
		jPanel2.add(systemTimeLabel, 
			new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
				GridBagConstraints.NONE, new Insets(4, 10, 4, 2), 0, 0));
		jPanel2.add(systemTimeField, 
			new GridBagConstraints(1, row, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
				GridBagConstraints.HORIZONTAL, new Insets(4, 0, 4, 10), 0, 0));
		
		row++;
		jPanel2.add(numClientsLabel, 
			new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
				GridBagConstraints.NONE, new Insets(4, 10, 4, 2), 0, 0));
		jPanel2.add(numClientsField, 
			new GridBagConstraints(1, row, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
				GridBagConstraints.HORIZONTAL, new Insets(4, 0, 4, 10), 0, 0));
		
		row++;
		jPanel2.add(new JLabel("Msgs this Hr:"), 
			new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
				GridBagConstraints.NONE, new Insets(4, 10, 4, 2), 0, 0));
		jPanel2.add(msgsThisHourField, 
			new GridBagConstraints(1, row, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
				GridBagConstraints.HORIZONTAL, new Insets(4, 0, 4, 10), 0, 0));
		
		row++;
		jPanel2.add(alarmsLabel, 
			new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHEAST,
				GridBagConstraints.NONE, new Insets(4, 10, 4, 2), 0, 0));
		jPanel2.add(alarmsField, 
			new GridBagConstraints(1, row, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
				GridBagConstraints.HORIZONTAL, new Insets(4, 0, 4, 10), 0, 0));
		
		row++;
		jPanel2.add(bottomFillerLabel, 
			new GridBagConstraints(0, row, 2, 1, 0.0, 1.0, GridBagConstraints.NORTH,
				GridBagConstraints.VERTICAL, new Insets(0, 0, 0, 0), 0, 0));
	}

	/**
	 * Sets the status fields according to the passed data.
	 * 
	 * @param status
	 *            the status snapshot received from the server.
	 * @param numAlarm
	 *            the number of alarms asserted on this server.
	 * @param src
	 *            the source for this server 'u'plink or 'd'ownlink.
	 */
	public void setStatus(LrgsStatusSnapshotExt currentStatus, int numAlarms, char src)
	{
		StringBuilder sb = new StringBuilder();
		if (currentStatus != null && currentStatus.lss != null && currentStatus.lss.downLinks != null)
			for(int slot = 0; slot < currentStatus.lss.downLinks.length; slot++)
			{
				DownLink dl = currentStatus.lss.downLinks[slot];
				if (dl == null)
					continue;
				switch(dl.type)
				{
				case LrgsInputInterface.DL_DRGS:
					if (sb.indexOf("DRGS") < 0)
						sb.append("DRGS ");
					break;
				case LrgsInputInterface.DL_DOMSAT:
					if (sb.indexOf("DOMSAT") < 0)
						sb.append("DOMSAT ");
					break;
				case LrgsInputInterface.DL_NETBAK:
				case LrgsInputInterface.DL_DDS:
					if (sb.indexOf("DDS") < 0)
						sb.append("DDS ");
					break;
				case LrgsInputInterface.DL_NOAAPORT:
					if (sb.indexOf("NOAAPORT") < 0)
						sb.append("NOAAPORT ");
					break;
				case LrgsInputInterface.DL_LRIT:
					if (sb.indexOf("LRIT") < 0)
						sb.append("LRIT ");
					break;
				case LrgsInputInterface.DL_IRIDIUM:
					if (sb.indexOf("IRIDIUM") < 0)
						sb.append("IRIDIUM ");
					break;
				case LrgsInputInterface.DL_EDL:
					if (sb.indexOf("EDL") < 0)
						sb.append("EDL ");
					break;
				}
			}
		dataSourceField.setText(sb.toString());
		
		String ss = currentStatus.systemStatus;
		boolean isError = ss.startsWith("R:");
		if (isError)
			ss = ss.substring(2);
		boolean isWarning = ss.startsWith("Y:");
		if (isWarning)
			ss = ss.substring(2);

		systemStatusField.setText(ss);
		if (isError)
			systemStatusField.setError();
		else if (isWarning)
			systemStatusField.setWarning();
		else
			systemStatusField.setOk();

		systemTimeField.setText(dateFormat.format(new Date(currentStatus.lss.lrgsTime * 1000L)));
		int timediff = currentStatus.lss.lrgsTime - (int) (System.currentTimeMillis() / 1000L);
		if (timediff < -600 || timediff > 600)
			systemTimeField.setError();
		else if (timediff < -60 || timediff > 60)
			systemTimeField.setWarning();
		else
			systemTimeField.setOk();

		int numClients = 0;
		if (currentStatus.lss.attProcs != null)
			for (int i = 0; i < currentStatus.lss.attProcs.length; i++)
			{
				AttachedProcess ap = currentStatus.lss.attProcs[i];
				if (ap == null)
					continue;
				if (ap.type.startsWith("DDS-C") || ap.type.startsWith("Net"))
					numClients++;
			}

		numClientsField.setText("" + numClients);
		if (numClients < 5)
			numClientsField.setWarning();
		else
			numClientsField.setOk();

		int curhour = (currentStatus.lss.lrgsTime / 3600) % 24;
		int secOfHour = currentStatus.lss.lrgsTime % 3600;
		int g = currentStatus.lss.qualMeas[curhour].numGood;
		msgsThisHourField.setText("" + g);
		if (secOfHour > 10 && (g / secOfHour) < 1)
			msgsThisHourField.setError();
		else if (secOfHour > 10 && (g / secOfHour) < 3)
			msgsThisHourField.setWarning();
		else
			msgsThisHourField.setOk();

		alarmsField.setText("" + numAlarms);
		if (numAlarms > 0)
			alarmsField.setWarning();
		else
			alarmsField.setOk();
	}
}
