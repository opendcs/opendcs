/*
* $Id$
*
* $Log$
* Revision 1.5  2012/12/12 16:01:31  mmaloney
* Several updates for 5.2
*
* Revision 1.4  2009/08/24 18:04:30  shweta
* *** empty log message ***
*
* Revision 1.3  2009/08/24 13:46:22  shweta
* fillConfigValues and saveConfigValues methods added to populate config dialog from properties passed as argument.
*
* Revision 1.2  2009/08/14 14:06:46  shweta
* Changes done to incorporate all 3  Domain 2 servers configuration.
*
* Revision 1.1  2008/04/04 18:21:16  cvs
* Added legacy code to repository
*
* Revision 1.9  2004/05/10 15:30:36  mjmaloney
* Distributed UI implemented.
*
* Revision 1.8  2004/05/10 14:15:29  mjmaloney
* Added checkbox for LRIT enable schedule
*
* Revision 1.7  2004/05/05 18:48:18  mjmaloney
* Added UIServer & UISvrThread
*
* Revision 1.6  2004/05/05 15:46:18  mjmaloney
* GUI Adjustments
*
* Revision 1.5  2004/05/05 15:24:11  mjmaloney
* Controls, save, & restore for config dialog working.
*
* Revision 1.4  2004/05/05 12:44:50  mjmaloney
* dev
*
* Revision 1.3  2004/05/04 20:28:19  mjmaloney
* Initialize controls from data structure.
*
* Revision 1.2	2004/05/04 19:43:10	mjmaloney
* Updated with new fields
*
*/
package lritdcs;

import ilex.util.TextUtil;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

public class LritDcsConfigDialog extends JDialog
{
	JPanel panel1 = new JPanel();
	BorderLayout borderLayout1 = new BorderLayout();
	JPanel jPanel1 = new JPanel();
	JButton okButton = new JButton();
	JButton cancelButton = new JButton();
	FlowLayout flowLayout1 = new FlowLayout();
	JPanel outerPanel = new JPanel();
	JPanel ddsServersPanel = new JPanel();
	TitledBorder titledBorder1;
	JLabel jLabel1 = new JLabel();
	JComboBox serverTimeoutCombo = new JComboBox(
		new String[] { "60", "120", "180", "300" } );
	JComboBox serverRetryCombo = new JComboBox(
		new String[] { "300", "600", "900", "1800"} );
	String ddsList[] = new String[] { "", "cdadata.wcda.noaa.gov",
		"cdabackup.wcda.noaa.gov", "lrgseddn1.cr.usgs.gov" };
	JComboBox dds1HostCombo = new JComboBox(ddsList);
	JComboBox dds2HostCombo = new JComboBox(ddsList);
	JComboBox dds3HostCombo = new JComboBox(ddsList);
	String portList[] = new String[] { "16003" };
	JComboBox dds1PortCombo = new JComboBox(portList);
	JComboBox dds2PortCombo = new JComboBox(portList);
	JComboBox dds3PortCombo = new JComboBox(portList);
	JTextField dds1UserField = new JTextField();
	JTextField dds2UserField = new JTextField();
	JTextField dds3UserField = new JTextField();
	JPanel jPanel4 = new JPanel();
	TitledBorder titledBorder2;
	JLabel jLabel10 = new JLabel();
	JLabel jLabel11 = new JLabel();
	JLabel jLabel12 = new JLabel();
	JLabel jLabel13 = new JLabel();
	JLabel jLabel14 = new JLabel();
	JLabel jLabel15 = new JLabel();
	JTextField maxHighBytesField = new JTextField();
	JTextField maxMediumBytesField = new JTextField();
	JTextField maxLowBytesField = new JTextField();
	JTextField maxHighMsgsField = new JTextField();
	JTextField maxMediumMsgsField = new JTextField();
	JTextField maxLowMsgsField = new JTextField();
	JTextField maxHighSecondsField = new JTextField();
	JTextField maxMediumSecondsField = new JTextField();
	JTextField maxLowSecondsField = new JTextField();
	JLabel jLabel16 = new JLabel();
	JTextField scrubHoursField = new JTextField();
	JLabel jLabel17 = new JLabel();
	JTextField maxManualField = new JTextField();
	JLabel jLabel18 = new JLabel();
	JTextField maxAutoField = new JTextField();
	GridBagLayout gridBagLayout2 = new GridBagLayout();
	JPanel domain2Panel = new JPanel();
	JTextField dom2AHostField = new JTextField();
	JTextField dom2AUserField = new JTextField();
	JTextField dom2AHighPriDirField = new JTextField();
	JTextField dom2AMediumPriDirField = new JTextField();
	JTextField dom2ALowPriDirField = new JTextField();
	JPanel lqmUiPanel = new JPanel();
	JCheckBox lqmEnableCheck = new JCheckBox();
	JTextField pendingTimeoutField = new JTextField();
	JTextField rsaField = new JTextField();
	GridBagLayout gridBagLayout5 = new GridBagLayout();
	JTextField lqmPortField = new JTextField();
	JTextField lqmHostField = new JTextField();
	JPanel jPanel7 = new JPanel();
	JTextField uiPortField = new JTextField();
	JTextField uiHostsField = new JTextField();
	FlowLayout flowLayout2 = new FlowLayout();
	boolean isOK;
	private JTextField dom2BHostField = new JTextField();
	private JTextField dom2CHostField = new JTextField();
	private JTextField dom2BUserField = new JTextField();
	private JTextField dom2CUserField = new JTextField();
	private JTextField dom2BHighPriDirField = new JTextField();
	private JTextField dom2CHighPriDirField = new JTextField();
	private JTextField dom2BLowPriDirField = new JTextField();
	private JTextField dom2CLowPriDirField = new JTextField();
	private JTextField dom2BMediumPriDirField = new JTextField();
	private JTextField dom2CMediumPriDirField = new JTextField();
	private JLabel lblLritHost = null;
	public JTextField txtLritHost = new JTextField();
	private JLabel lblLritState = null;
	public JComboBox comboSate = null;
	
//	private JCheckBox ptpEnabledCheck = new JCheckBox("Save in Share Dir for PTP: ");
//	private JTextField ptpDirField = new JTextField();


	public java.util.Properties configProps;  //  @jve:decl-index=0:
	public LritDcsConfigDialog(Frame frame, String title, boolean modal)
	{
		super(frame, title, modal);
		isOK = false;
		try {
			jbInit();
			pack();
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	public LritDcsConfigDialog(Frame frame, String title, boolean modal,String mode)
	{
		super(frame, title, modal);
		isOK = false;
		try {
			jbInit();
			pack();
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	public LritDcsConfigDialog()
	{
		this(null, "", false);
	}

	public  void fillConfigValues(java.util.Properties prop)
	{
		serverTimeoutCombo.setSelectedItem("" + prop.getProperty("ddsTimeOut"));
		serverRetryCombo.setSelectedItem("" + prop.getProperty("ddsRetryPeriod"));

		dds1HostCombo.setSelectedItem("" + prop.getProperty("dds1HostName"));
		dds2HostCombo.setSelectedItem("" + prop.getProperty("dds2HostName"));
		dds3HostCombo.setSelectedItem("" + prop.getProperty("dds3HostName"));  
		dds1PortCombo.setSelectedItem("" + prop.getProperty("dds1Port"));
		dds2PortCombo.setSelectedItem("" + prop.getProperty("dds2Port"));
		dds3PortCombo.setSelectedItem("" + prop.getProperty("dds3Port"));  
		dds1UserField.setText("" + prop.getProperty("dds1UserName"));
		dds2UserField.setText("" + prop.getProperty("dds2UserName"));
		dds3UserField.setText("" + prop.getProperty("dds3UserName"));  

		maxHighBytesField.setText("" + prop.getProperty("maxBytesHigh"));
		maxMediumBytesField.setText("" + prop.getProperty("maxBytesMedium"));
		maxLowBytesField.setText("" + prop.getProperty("maxBytesLow"));   
		maxHighMsgsField.setText("" + prop.getProperty("maxMsgsHigh"));
		maxMediumMsgsField.setText("" + prop.getProperty("maxMsgsMedium"));
		maxLowMsgsField.setText("" + prop.getProperty("maxMsgsLow"));   
		maxHighSecondsField.setText("" + prop.getProperty("maxSecondsHigh"));
		maxMediumSecondsField.setText("" + prop.getProperty("maxSecondsMedium"));
		maxLowSecondsField.setText("" + prop.getProperty("maxSecondsLow"));

		scrubHoursField.setText("" + prop.getProperty("scrubHours"));
		maxManualField.setText("" + prop.getProperty("maxManualRetrans"));
		maxAutoField.setText("" + prop.getProperty("maxAutoRetrans"));

		dom2AHostField.setText("" + prop.getProperty("dom2AHostName"));
		dom2AUserField.setText("" + prop.getProperty("dom2AUser"));
		dom2AHighPriDirField.setText("" + prop.getProperty("dom2ADirHigh"));
		dom2AMediumPriDirField.setText("" + prop.getProperty("dom2ADirMedium"));
		dom2ALowPriDirField.setText("" + prop.getProperty("dom2ADirLow"));
		
		dom2BHostField.setText("" + prop.getProperty("dom2BHostName"));
		dom2BUserField.setText("" + prop.getProperty("dom2BUser"));
		dom2BHighPriDirField.setText("" + prop.getProperty("dom2BDirHigh"));
		dom2BMediumPriDirField.setText("" + prop.getProperty("dom2BDirMedium"));
		dom2BLowPriDirField.setText("" + prop.getProperty("dom2BDirLow"));
		
		dom2CHostField.setText("" + prop.getProperty("dom2CHostName"));
		dom2CUserField.setText("" + prop.getProperty("dom2CUser"));
		dom2CHighPriDirField.setText("" + prop.getProperty("dom2CDirHigh"));
		dom2CMediumPriDirField.setText("" + prop.getProperty("dom2CDirMedium"));
		dom2CLowPriDirField.setText("" + prop.getProperty("dom2CDirLow"));

		lqmEnableCheck.setSelected(Boolean.getBoolean(prop.getProperty("enableLqm")));
		lqmPortField.setText("" + prop.getProperty("lqmPort"));
		lqmHostField.setText("" + prop.getProperty("lqmIPAddress"));
		pendingTimeoutField.setText("" + prop.getProperty("lqmPendingTimeout"));
		uiPortField.setText("" + prop.getProperty("lritUIPort"));
		uiHostsField.setText("" + prop.getProperty("UIIPAddresses"));
		rsaField.setText(""+ prop.getProperty("rsaDir"));
		
		String strstate=  prop.getProperty("fileSenderState");
		if(strstate!=null)
		{				
			if(strstate.equalsIgnoreCase("active"))
			{				
		    comboSate.setSelectedItem("Active");
			}
			else
				comboSate.setSelectedItem("Dormant");
		}
		else
			comboSate.setSelectedItem("Dormant");
		
//		ptpEnabledCheck.setSelected(TextUtil.str2boolean(prop.getProperty("ptpEnabled")));
//		ptpDirField.setText("" + prop.getProperty("ptpDir"));
	}
	
	
	public void fillValues()
	{
		LritDcsConfig cfg = LritDcsConfig.instance();
		serverTimeoutCombo.setSelectedItem("" + cfg.getDdsTimeOut());
		serverRetryCombo.setSelectedItem("" + cfg.getDdsRetryPeriod());

		dds1HostCombo.setSelectedItem(cfg.getDds1HostName());
		dds2HostCombo.setSelectedItem(cfg.getDds2HostName());
		dds3HostCombo.setSelectedItem(cfg.getDds3HostName());
		dds1PortCombo.setSelectedItem("" + cfg.getDds1Port());
		dds2PortCombo.setSelectedItem("" + cfg.getDds2Port());
		dds3PortCombo.setSelectedItem("" + cfg.getDds3Port());
		dds1UserField.setText(cfg.getDds1UserName());
		dds2UserField.setText(cfg.getDds2UserName());
		dds3UserField.setText(cfg.getDds3UserName());

		maxHighBytesField.setText("" + cfg.getMaxBytesHigh());
		maxMediumBytesField.setText("" + cfg.getMaxBytesMedium());
		maxLowBytesField.setText("" + cfg.getMaxBytesLow());
		maxHighMsgsField.setText("" + cfg.getMaxMsgsHigh());
		maxMediumMsgsField.setText("" + cfg.getMaxMsgsMedium());
		maxLowMsgsField.setText("" + cfg.getMaxMsgsLow());
		maxHighSecondsField.setText("" + cfg.getMaxSecondsHigh());
		maxMediumSecondsField.setText("" + cfg.getMaxSecondsMedium());
		maxLowSecondsField.setText("" + cfg.getMaxSecondsLow());

		scrubHoursField.setText("" + cfg.getScrubHours());
		maxManualField.setText("" + cfg.getMaxManualRetrans());
		maxAutoField.setText("" + cfg.getMaxAutoRetrans());

		dom2AHostField.setText(cfg.getDom2AHostName());
		dom2AUserField.setText(cfg.getDom2AUser());
		dom2AHighPriDirField.setText(cfg.getDom2ADirHigh());
		dom2AMediumPriDirField.setText(cfg.getDom2ADirMedium());
		dom2ALowPriDirField.setText(cfg.getDom2ADirLow());
		
		dom2BHostField.setText(cfg.getDom2BHostName());
		dom2BUserField.setText(cfg.getDom2BUser());
		dom2BHighPriDirField.setText(cfg.getDom2BDirHigh());
		dom2BMediumPriDirField.setText(cfg.getDom2BDirMedium());
		dom2BLowPriDirField.setText(cfg.getDom2BDirLow());
		
		dom2CHostField.setText(cfg.getDom2CHostName());
		dom2CUserField.setText(cfg.getDom2CUser());
		dom2CHighPriDirField.setText(cfg.getDom2CDirHigh());
		dom2CMediumPriDirField.setText(cfg.getDom2CDirMedium());
		dom2CLowPriDirField.setText(cfg.getDom2CDirLow());

		lqmEnableCheck.setSelected(cfg.getEnableLqm());
		lqmPortField.setText("" + cfg.getLqmPort());
		lqmHostField.setText(cfg.getLqmIPAddress());
		pendingTimeoutField.setText("" + cfg.getLqmPendingTimeout());
		uiPortField.setText("" + cfg.getLritUIPort());
		uiHostsField.setText(cfg.getUIIPAddresses());
		
		txtLritHost.setText(cfg.getFileSenderHost());
		String strstate=  cfg.getFileSenderState();		
			if(strstate!=null)
			{				
				if(strstate.equalsIgnoreCase("active"))
				{				
			    comboSate.setSelectedItem("Active");
				}
				else
					comboSate.setSelectedItem("Dormant");
			}
			else
				comboSate.setSelectedItem("Dormant");
			
//		ptpEnabledCheck.setSelected(cfg.isPtpEnabled());
//		ptpDirField.setText(cfg.getPtpDir());
		
	}
	
	
	
	
	public void saveConfigValues()
	{
		java.util.Properties prop = new java.util.Properties();
		//LritDcsConfig cfg = LritDcsConfig.instance();
		prop.put("ddsTimeOut", cnvtInt(serverTimeoutCombo.getSelectedItem(), 60) );
		prop.put("ddsRetryPeriod",cnvtInt(serverRetryCombo.getSelectedItem(),600));

		String s = (String)dds1HostCombo.getSelectedItem();
		if (s == null) s = "";
		prop.setProperty("dds1HostName",s);
		s = (String)dds2HostCombo.getSelectedItem();
		if (s == null) s = "";
		prop.setProperty("dds2HostName",s);
		s = (String)dds3HostCombo.getSelectedItem();
		if (s == null) s = "";
		prop.setProperty("dds3HostName",s);

		prop.put("dds1Port",cnvtInt(dds1PortCombo.getSelectedItem(), 16003)) ;
		prop.put("dds2Port",cnvtInt(dds2PortCombo.getSelectedItem(), 16003));
		prop.put("dds3Port", cnvtInt(dds3PortCombo.getSelectedItem(), 16003)) ;

		s = dds1UserField.getText().trim();
		prop.setProperty("dds1UserName",s);
		s = dds2UserField.getText().trim();
		prop.setProperty("dds2UserName",s);
		s = dds3UserField.getText().trim();
		if (s.length() == 0) s = null;
		prop.setProperty("dds3UserName",s);

		prop.put("maxBytesHigh",cnvtInt(maxHighBytesField.getText(), 5000));
		prop.put("maxBytesMedium",cnvtInt(maxMediumBytesField.getText(), 5000));
		prop.put("maxBytesLow",cnvtInt(maxLowBytesField.getText(), 5000));
		
		prop.put("maxMsgsHigh",cnvtInt(maxHighMsgsField.getText(), 20));
		prop.put("maxMsgsMedium",cnvtInt(maxMediumMsgsField.getText(), 20));
		prop.put("maxMsgsLow",cnvtInt(maxLowMsgsField.getText(), 20));
		
		prop.put("maxSecondsHigh",cnvtInt(maxHighSecondsField.getText(), 30));
		prop.put("maxSecondsMedium",cnvtInt(maxMediumSecondsField.getText(), 30));
		prop.put("maxSecondsLow",cnvtInt(maxLowSecondsField.getText(), 30));

		prop.put("scrubHours",cnvtInt(scrubHoursField.getText(), 48));
		prop.put("maxManualRetrans",cnvtInt(maxManualField.getText(), 20));
		prop.put("maxAutoRetrans",cnvtInt(maxAutoField.getText(), 40));		
		prop.put("dom2AHostName",dom2AHostField.getText());
		prop.put("dom2AUser",dom2AUserField.getText());
		prop.put("dom2ADirHigh",dom2AHighPriDirField.getText());
		prop.put("dom2ADirMedium",dom2AMediumPriDirField.getText());
		prop.put("dom2ADirLow",dom2ALowPriDirField.getText());
		
		prop.put("dom2BHostName",dom2BHostField.getText());
		prop.put("dom2BUser",dom2BUserField.getText());
		prop.put("dom2BDirHigh",dom2BHighPriDirField.getText());
		prop.put("dom2BDirMedium",dom2BMediumPriDirField.getText());
		prop.put("dom2BDirLow",dom2BLowPriDirField.getText());
		
		prop.put("dom2CHostName",dom2CHostField.getText());
		prop.put("dom2CUser",dom2CUserField.getText());
		prop.put("dom2CDirHigh",dom2CHighPriDirField.getText());
		prop.put("dom2CDirMedium",dom2CMediumPriDirField.getText());
		prop.put("dom2CDirLow",dom2CLowPriDirField.getText());

		prop.put("enableLqm",lqmEnableCheck.isSelected());

		prop.put("lqmPort",cnvtInt(lqmPortField.getText(), 17004));
		prop.put("lqmIPAddress",lqmHostField.getText() );
		prop.put("lqmPendingTimeout",cnvtInt(pendingTimeoutField.getText(), 300));
		prop.put("lritUIPort",cnvtInt(uiPortField.getText(), 17005));
		prop.put("UIIPAddresses",uiHostsField.getText());
		prop.put("fileSenderHost",txtLritHost.getText());
		
		prop.put("fileSenderState",(String)(comboSate.getSelectedItem()));
		prop.put("rsaDir",rsaField.getText());
		
//		prop.setProperty("ptpEnabled", "" + ptpEnabledCheck.isSelected());
//		prop.setProperty("ptpDir", ptpDirField.getText());
		
		this.setConfigProps(prop);
	}


	public void saveValues()
	{		
		LritDcsConfig cfg = LritDcsConfig.instance();
		cfg.setDdsTimeOut( cnvtInt(serverTimeoutCombo.getSelectedItem(), 60) );
		cfg.setDdsRetryPeriod(cnvtInt(serverRetryCombo.getSelectedItem(),600));

		String s = (String)dds1HostCombo.getSelectedItem();
		if (s != null && s.length() == 0) s = null;
		cfg.setDds1HostName(s);
		s = (String)dds2HostCombo.getSelectedItem();
		if (s != null && s.length() == 0) s = null;
		cfg.setDds2HostName(s);
		s = (String)dds3HostCombo.getSelectedItem();
		if (s != null && s.length() == 0) s = null;
		cfg.setDds3HostName(s);

		cfg.setDds1Port( cnvtInt(dds1PortCombo.getSelectedItem(), 16003) );
		cfg.setDds2Port( cnvtInt(dds2PortCombo.getSelectedItem(), 16003) );
		cfg.setDds3Port( cnvtInt(dds3PortCombo.getSelectedItem(), 16003) );

		s = (String)dds1UserField.getText();
		if (s.length() == 0) s = null;
		cfg.setDds1UserName(s);
		s = (String)dds2UserField.getText();
		if (s.length() == 0) s = null;
		cfg.setDds2UserName(s);
		s = (String)dds3UserField.getText();
		if (s.length() == 0) s = null;
		cfg.setDds3UserName(s);

		cfg.setMaxBytesHigh(cnvtInt(maxHighBytesField.getText(), 5000));
		cfg.setMaxBytesMedium(cnvtInt(maxMediumBytesField.getText(), 5000));
		cfg.setMaxBytesLow(cnvtInt(maxLowBytesField.getText(), 5000));

		cfg.setMaxMsgsHigh(cnvtInt(maxHighMsgsField.getText(), 20));
		cfg.setMaxMsgsMedium(cnvtInt(maxMediumMsgsField.getText(), 20));
		cfg.setMaxMsgsLow(cnvtInt(maxLowMsgsField.getText(), 20));

		cfg.setMaxSecondsHigh(cnvtInt(maxHighSecondsField.getText(), 30));
		cfg.setMaxSecondsMedium(cnvtInt(maxMediumSecondsField.getText(), 30));
		cfg.setMaxSecondsLow(cnvtInt(maxLowSecondsField.getText(), 30));

		cfg.setScrubHours(cnvtInt(scrubHoursField.getText(), 48));
		cfg.setMaxManualRetrans(cnvtInt(maxManualField.getText(), 20));
		cfg.setMaxAutoRetrans(cnvtInt(maxAutoField.getText(), 40));

		cfg.setDom2AHostName(dom2AHostField.getText());
		cfg.setDom2AUser(dom2AUserField.getText());
		cfg.setDom2ADirHigh(dom2AHighPriDirField.getText());
		cfg.setDom2ADirMedium(dom2AMediumPriDirField.getText());
		cfg.setDom2ADirLow(dom2ALowPriDirField.getText());
		
		cfg.setDom2BHostName(dom2BHostField.getText());
		cfg.setDom2BUser(dom2BUserField.getText());
		cfg.setDom2BDirHigh(dom2BHighPriDirField.getText());
		cfg.setDom2BDirMedium(dom2BMediumPriDirField.getText());
		cfg.setDom2BDirLow(dom2BLowPriDirField.getText());
		
		cfg.setDom2CHostName(dom2CHostField.getText());
		cfg.setDom2CUser(dom2CUserField.getText());
		cfg.setDom2CDirHigh(dom2CHighPriDirField.getText());
		cfg.setDom2CDirMedium(dom2CMediumPriDirField.getText());
		cfg.setDom2CDirLow(dom2CLowPriDirField.getText());

		cfg.setEnableLqm(lqmEnableCheck.isSelected());

		cfg.setLqmPort( cnvtInt(lqmPortField.getText(), 17004));
		cfg.setLqmIPAddress( lqmHostField.getText() );
		cfg.setLqmPendingTimeout(cnvtInt(pendingTimeoutField.getText(), 300));
		cfg.setLritUIPort(cnvtInt(uiPortField.getText(), 17005));
		cfg.setUIIPAddresses(uiHostsField.getText());
		cfg.setFileSenderHost(txtLritHost.getText());
		
		s= (String)(comboSate.getSelectedItem());
		
		cfg.setFileSenderState(s);
		
//		cfg.setPtpEnabled(ptpEnabledCheck.isSelected());
//		cfg.setPtpDir(ptpDirField.getText());
	}

	private void jbInit() throws Exception {
		GridBagConstraints gridBagConstraints101 = new GridBagConstraints();
		gridBagConstraints101.fill = GridBagConstraints.BOTH;
		gridBagConstraints101.gridy = 0;
		gridBagConstraints101.gridx = 3;
		gridBagConstraints101.insets = new Insets(5, 0, 5, 90);
		gridBagConstraints101.weightx = 1.0;
		GridBagConstraints gridBagConstraints91 = new GridBagConstraints();
		gridBagConstraints91.gridx = 2;
		gridBagConstraints91.fill = GridBagConstraints.BOTH;
		gridBagConstraints91.insets = new Insets(5, 45, 5, 0);
		gridBagConstraints91.gridy = 0;
		lblLritState = new JLabel();
		lblLritState.setText("Operation Mode:");
		GridBagConstraints gridBagConstraints83 = new GridBagConstraints();
		gridBagConstraints83.fill = GridBagConstraints.BOTH;
		gridBagConstraints83.gridy = 0;
		gridBagConstraints83.gridx = 1;
		gridBagConstraints83.insets = new Insets(5, 0, 5, 5);
		gridBagConstraints83.ipadx = 30;
		gridBagConstraints83.weightx = 1.0;
		GridBagConstraints gridBagConstraints73 = new GridBagConstraints();
		gridBagConstraints73.anchor = GridBagConstraints.NORTHWEST;
		gridBagConstraints73.gridx = 0;
		gridBagConstraints73.gridy = 0;
		gridBagConstraints73.ipadx = 0;
		gridBagConstraints73.insets = new Insets(5, 16, 5, 5);
		gridBagConstraints73.fill = GridBagConstraints.NONE;
		lblLritHost = new JLabel();
		lblLritHost.setText("LRIT File Sender Host:");
		GridBagConstraints gridBagConstraints53 = new GridBagConstraints(0, 3, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 4, 5), 30, 0);
		gridBagConstraints53.gridy = 4;
		gridBagConstraints53.weighty = 1.1D;
		GridBagConstraints gridBagConstraints43 = new GridBagConstraints(0, 5, 3, 1, 1.0, 1.0, GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(3, 4, 0, 6), 14, 2);
		gridBagConstraints43.insets = new Insets(5, 4, 0, 6);
		gridBagConstraints43.gridy = 0;
		gridBagConstraints43.ipady = 0;
		gridBagConstraints43.gridwidth = 1;
		gridBagConstraints43.weighty = 0.7D;
		gridBagConstraints43.gridx = 0;
			
		GridBagConstraints gridBagConstraints17 = new GridBagConstraints(0, 1, 2, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 2, 0, 5), 0, -4);
		gridBagConstraints17.gridx = 0;
		gridBagConstraints17.ipady = 0;
		gridBagConstraints17.weighty = 1.3D;
		gridBagConstraints17.gridy = 2;
		GridBagConstraints gridBagConstraints16 = new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 2, 4, 0), 3, 3);
		gridBagConstraints16.insets = new Insets(0, 2, 0, 5);
		gridBagConstraints16.gridy = 3;
		gridBagConstraints16.ipady = 0;
		gridBagConstraints16.ipadx = 0;
		gridBagConstraints16.weighty = 1.4D;
		gridBagConstraints16.gridx = 0;
		
		ddsServersPanel.setBorder(BorderFactory.createTitledBorder(
			BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), 
			"DDS Servers", TitledBorder.LEADING, TitledBorder.DEFAULT_POSITION, 
			new Font("Dialog", Font.BOLD, 12), new Color(51, 51, 51)));
			
			
			
		titledBorder1 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white,new Color(165, 163, 151)),"DDS Servers");
		titledBorder2 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white,new Color(165, 163, 151)),"LRIT File Limits");
			
		panel1.setLayout(borderLayout1);
		okButton.setText("OK");
		okButton.addActionListener(new LritDcsConfigDialog_okButton_actionAdapter(this));
		cancelButton.setText("Cancel");
		cancelButton.addActionListener(new LritDcsConfigDialog_cancelButton_actionAdapter(this));
		jPanel1.setLayout(flowLayout1);
		flowLayout1.setHgap(20);
		flowLayout1.setVgap(10);
		this.setModal(true);
		this.setTitle("LRIT DCS Configuration");
		outerPanel.setLayout(gridBagLayout5);
		ddsServersPanel.setLayout(new GridBagLayout());
		dds1UserField.setText("lritdcs");
		dds2UserField.setText("lritdcs");
		dds3UserField.setText("lritdcs");
		jPanel4.setBorder(titledBorder2);
		jPanel4.setLayout(gridBagLayout2);
		jLabel10.setText("High Priority: ");
		jLabel11.setText("Medium Priority: ");
		jLabel12.setText("Low Priority: ");
		jLabel13.setText("Max Bytes");
		jLabel14.setText("Max Msgs");
		jLabel15.setText("Max Seconds");
		maxHighBytesField.setText("");
		maxMediumBytesField.setText("");
		maxLowBytesField.setText("");
		maxHighMsgsField.setText("");
		maxMediumMsgsField.setText("");
		maxLowMsgsField.setText("");
		maxMediumSecondsField.setText("");
		maxHighSecondsField.setText("");
		maxLowSecondsField.setText("");
		jLabel16.setText("Scrub Hours: ");
		scrubHoursField.setText("48");
		jLabel17.setText("Max Manual Retrans Q\'d: ");
		maxManualField.setText("20");
		jLabel18.setText("Max Auto Retrans Q\'d: ");
		maxAutoField.setText("40");
		
		
		
		
		
		dom2AHostField.setText("");
		dom2AUserField.setText("");
		dom2AHighPriDirField.setText("");
		dom2AMediumPriDirField.setText("");
		dom2ALowPriDirField.setText("");
		dom2ALowPriDirField.setCursor(new Cursor(Cursor.TEXT_CURSOR));

		
		pendingTimeoutField.setText("120 ");
		lqmPortField.setText("17004");
		
		lqmHostField.setText("");
		
		uiPortField.setText("17005");
		
		jPanel7.setLayout(new GridBagLayout());
		jPanel7.add(lblLritHost, gridBagConstraints73);
		jPanel7.add(getTxtLritHost(), gridBagConstraints83);
		jPanel7.add(getComboSate(), gridBagConstraints101);
		flowLayout2.setAlignment(FlowLayout.RIGHT);
		uiHostsField.setText("");
		getContentPane().add(panel1);
		panel1.add(jPanel1, BorderLayout.SOUTH);
		jPanel1.add(okButton, null);
		jPanel1.add(cancelButton, null);
		panel1.add(outerPanel, BorderLayout.CENTER);
		outerPanel.add(lqmUiPanel, gridBagConstraints53);
		outerPanel.add(domain2Panel, gridBagConstraints16);
		
		outerPanel.add(ddsServersPanel, 
			new GridBagConstraints(0, 1, 2, 1, 1.0, 1.0, 
				GridBagConstraints.CENTER, GridBagConstraints.BOTH, 
				new Insets(5, 2, 0, 5), 0, 0));

		outerPanel.add(jPanel4, gridBagConstraints17);
		outerPanel.add(jPanel7, gridBagConstraints43);
		ddsServersPanel.add(new JLabel("(seconds)"), 
			new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(8, 10, 0, 0), 0, 0));
		ddsServersPanel.add(new JLabel("Host or IP Address"), 
			new GridBagConstraints(3, 0, 1, 1, 1.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(8, 30, 0, 0), 0, 0));
		ddsServersPanel.add(new JLabel("Port"), 
			new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(8, 30, 0, 0), 0, 0));
		ddsServersPanel.add(new JLabel("User"), 
			new GridBagConstraints(5, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(8, 30, 0, 0), 0, 0));

		ddsServersPanel.add(new JLabel("Server Timeout: "),
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 15, 2, 0), 0, 0));
		ddsServersPanel.add(serverTimeoutCombo,	
			new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 0, 2, 0), 0, 0));
		ddsServersPanel.add(new JLabel("Server 1: "), 
			new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 15, 2, 0), 0, 0));
		ddsServersPanel.add(dds1HostCombo, 
			new GridBagConstraints(3, 1, 1, 1, 1.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 0, 2, 0), 133, 0));
		ddsServersPanel.add(dds1PortCombo,	
			new GridBagConstraints(4, 1, 1, 1, 1.0, 0.0, 
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 10, 2, 0), 30, 0));
		ddsServersPanel.add(dds1UserField, 
			new GridBagConstraints(5, 1, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 10, 2, 10), 60, 0));

		ddsServersPanel.add(new JLabel("Retry After: "), 
			new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
            	GridBagConstraints.EAST, GridBagConstraints.NONE, 
            	new Insets(2, 15, 2, 0), 0, 0));
		ddsServersPanel.add(serverRetryCombo, 
			new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 0, 2, 0), 0, 0));
		ddsServersPanel.add(new JLabel("Server 2: "), 
			new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 15, 2, 0), 0, 0));
		ddsServersPanel.add(dds2HostCombo, 
			new GridBagConstraints(3, 2, 1, 1, 1.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 0, 2, 0), 133, 0));
		ddsServersPanel.add(dds2PortCombo,	
			new GridBagConstraints(4, 2, 1, 1, 1.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 10, 2, 0), 30, 0));
		ddsServersPanel.add(dds2UserField,
			new GridBagConstraints(5, 2, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 10, 2, 10), 60, 0));

		ddsServersPanel.add(new JLabel("Server 3: "),
			new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(2, 15, 8, 0), 0, 0));
		ddsServersPanel.add(dds3HostCombo,
			new GridBagConstraints(3, 3, 1, 1, 1.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 0, 8, 0), 133, 0));
		ddsServersPanel.add(dds3PortCombo,
			new GridBagConstraints(4, 3, 1, 1, 1.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 10, 8, 0), 30, 0));
		ddsServersPanel.add(dds3UserField,
			new GridBagConstraints(5, 3, 1, 1, 1.0, 0.0,
            	GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
            	new Insets(2, 10, 8, 10), 60, 0));

		domain2Panel.setBorder(
			new TitledBorder(
				BorderFactory.createEtchedBorder(Color.white,new Color(165, 163, 151)),
				"Domain 2 Parameters"));
		domain2Panel.setLayout(new GridBagLayout());
		
		domain2Panel.add(new JLabel("Host or IP Address"),
			new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, 
				GridBagConstraints.CENTER, GridBagConstraints.NONE, 
				new Insets(2, 0, 2, 0), 0, 0));
		domain2Panel.add(new JLabel("SSH User"), 
			new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, 
				GridBagConstraints.CENTER, GridBagConstraints.NONE, 
				new Insets(2, 0, 2, 0), 0, 0));
		domain2Panel.add(new JLabel("High Pri Dir"), 
			new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0, 
				GridBagConstraints.CENTER, GridBagConstraints.NONE, 
				new Insets(2, 0, 2, 0), 0, 0));
		domain2Panel.add(new JLabel("Medium Pri Dir"), 
			new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0, 
				GridBagConstraints.CENTER, GridBagConstraints.NONE, 
				new Insets(2, 0, 2, 0), 0, 0));
		domain2Panel.add(new JLabel("Low Pri Dir"), 
			new GridBagConstraints(5, 0, 1, 1, 0.0, 0.0, 
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(2, 0, 2, 0), 0, 0));

		domain2Panel.add(new JLabel("Domain 2A:"), 
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 0, 2, 0), 0, 0));
		domain2Panel.add(dom2AHostField, 
			new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 0, 2, 5), 150, 0));
		domain2Panel.add(dom2AUserField, 
			new GridBagConstraints(2, 1, 1, 1, 1.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 5, 2, 5), 70, 0));
		domain2Panel.add(dom2AHighPriDirField, 
			new GridBagConstraints(3, 1, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 5, 2, 5), 130, 0));
		domain2Panel.add(dom2AMediumPriDirField, 
			new GridBagConstraints(4, 1, 1, 1, 1.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 5, 2, 5), 130, 1));
		domain2Panel.add(dom2ALowPriDirField, 
			new GridBagConstraints(5, 1, 1, 1, 1.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 5, 2, 15), 130, 1));

		domain2Panel.add(new JLabel("Domain 2B:"), 
			new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 0, 2, 0), 0, 0));
		domain2Panel.add(dom2BHostField, 
			new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 0, 2, 5), 150, 0));
		domain2Panel.add(dom2BUserField, 
			new GridBagConstraints(2, 2, 1, 1, 1.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 5, 2, 5), 70, 0));
		domain2Panel.add(dom2BHighPriDirField, 
			new GridBagConstraints(3, 2, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 5, 2, 5), 130, 0));
		domain2Panel.add(dom2BMediumPriDirField, 
			new GridBagConstraints(4, 2, 1, 1, 1.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 5, 2, 5), 130, 1));
		domain2Panel.add(dom2BLowPriDirField, 
			new GridBagConstraints(5, 2, 1, 1, 1.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 5, 2, 15), 130, 1));

		domain2Panel.add(new JLabel("Domain 2C:"), 
			new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 0, 2, 0), 0, 0));
		domain2Panel.add(dom2CHostField, 
			new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 0, 2, 5), 150, 0));
		domain2Panel.add(dom2CUserField, 
			new GridBagConstraints(2, 3, 1, 1, 1.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 5, 2, 5), 70, 0));
		domain2Panel.add(dom2CHighPriDirField, 
			new GridBagConstraints(3, 3, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 5, 2, 5), 130, 0));
		domain2Panel.add(dom2CMediumPriDirField, 
			new GridBagConstraints(4, 3, 1, 1, 1.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 5, 2, 5), 130, 1));
		domain2Panel.add(dom2CLowPriDirField, 
			new GridBagConstraints(5, 3, 1, 1, 1.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 5, 2, 15), 130, 1));

//		domain2Panel.add(ptpEnabledCheck, 
//			new GridBagConstraints(0, 4, 2, 1, 0.0, 0.0, 
//				GridBagConstraints.EAST, GridBagConstraints.NONE,
//				new Insets(2, 0, 2, 0), 0, 0));
//		domain2Panel.add(ptpDirField, 
//			new GridBagConstraints(2, 4, 2, 1, 1.0, 0.0, 
//				GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
//				new Insets(2, 0, 2, 0), 0, 0));
//		
		
		
		jPanel4.add(jLabel10,	 new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(9, 20, 0, 0), 0, 0));
		jPanel4.add(jLabel11,	  new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(11, 15, 0, 0), 0, 0));
		jPanel4.add(jLabel12,	  new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(11, 15, 8, 0), 0, 0));
		jPanel4.add(maxMediumMsgsField,	new GridBagConstraints(2, 2, 1, 1, 1.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(8, 11, 0, 0), 46, 0));
		jPanel4.add(maxLowMsgsField,	new GridBagConstraints(2, 3, 1, 1, 1.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(8, 11, 8, 0), 46, 0));
		jPanel4.add(maxMediumSecondsField,	new GridBagConstraints(3, 2, 1, 1, 1.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(8, 14, 0, 0), 51, 0));
		jPanel4.add(maxLowSecondsField,	new GridBagConstraints(3, 3, 1, 1, 1.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(8, 14, 8, 0), 51, 0));
		jPanel4.add(maxHighMsgsField,	new GridBagConstraints(2, 1, 1, 1, 1.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(6, 11, 0, 0), 46, 0));
		jPanel4.add(jLabel14,	new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 15, 0, 0), 0, 0));
		jPanel4.add(jLabel15,	new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 12, 0, 0), 0, 0));
		jPanel4.add(maxHighSecondsField,	new GridBagConstraints(3, 1, 1, 1, 1.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(6, 14, 0, 0), 51, 0));
		jPanel4.add(jLabel13,	new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 15, 0, 11), 0, 0));
		jPanel4.add(maxHighBytesField,	new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(6, 0, 0, 0), 63, 0));
		jPanel4.add(maxMediumBytesField,	new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(8, 0, 0, 0), 63, 0));
		jPanel4.add(maxLowBytesField,	new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(8, 0, 8, 0), 63, 0));
		jPanel4.add(scrubHoursField,	new GridBagConstraints(5, 1, 1, 1, 1.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(6, 6, 0, 25), 13, 0));
		jPanel4.add(jLabel16,	 new GridBagConstraints(4, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(9, 0, 0, 0), 0, 0));
		jPanel4.add(jLabel17,	 new GridBagConstraints(4, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(11, 25, 0, 0), 0, 0));
		jPanel4.add(maxManualField,	new GridBagConstraints(5, 2, 1, 1, 1.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(8, 6, 0, 25), 12, 0));
		jPanel4.add(jLabel18,	 new GridBagConstraints(4, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(11, 25, 8, 0), 0, 0));
		jPanel4.add(maxAutoField,	new GridBagConstraints(5, 3, 1, 1, 1.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(8, 6, 8, 25), 12, 0));

		
		lqmUiPanel.setLayout(new GridBagLayout());
		lqmUiPanel.setBorder(
			new TitledBorder(
				BorderFactory.createEtchedBorder(Color.white,new Color(165, 163, 151)),
				"Other Settings"));

		lqmEnableCheck.setText("Listen for LQM on port: ");
	    lqmEnableCheck.addActionListener(
	    	new java.awt.event.ActionListener() 
	    	{
	            public void actionPerformed(ActionEvent e) {
	                lqmEnableChecked();
	            }
	        });
		lqmUiPanel.add(lqmEnableCheck, 
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 15, 2, 0), 0, 0));
		lqmUiPanel.add(lqmPortField,
			new GridBagConstraints(1, 0, 1, 1, .1, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 0, 2, 0), 60, 0));
		lqmUiPanel.add(new JLabel(" from host:"), 
			new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 10, 2, 0), 0, 0));
		lqmUiPanel.add(lqmHostField, 
			new GridBagConstraints(3, 0, 1, 1, .9, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 0, 2, 15), 120, 0));

		lqmUiPanel.add(new JLabel("Pending Timeout Sec: "), 
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 15, 2, 0), 0, 0));
		lqmUiPanel.add(pendingTimeoutField,
			new GridBagConstraints(1, 1, 1, 1, .1, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 0, 2, 0), 60, 0));
		
		lqmUiPanel.add(new JLabel("Listen for GUI on Port: "), 
			new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 15, 2, 0), 0, 0));
		lqmUiPanel.add(uiPortField,
			new GridBagConstraints(1, 2, 1, 1, .1, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 0, 2, 0), 0, 0));
		lqmUiPanel.add(new JLabel(" from hosts:"), 
			new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 10, 2, 0), 0, 0));
		lqmUiPanel.add(uiHostsField, 
			new GridBagConstraints(3, 2, 1, 1, .9, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 0, 2, 15), 0, 0));
		

		
		dds1HostCombo.setEditable(true);
		dds2HostCombo.setEditable(true);
		dds3HostCombo.setEditable(true);
		dds1PortCombo.setEditable(true);
		dds2PortCombo.setEditable(true);
		dds3PortCombo.setEditable(true);
		jPanel7.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "LRIT File Sender", TitledBorder.LEADING, TitledBorder.DEFAULT_POSITION, new Font("Dialog", Font.BOLD, 12), new Color(51, 51, 51)));
		jPanel7.add(lblLritState, gridBagConstraints91);
	}

	void okButton_actionPerformed(ActionEvent e)
	{
		
		saveValues();
		saveConfigValues();
		
		isOK = true;
		closeDlg();
	}

	void cancelButton_actionPerformed(ActionEvent e)
	{
		isOK = false;
		closeDlg();
	}

	void lqmEnableChecked() 
	{

	}

	private int cnvtInt(Object ob, int dflt)
	{
		String s = (String)ob;
		if (s == null)
			return dflt;
		try { return Integer.parseInt(s.trim()); }
		catch(NumberFormatException ex) { return dflt; }
	}

	public boolean okPressed() { return isOK; }

	private void closeDlg()
	{
		setVisible(false);
		dispose();
	}

	/**
	 * This method initializes txtLritHost	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	public JTextField getTxtLritHost() {
		if (txtLritHost == null) {
			txtLritHost = new JTextField();
			txtLritHost.setEnabled(false);
		}
		return txtLritHost;
	}

	/**
	 * This method initializes comboSate	
	 * 	
	 * @return javax.swing.JComboBox	
	 */
	private JComboBox getComboSate() {
		if (comboSate == null) {
			comboSate = new JComboBox();
			comboSate.addItem("Active");
			comboSate.addItem("Dormant");
			
		}
		return comboSate;
	}
	
	/**
	 * @return the configProps
	 */
	public java.util.Properties getConfigProps() {
		return configProps;
	}

	/**
	 * @param configProps the configProps to set
	 */
	public void setConfigProps(java.util.Properties configProps) {
		this.configProps = configProps;
	}

	public static void main(String[] args) {
		LritDcsConfigDialog dlg = new LritDcsConfigDialog();
		dlg.setVisible(true);
	}
	
}



class LritDcsConfigDialog_okButton_actionAdapter implements java.awt.event.ActionListener {
	LritDcsConfigDialog adaptee;

	LritDcsConfigDialog_okButton_actionAdapter(LritDcsConfigDialog adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.okButton_actionPerformed(e);
	}
}

class LritDcsConfigDialog_cancelButton_actionAdapter implements java.awt.event.ActionListener {
	LritDcsConfigDialog adaptee;

	LritDcsConfigDialog_cancelButton_actionAdapter(LritDcsConfigDialog adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.cancelButton_actionPerformed(e);
	}
}

