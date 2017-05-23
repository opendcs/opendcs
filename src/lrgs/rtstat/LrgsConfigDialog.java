/*
*  $Id$
*/
package lrgs.rtstat;

import ilex.net.BasicClient;
import ilex.util.AuthException;
import ilex.util.LoadResourceBundle;
import ilex.util.TextUtil;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

import lrgs.ddsrecv.DdsRecvConnectCfg;
import lrgs.ddsrecv.DdsRecvSettings;
import lrgs.ddsrecv.NetlistGroupAssoc;
import lrgs.drgs.DrgsConnectCfg;
import lrgs.drgs.DrgsInputSettings;
import lrgs.ldds.LddsClient;
import lrgs.lrgsmain.LrgsConfig;
import decodes.dbeditor.TimeZoneSelector;
import decodes.gui.GuiDialog;
import decodes.gui.PropertiesEditPanel;
import decodes.gui.SortingListTable;
import decodes.gui.SortingListTableModel;
import decodes.util.ResourceFactory;

public class LrgsConfigDialog extends GuiDialog
{
	private static ResourceBundle labels = 
		RtStat.getLabels();
	private static ResourceBundle genericLabels = 
		RtStat.getGenericLabels();
	private DefaultListModel netlistListModel = new DefaultListModel();
	private DefaultTableModel netlistTableModel = new DefaultTableModel();
	private DdsSortingListTableModel ddsTableModel;
	private DrgsSortingListTableModel drgsTableModel;
	private JPanel centralPane = null;
	private JTabbedPane tabbedPane = null;
	private JPanel buttonPanel = null;  //  @jve:decl-index=0:visual-constraint="774,380"
	private JButton okButton = null;
	private JButton cancelButton = null;
	private JButton applyButton = null;
	private JPanel archiveConfigTab = null;
	private JPanel domsatConfigTab = null;
	private JPanel ddsServerConfigTab = null;
	private JPanel ddsRecvConfigTab = null;
	private JPanel drgsConfigTab = null;
	private JPanel miscConfigTab = null;
	private NoaaportConfigPanel noaaConfigTab = null;
	private JPanel lrgsArchiveConfigPanel = null;
	private JPanel dataSourcePrefPanel = null;
	private JLabel archiveDirectoryLabel = null;
	private JLabel archiveLengthLabel = null;
	private JLabel archiveTimeoutLabel = null;
	private JLabel localStatusFileLabel = null;
	private JLabel statusPeriodLabel = null;
	private JLabel sharedNetlistDirectoryLabel = null;
	private JTextField archiveDirField = null;
	private JTextField archiveLengthField = null;
	private JTextField archiveTimeoutField = null;
	private JTextField localStatusFileField = null;
	private JTextField statusPeriodField = null;
	private JTextField sharedNetlistDirectoryField = null;
	private JTextField ddsTimeoutField = new JTextField(9);
	private JLabel preference1Label = null;
	private JLabel preference2Label = null;
	private JLabel preference3Label = null;
	private JLabel preference4Label = null;
	private JComboBox mergePref1Combo = null;
	private JComboBox mergePref2Combo = null;
	private JComboBox mergePref3Combo = null;
	private JComboBox mergePref4Combo = null;
	private JPanel domsatPanel = null;
	private JCheckBox initializeDOMSATcheck = null;
	private JLabel domsatHardwareLabel = null;
	private JCheckBox domsatLinkCheck = null;
	private JLabel domsatTimeoutLabel = null;
	private JComboBox domsatHardwareCombo = null;
	private JTextField domsatTimeoutField = null;
	private JTextField domsatDpcHost = new JTextField();;
	private JTextField domsatDpcPort = new JTextField();;
	private JTextField ddsListenPortField = null;
	private JTextField ddsBindAddrField = null;
	private JTextField ddsMaxClientsField = null;
	private JTextField ddsParentDirectoryField = null;
	private JTextField ddsLogFileField = null;
	private JCheckBox ddsRequireAuthCheck = null;
	private JPanel recoverPanel = null;
	private JLabel recoveryLabel = null;
//	private JComboBox recoveryCombo = null;
	private JCheckBox enableDDSReceiveCheck = null;
	private JLabel emptyLabel1 = null;
	private JLabel emptyLabel2 = null;
	private JLabel emptyLabel5 = null;
	private JLabel nicLabel = null;
	private JPanel connectionsPanel = null;  //  @jve:decl-index=0:visual-constraint="884,87"
	private JPanel connectionsButtonPanel = null;
	private JButton ddsConAddButton = null;  //  @jve:decl-index=0:visual-constraint="862,324"
	private JButton ddsConEditButton = null;
	private JButton ddsConDeleteButton = null;
	private JButton ddsConMoveUpButton = null;
	private JButton ddsConMoveDownButton = null;
	private JButton ddsConTestButton = null;
	private JPanel networkListsPanel = null;  //  @jve:decl-index=0:visual-constraint="862,260"
	private JPanel networkListsButtonPanel = null;
	private JButton networkListsAddButton = null;
	private JButton networkListsDeleteButton = null;
	private JPanel jPanel = null;
	private JPanel drgsConfigPanel = null;
	private JPanel drgsButtonPanel = null;
	private JButton drgsAddButton = null;  //  @jve:decl-index=0:visual-constraint="891,230"
	private JButton drgsEditButton = null;
	private JButton drgsDeleteButton = null;
	private JButton drgsTestButton = null;
	private JScrollPane networkListsScrollPane = null;
	private JList networkList = null;
	private JTable networkListTable = null;
	private JPanel drgsTablePanel = null;
	private JCheckBox enableDRGSCheck = null;
	private JTable drgsConTable = null;
	private JTable ddsConTable = null;
	private JPanel SqlDatabasePanel = null;  //  @jve:decl-index=0:visual-constraint="663,689"
	private JPanel containerPanel = null;  //  @jve:decl-index=0:visual-constraint="373,705"
	private JLabel sqlUrlLabel = null;
	private JLabel sqlReadLabel = null;
	private JLabel sqlWriteLabel = null;
	private JTextField sqlUrlField = null;
	private JTextField sqlReadField = null;
	private JTextField sqlWriteField = null;
	private JLabel sqlTimeZoneLabel = null;
	private TimeZoneSelector sqlTimeZoneCombo = null;
	private JLabel sqlDriverLabel = null;
	private JLabel sqlKeyLabel = null;
	private JTextField sqlDriverField = null;
	private JTextField sqlKeyField = null;
	private JScrollPane connectionsScrollPane = null;
	private JScrollPane ddsReceiveScrollPane = null;
	private JCheckBox pdtValidationCheck = null;
	private JTextField pdtUrlField = new JTextField();
	private JTextField cdtUrlField = new JTextField();
	private JCheckBox localAdminOnlyCheck = new JCheckBox();
	private JTextField localSandboxDir = new JTextField();
	private JCheckBox goesXmitCheck = new JCheckBox("Save GOES Xmit Records");
	private JCheckBox requireStrongAuthCheck = new JCheckBox();

	private String mergePrefs[] = { "(unspecified)", "DRGS",
		"DOMSAT", "DDS-Receive" };

	private LrgsConfig lrgsConfig;  //  @jve:decl-index=0:
	public DdsRecvSettings ddsSettings;
	private DrgsInputSettings drgsSettings;
	private DdsClientIf ddsClientIf = null;
	private boolean netlistsModified = false;
	private PropertiesEditPanel miscPanel = null;
	private JPanel networkDcpTab = null;
	private DrgsInputSettings networkDcpSettings;
	private NetworkDcpCfgPanel networkDcpCfgPanel;
	private JCheckBox preferredGoodCheck = new JCheckBox();
	private JCheckBox acceptDomsatARMsCheck = new JCheckBox();
	private IridiumCfgPanel iridiumCfgTab = null;
	private LritCfgPanel lritCfgPanel = null;
	private EdlConfigPanel edlConfigPanel = null;
	private JTextField ddsMinHourlyField = new JTextField(9);
	private JTextField drgsMinHourlyField = new JTextField(9);
	
	public LrgsConfigDialog(JFrame parent, String title)
	{
		super(parent, title, true);
		pdtValidationCheck 
			= new JCheckBox(labels.getString(
					"LrgsConfigDialog.enableScheChanVal"));
		centralPane = new JPanel();
		centralPane.setLayout(new BorderLayout());
		centralPane.add(getTabbedPane(), BorderLayout.CENTER);
		centralPane.add(getButtonPanel(),BorderLayout.SOUTH);
		centralPane.setPreferredSize(new Dimension(720, 700));
		getContentPane().add(centralPane);
		pack();
	}

	/**
	 * Clears all the controls, as if creating a new LRGS Configuration.
	 */
	public void clear()
	{
		lrgsConfig=null;
		ddsSettings=null;
		drgsSettings=null;
		
		//Archive Tab
		getArchiveDirField().setText("");
		getArchiveLengthField().setText("");
		getArchiveTimeoutField().setText("");
		getLocalStatusFileField().setText("");
		getStatusPeriodField().setText("");
		getSharedNetlistDirectoryField().setText("");
		preferredGoodCheck.setSelected(false);
		mergePref1Combo.setSelectedIndex(0);
		mergePref2Combo.setSelectedIndex(0);
		mergePref3Combo.setSelectedIndex(0);
		mergePref4Combo.setSelectedIndex(0);
		getSqlUrlField().setText("");
		getSqlReadField().setText("");
		getSqlWriteField().setText("");
		getSqlTimeZoneCombo().setSelectedItem("UTC");
		sqlDriverField.setText("");
		sqlKeyField.setText("");
		ddsTimeoutField.setText("");
		ddsMinHourlyField.setText("");
		drgsMinHourlyField.setText("");
		pdtValidationCheck.setSelected(false);
		pdtUrlField.setText("");
		cdtUrlField.setText("");
		goesXmitCheck.setSelected(false);
		
		//Domsat Tab
		getInitializeDOMSATcheck().setSelected(false);
		getDomsatHardwareCombo().setSelectedItem(0);
		getDomsatTimeoutField().setText("");
		getDomsatLinkCheck().setSelected(false);
		domsatDpcHost.setText(LrgsConfig.def_dpcHost);
		domsatDpcPort.setText("" + LrgsConfig.def_dpcPort);
		chkDpcEnabled();
		acceptDomsatARMsCheck.setSelected(true);
		
		//dds server tab
		getDdsListenPortField().setText("");
		getDdsBindAddrField().setText("");
		getDdsMaxClientsField().setText("");
		getDdsParentDirectoryField().setText("");
		getDdsLogFileField().setText("");
		getDdsRequireAuthCheck().setSelected(false);
		
		//DDS Receive tab
//		recoveryCombo.setSelectedIndex(0);
		enableDDSReceiveCheck.setSelected(true);
		//netlistListModel.clear();
		netlistTableModel.getDataVector().removeAllElements();
		ddsTableModel.clear();

		//DRGS tab
		enableDRGSCheck.setSelected(false);
		drgsTableModel.clear();

		tabbedPane.setSelectedIndex(0);
		
		// Network DCP Tab
		this.networkDcpCfgPanel.clear();
	}

	public void setConfig(LrgsConfig tmpLrgsConfig, 
		DdsRecvSettings tmpDdsSettings, DrgsInputSettings tmpDrgsSettings,
		DrgsInputSettings networkDcpSettings)
	{
		this.lrgsConfig=tmpLrgsConfig;
		this.ddsSettings=tmpDdsSettings;
		this.drgsSettings=tmpDrgsSettings;
		this.networkDcpSettings = networkDcpSettings;
		
		//Archive Tab
		getArchiveDirField().setText(lrgsConfig.archiveDir);
		getArchiveLengthField().setText(String.valueOf(lrgsConfig.numDayFiles));
		getArchiveTimeoutField().setText(String.valueOf(lrgsConfig.timeoutSeconds));
		getLocalStatusFileField().setText(lrgsConfig.htmlStatusFile);
		getStatusPeriodField().setText(String.valueOf(lrgsConfig.htmlStatusSeconds));
		getSharedNetlistDirectoryField().setText(lrgsConfig.ddsNetlistDir);
		preferredGoodCheck.setSelected(lrgsConfig.archivePreferredGood);
		pdtValidationCheck.setSelected(lrgsConfig.getDoPdtValidation());
		pdtUrlField.setText(lrgsConfig.getPdtUrl());
		cdtUrlField.setText(lrgsConfig.getChannelMapUrl());
		for(int i=1; i<mergePrefs.length; i++)
			if (TextUtil.strEqualIgnoreCase(lrgsConfig.mergePref1, mergePrefs[i]))
			{
				getMergePref1Combo().setSelectedIndex(i);
				break;
			}
		for(int i=1; i<mergePrefs.length; i++)
			if (TextUtil.strEqualIgnoreCase(lrgsConfig.mergePref2, mergePrefs[i]))
			{
				getMergePref2Combo().setSelectedIndex(i);
				break;
			}
		for(int i=1; i<mergePrefs.length; i++)
			if (TextUtil.strEqualIgnoreCase(lrgsConfig.mergePref3, mergePrefs[i]))
			{
				getMergePref3Combo().setSelectedIndex(i);
				break;
			}
		for(int i=1; i<mergePrefs.length; i++)
			if (TextUtil.strEqualIgnoreCase(lrgsConfig.mergePref4, mergePrefs[i]))
			{
				getMergePref4Combo().setSelectedIndex(i);
				break;
			}
		getSqlUrlField().setText(lrgsConfig.dbUrl);
		getSqlReadField().setText(lrgsConfig.sqlReadDateFormat);
		getSqlWriteField().setText(lrgsConfig.sqlWriteDateFormat);
		getSqlTimeZoneCombo().setSelectedItem(lrgsConfig.sqlTimeZone);
		if (lrgsConfig.JdbcDriverClass != null)
			sqlDriverField.setText(lrgsConfig.JdbcDriverClass);
		if (lrgsConfig.keyGeneratorClass != null)
			sqlKeyField.setText(lrgsConfig.keyGeneratorClass);
		goesXmitCheck.setSelected(lrgsConfig.storeXmitRecords);
		noaaConfigTab.fillFields(lrgsConfig);
		iridiumCfgTab.fillFields(lrgsConfig);
		lritCfgPanel.fillFields(lrgsConfig);
		edlConfigPanel.fillFields(lrgsConfig);
		
		miscPanel.setProperties(lrgsConfig.getOtherProps());

		//Domsat Tab
		getInitializeDOMSATcheck().setSelected(lrgsConfig.loadDomsat);
		//getDomsatHardwareCombo().setEnabled(lrgsConfig.loadDomsat);
		getDomsatHardwareCombo().setSelectedItem(lrgsConfig.domsatClass);
		String s = lrgsConfig.dpcHost;
		if (s == null) s = "";
		domsatDpcHost.setText(s);
		domsatDpcPort.setText("" + lrgsConfig.dpcPort);
		chkDpcEnabled();
		getDomsatTimeoutField().setText(String.valueOf(lrgsConfig.domsatTimeout));
		getDomsatLinkCheck().setSelected(lrgsConfig.enableDomsatRecv);
		acceptDomsatARMsCheck.setSelected(lrgsConfig.acceptDomsatARMs);
		
		//dds server tab
		getDdsListenPortField().setText(String.valueOf(lrgsConfig.ddsListenPort));
		getDdsBindAddrField().setText(lrgsConfig.ddsBindAddr);
		getDdsMaxClientsField().setText(String.valueOf(lrgsConfig.ddsMaxClients));
		getDdsParentDirectoryField().setText(lrgsConfig.ddsUserRootDir);
		getDdsLogFileField().setText(lrgsConfig.ddsUsageLog);
		getDdsRequireAuthCheck().setSelected(lrgsConfig.ddsRequireAuth);
		localAdminOnlyCheck.setSelected(lrgsConfig.localAdminOnly);
		localSandboxDir.setText(lrgsConfig.ddsUserRootDirLocal);
		requireStrongAuthCheck.setSelected(lrgsConfig.reqStrongEncryption);
		
		//DDS Receive tab
//		if (lrgsConfig.recoverOutages)
//			recoveryCombo.setSelectedIndex(1);
//		else
//			recoveryCombo.setSelectedIndex(0);
		enableDDSReceiveCheck.setSelected(lrgsConfig.enableDdsRecv);
		ddsTimeoutField.setText("" + ddsSettings.timeout);
		ddsMinHourlyField.setText(
			lrgsConfig.ddsMinHourly > 0 ? ("" + lrgsConfig.ddsMinHourly) : "");

		for(NetlistGroupAssoc nga : ddsSettings.getNetlistGroupAssociations())
		{
			Object[] obj = { nga.getNetlistName(), nga.getGroupName() };
			netlistTableModel.addRow(obj);
		}

		ddsTableModel.setContents(ddsSettings);

		// DRGS Tab
		drgsTableModel.setContents(drgsSettings);
		enableDRGSCheck.setSelected(lrgsConfig.enableDrgsRecv);
		drgsMinHourlyField.setText(
			lrgsConfig.drgsMinHourly > 0 ? ("" + lrgsConfig.drgsMinHourly) : "");

		// Network DCP Tab
		networkDcpCfgPanel.setContents(networkDcpSettings,
			lrgsConfig.networkDcpEnable);
		
		miscPanel.setPropertiesOwner(lrgsConfig);
	}

	/**
	 * Copy data from controls back to the LrgsConfig object.
	 * @return true if anything was changed.
	 */
	private boolean copyBackLrgsConfig()
		throws ParseException
	{
		boolean changed = false;
		
		String fieldName = "Archive Directory";
		try
		{
			//Archive Tab
			String sv = getStringFieldValue(getArchiveDirField(), 
				lrgsConfig.def_archiveDir);
			if (!TextUtil.strEqual(sv, lrgsConfig.archiveDir))
			{
				lrgsConfig.archiveDir = sv;
				changed = true;
			}
			
			fieldName = "Archive Length";
			int iv = getIntFieldValue(getArchiveLengthField(), 
				lrgsConfig.def_numDayFiles);
			if (lrgsConfig.numDayFiles != iv)
			{
				lrgsConfig.numDayFiles = iv;
				changed = true;
			}

			fieldName = "Archive Timeout";
			iv = getIntFieldValue(getArchiveTimeoutField(), 
				lrgsConfig.def_timeoutSeconds);
			if (lrgsConfig.timeoutSeconds != iv)
			{
				lrgsConfig.timeoutSeconds = iv;
				changed = true;
			}

			fieldName = "Local Status File";
			sv = getStringFieldValue(getLocalStatusFileField(), 
				lrgsConfig.def_htmlStatusFile);
			if (!TextUtil.strEqual(lrgsConfig.htmlStatusFile, sv))
			{
				lrgsConfig.htmlStatusFile = sv;
				changed = true;
			}

			fieldName = "Status Period";
			iv = getIntFieldValue(getStatusPeriodField(), 
				lrgsConfig.def_htmlStatusSeconds);
			if (lrgsConfig.htmlStatusSeconds != iv)
			{
				lrgsConfig.htmlStatusSeconds = iv;
				changed = true;
			}

			fieldName = "Netlist Directory";
			sv = getStringFieldValue(getSharedNetlistDirectoryField(), 
				lrgsConfig.def_ddsNetlistDir);
			if (!TextUtil.strEqual(lrgsConfig.ddsNetlistDir, sv))
			{
				lrgsConfig.ddsNetlistDir = sv;
				changed = true;
			}

			
			boolean bv = preferredGoodCheck.isSelected();
			if (bv != lrgsConfig.archivePreferredGood)
			{
				lrgsConfig.archivePreferredGood = bv;
				changed = true;
			}

			fieldName = "Merge Pref1";
			iv = getMergePref1Combo().getSelectedIndex();
			sv = iv == 0 ? null : mergePrefs[iv];
			if (!TextUtil.strEqual(lrgsConfig.mergePref1, sv))
			{
				lrgsConfig.mergePref1 = sv;
				changed = true;
			}
			
			fieldName = "Merge Pref2";
			iv = getMergePref2Combo().getSelectedIndex();
			sv = iv == 0 ? null : mergePrefs[iv];
			if (!TextUtil.strEqual(lrgsConfig.mergePref2, sv))
			{
				lrgsConfig.mergePref2 = sv;
				changed = true;
			}

			fieldName = "Merge Pref3";
			iv = getMergePref3Combo().getSelectedIndex();
			sv = iv == 0 ? null : mergePrefs[iv];
			if (!TextUtil.strEqual(lrgsConfig.mergePref3, sv))
			{
				lrgsConfig.mergePref3 = sv;
				changed = true;
			}

			fieldName = "Merge Pref4";
			iv = getMergePref4Combo().getSelectedIndex();
			sv = iv == 0 ? null : mergePrefs[iv];
			if (!TextUtil.strEqual(lrgsConfig.mergePref4, sv))
			{
				lrgsConfig.mergePref4 = sv;
				changed = true;
			}

			fieldName = "SQL URL";
			sv = getStringFieldValue(getSqlUrlField(), lrgsConfig.def_dbUrl);
			if (!TextUtil.strEqual(lrgsConfig.dbUrl, sv))
			{
				lrgsConfig.dbUrl = sv;
				changed = true;
			}

			fieldName = "SQL Read Date Fmt";
			sv = getStringFieldValue(getSqlReadField(), 
				lrgsConfig.def_sqlReadDateFormat);
			if (!TextUtil.strEqual(lrgsConfig.sqlReadDateFormat, sv))
			{
				lrgsConfig.sqlReadDateFormat = sv;
				changed = true;
			}

			fieldName = "SQL Write Date Fmt";
			sv = getStringFieldValue(getSqlWriteField(), 
				lrgsConfig.def_sqlWriteDateFormat);
			if (!TextUtil.strEqual(lrgsConfig.sqlWriteDateFormat, sv))
			{
				lrgsConfig.sqlWriteDateFormat = sv;
				changed = true;
			}

			fieldName = "SQL Driver";
			sv = getStringFieldValue(sqlDriverField, 
				lrgsConfig.def_JdbcDriverClass);
			if (!TextUtil.strEqual(lrgsConfig.JdbcDriverClass, sv))
			{
				lrgsConfig.JdbcDriverClass = sv;
				changed = true;
			}

			fieldName = "SQL Key Generator";
			sv = getStringFieldValue(sqlKeyField, 
				lrgsConfig.def_keyGeneratorClass);
			if (!TextUtil.strEqual(lrgsConfig.keyGeneratorClass, sv))
			{
				lrgsConfig.keyGeneratorClass = sv;
				changed = true;
			}

			fieldName = "SQL Time Zone";
			sv = (String)getSqlTimeZoneCombo().getSelectedItem();
			if (!TextUtil.strEqual(lrgsConfig.sqlTimeZone, sv))
			{
				lrgsConfig.sqlReadDateFormat = sv;
				changed = true;
			}
			
			fieldName = "GOES Xmit Check";
			bv = goesXmitCheck.isSelected();
			if (bv != lrgsConfig.storeXmitRecords)
			{
				lrgsConfig.storeXmitRecords = bv;
				changed = true;
			}
			
			fieldName = "PDT Validation Check";
			bv = pdtValidationCheck.isSelected();
			if (bv != lrgsConfig.getDoPdtValidation())
			{
				lrgsConfig.doPdtValidation = bv;
				changed = true;
			}

			fieldName = "PDT URL";
			sv = pdtUrlField.getText().trim();
			if (!sv.equals(lrgsConfig.getPdtUrl()))
			{
				lrgsConfig.pdtUrl = sv;
				changed = true;
			}

			fieldName = "CDT URL";
			sv = cdtUrlField.getText().trim();
			if (!sv.equals(lrgsConfig.getChannelMapUrl()))
			{
				lrgsConfig.channelMapUrl = sv;
				changed = true;
			}
			
			// DOMSAT Tab
			fieldName = "DOMSAT Load";
			bv = getInitializeDOMSATcheck().isSelected();
			if (lrgsConfig.loadDomsat != bv)
			{
				lrgsConfig.loadDomsat = bv;
				changed = true;
			}

			fieldName = "DOMSAT Class";
			sv = (String)getDomsatHardwareCombo().getSelectedItem();
			if (!TextUtil.strEqual(lrgsConfig.domsatClass, sv))
			{
				lrgsConfig.domsatClass = sv;
				changed = true;
			}
			sv = domsatDpcHost.getText().trim();
			if (sv.length() == 0)
				sv = null;
			if (!TextUtil.strEqual(sv, lrgsConfig.dpcHost))
			{
				lrgsConfig.dpcHost = sv;
				changed = true;
			}
			
			try
			{
				int p = Integer.parseInt(domsatDpcPort.getText().trim());
				if (p != lrgsConfig.dpcPort)
				{
					lrgsConfig.dpcPort = p;
					changed = true;
				}
			}
			catch(Exception ex) { }

			fieldName = "DOMSAT Enable";
			bv = getDomsatLinkCheck().isSelected();
			if (lrgsConfig.enableDomsatRecv != bv)
			{
				lrgsConfig.enableDomsatRecv = bv;
				changed = true;
			}

			fieldName = "DOMSAT Timeout";
			iv = getIntFieldValue(getDomsatTimeoutField(), 
				lrgsConfig.def_domsatTimeout);
			if (lrgsConfig.domsatTimeout != iv)
			{
				lrgsConfig.domsatTimeout = iv;
				changed = true;
			}
			
			bv = acceptDomsatARMsCheck.isSelected();
			if (lrgsConfig.acceptDomsatARMs != bv)
			{
				lrgsConfig.acceptDomsatARMs = bv;
				changed = true;
			}

			// DDS Server Tab
			fieldName = "DDS Listen Port";
			iv = getIntFieldValue(getDdsListenPortField(), 
				lrgsConfig.def_ddsListenPort);
			if (lrgsConfig.ddsListenPort != iv)
			{
				lrgsConfig.ddsListenPort = iv;
				changed = true;
			}

			fieldName = "DDS Bind Addr";
			sv = getStringFieldValue(getDdsBindAddrField(), 
				lrgsConfig.def_ddsBindAddr);
			if (!TextUtil.strEqual(lrgsConfig.ddsBindAddr, sv))
			{
				lrgsConfig.ddsBindAddr = sv;
				changed = true;
			}

			fieldName = "DDS Max Clients";
			iv = getIntFieldValue(getDdsMaxClientsField(), 
				lrgsConfig.def_ddsMaxClients);
			if (lrgsConfig.ddsMaxClients != iv)
			{
				lrgsConfig.ddsMaxClients = iv;
				changed = true;
			}

			fieldName = "DDS User Parent Dir";
			sv = getStringFieldValue(getDdsParentDirectoryField(), 
				lrgsConfig.def_ddsUserRootDir);
			if (!TextUtil.strEqual(lrgsConfig.ddsUserRootDir, sv))
			{
				lrgsConfig.ddsUserRootDir = sv;
				changed = true;
			}

			fieldName = "DDS Usage Log";
			sv = getStringFieldValue(getDdsLogFileField(), 
				lrgsConfig.def_ddsUsageLog);
			if (!TextUtil.strEqual(lrgsConfig.ddsUsageLog, sv))
			{
				lrgsConfig.ddsUsageLog = sv;
				changed = true;
			}

			fieldName = "DDS Require Auth";
			bv = getDdsRequireAuthCheck().isSelected();
			if (lrgsConfig.ddsRequireAuth != bv)
			{
				lrgsConfig.ddsRequireAuth = bv;
				changed = true;
			}
			
			fieldName = "DDS Local Admin Only";
			bv = localAdminOnlyCheck.isSelected();
			if (lrgsConfig.localAdminOnly != bv)
			{
				lrgsConfig.localAdminOnly = bv;
				changed = true;
			}
			
			bv = requireStrongAuthCheck.isSelected();
			if (lrgsConfig.reqStrongEncryption != bv)
			{
				lrgsConfig.reqStrongEncryption = bv;
				changed = true;
			}

			fieldName = "Local DDS User Dir";
			sv = getStringFieldValue(localSandboxDir, 
				lrgsConfig.def_ddsUserRootDirLocal);
			if (!TextUtil.strEqual(lrgsConfig.ddsUserRootDirLocal, sv))
			{
				lrgsConfig.ddsUserRootDirLocal = sv;
				changed = true;
			}

			// on the DDS Recv Tab
			// MJM OpenDCS 6.2 does not support Outage recovery
//			iv = recoveryCombo.getSelectedIndex();
//			bv = iv == 0 ? false : true;
//			if (lrgsConfig.recoverOutages != bv)
//			{
//				lrgsConfig.recoverOutages = bv;
//				changed = true;
//			}
			lrgsConfig.recoverOutages = false;

			bv = enableDDSReceiveCheck.isSelected();
			if (lrgsConfig.enableDdsRecv != bv)
			{
				lrgsConfig.enableDdsRecv = bv;
				changed = true;
			}

			fieldName = "DDS Recv Timeout";
			iv = getIntFieldValue(ddsTimeoutField, 90);
			if (ddsSettings.timeout != iv)
			{
				ddsSettings.timeout = iv;
				changed = true;
			}
			
			fieldName = "DDS Min Hourly";
			iv = getIntFieldValue(ddsMinHourlyField, 0);
			if (lrgsConfig.ddsMinHourly != iv)
			{
				lrgsConfig.ddsMinHourly = iv;
				changed = true;
			}
			
			fieldName = "DRGS Min Hourly";
			iv = getIntFieldValue(drgsMinHourlyField, 0);
			if (lrgsConfig.drgsMinHourly != iv)
			{
				lrgsConfig.drgsMinHourly = iv;
				changed = true;
			}


			// On the DRGS Tab
			bv = enableDRGSCheck.isSelected();
			if (lrgsConfig.enableDrgsRecv != bv)
			{
				lrgsConfig.enableDrgsRecv = bv;
				changed = true;
			}

			// On the Network Dcp tab
			bv = networkDcpCfgPanel.networkDcpEnable();
			if (lrgsConfig.networkDcpEnable != bv)
			{
				lrgsConfig.networkDcpEnable = bv;
				changed = true;
			}

			// On the 'misc' tab
			if (miscPanel.hasChanged())
			{
				miscPanel.saveChanges();
				changed = true;
			}
		}
		catch(ParseException ex)
		{
			String msg = LoadResourceBundle.sprintf(
				labels.getString("LrgsConfigDialog.invalidValueErr"),
				fieldName);
			showError(msg);
			throw new ParseException(msg, 0);
		}
		return changed;
	}

	/**
	 * Copy data from controls back to the DdsRecvSettings object.
	 * @return true if anything was changed.
	 */
	private boolean copyBackDdsSettings()
	{
		if (!ddsTableModel.modified && !netlistsModified)
			return false;

		ddsSettings.resetToDefaults();
		for(DdsRecvConnectCfg cc : ddsTableModel.cons)
			ddsSettings.connectCfgs.add(cc);

		//DDS Receive tab
		
		for(int i = 0; i < netlistTableModel.getRowCount(); i++)
		{
			String netlistName  = (String)netlistTableModel.getValueAt(i,0);
			netlistName = netlistName.trim();
			if (netlistName.length() == 0)
				continue;
			
			String group = (String)netlistTableModel.getValueAt(i,1);
			if (group == null || group.trim().length() == 0)
				group = NetlistGroupAssoc.DEFAULT_GROUP;

			ddsSettings.addNetlistAssoc(netlistName, group);
		}
	
		return true;
	}

	/**
	 * Copy data from controls back to the DrgsSettings object.
	 * @return true if anything was changed.
	 */
	private boolean copyBackDrgsConfig()
	{
		if (!drgsTableModel.modified)
			return false;

		drgsSettings.resetToDefaults();

		for (DrgsConnectCfg cc : drgsTableModel.cons)
			drgsSettings.connections.add(cc);
		return true;
	}
	
	/**
	 * Copy data from controls back to the Network DCP Settings object.
	 * @return true if anything was changed.
	 */
	private boolean copyBackNetworkDcpConfig()
	{
		// On the Network DCP Tab
		if (!networkDcpCfgPanel.hasChanged())
			return false;
		
		networkDcpSettings.resetToDefaults();
		for(DrgsConnectCfg dcc : networkDcpCfgPanel.getConnections())
			networkDcpSettings.connections.add(dcc);

		return true;
	}

	public void setDdsClientIf(DdsClientIf dcif)
	{
		ddsClientIf = dcif;
	}
	
	/**
	 * This method initializes tabbedPane	
	 * 	
	 * @return javax.swing.JTabbedPane	
	 */
	private JTabbedPane getTabbedPane() 
	{
		if (tabbedPane == null) 
		{
			tabbedPane = new JTabbedPane();
			tabbedPane.addTab(labels.getString("LrgsConfigDialog.archiveTab"),
					null, getArchiveConfigTab(), null);
			tabbedPane.addTab(labels.getString("LrgsConfigDialog.DOMSATTab"),
					null, getDomsatConfigTab(), null);
			tabbedPane.addTab(labels.getString("LrgsConfigDialog.DDSServerTab"),
					null, getDdsServerConfigTab(), null);
			tabbedPane.addTab(labels.getString("LrgsConfigDialog.DDSReceiveTab"),
					null, getDdsRecvConfigTab(), null);
			tabbedPane.addTab(labels.getString("LrgsConfigDialog.DRGSTab"),
					null, getDrgsConfigTab(), null);
			tabbedPane.addTab(labels.getString("LrgsConfigDialog.networkDcpTab"),
				null, getNetworkDcpTab(), null);
			noaaConfigTab = new NoaaportConfigPanel(this);
			tabbedPane.addTab(noaaConfigTab.getLabel(), noaaConfigTab);
			iridiumCfgTab = new IridiumCfgPanel(this);
			tabbedPane.addTab(iridiumCfgTab.getLabel(), iridiumCfgTab);
			lritCfgPanel = new LritCfgPanel(this);
			tabbedPane.addTab(lritCfgPanel.getLabel(), lritCfgPanel);
			edlConfigPanel = new EdlConfigPanel(this);
			tabbedPane.addTab(edlConfigPanel.getLabel(), edlConfigPanel);
			
			tabbedPane.addTab(labels.getString("LrgsConfigDialog.miscTab"),
					null, getMiscConfigTab(),
				labels.getString("LrgsConfigDialog.miscPars"));
			
		}
		return tabbedPane;
	}

	/**
	 * This method initializes buttonPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getButtonPanel() {
		if (buttonPanel == null) {
			FlowLayout flowLayout = new FlowLayout();
			flowLayout.setHgap(20);
			flowLayout.setVgap(10);
			buttonPanel = new JPanel();
			buttonPanel.setLayout(flowLayout);
			buttonPanel.setSize(new Dimension(238, 75));
			buttonPanel.add(getOkButton(), null);
			buttonPanel.add(getCancelButton(), null);
			buttonPanel.add(getApplyButton(), null);
		}
		return buttonPanel;
	}

	/**
	 * This method initializes okButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getOkButton() 
	{
		if (okButton == null) 
		{
			okButton = new JButton();
			okButton.setText(genericLabels.getString("OK"));
			okButton.addActionListener(
				new java.awt.event.ActionListener() 
			{
				public void actionPerformed(java.awt.event.ActionEvent e) 
				{
					okPressed();
				}
			});
		}
		return okButton;
	}

	/**
	 * This method initializes cancelButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getCancelButton() 
	{
		if (cancelButton == null) 
		{
			cancelButton = new JButton();
			cancelButton.setText(genericLabels.getString("cancel"));
			cancelButton.addActionListener(
				new java.awt.event.ActionListener() 
			{
				public void actionPerformed(java.awt.event.ActionEvent e) 
				{
					cancelPressed();
				}
			});
		}
		return cancelButton;
	}

	/**
	 * This method initializes applyButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getApplyButton() 
	{
		if (applyButton == null) 
		{
			applyButton = new JButton();
			applyButton.setText(labels.getString("LrgsConfigDialog.apply"));
			applyButton.addActionListener(
				new java.awt.event.ActionListener() 
			{
				public void actionPerformed(java.awt.event.ActionEvent e) 
				{
					applyPressed(true);
				}
			});
		}
		return applyButton;
	}

	/**
	 * This method initializes archiveConfigTab	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getArchiveConfigTab() {
		if (archiveConfigTab == null) {
			archiveConfigTab = new JPanel();
			archiveConfigTab.setLayout(new BoxLayout(getArchiveConfigTab(), BoxLayout.Y_AXIS));
			archiveConfigTab.add(getLrgsArchiveConfigPanel(), null);
			archiveConfigTab.add(getContainerPanel(),null);
		}
		return archiveConfigTab;
	}

	/**
	 * This method initializes domsatConfigTab	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getDomsatConfigTab() 
	{
		if (domsatConfigTab == null) 
		{
			domsatHardwareLabel = new JLabel();
			domsatHardwareLabel.setText(labels.getString(
					"LrgsConfigDialog.DHardwareInterface"));

			domsatTimeoutLabel = new JLabel();
			domsatTimeoutLabel.setText(labels.getString(
					"LrgsConfigDialog.DTimeout"));

			domsatConfigTab = new JPanel();
			domsatConfigTab.setLayout(new GridBagLayout());
			domsatConfigTab.setBorder(BorderFactory.createTitledBorder(null, 
				labels.getString("LrgsConfigDialog.DOMSATConfigurationTitle"),
				TitledBorder.CENTER, 
				TitledBorder.BELOW_TOP, new Font("Dialog", Font.BOLD, 14), 
				new Color(51, 51, 51)));

			
			domsatConfigTab.add(getInitializeDOMSATcheck(), 
				new GridBagConstraints(0, 0, 2, 1, 0.0, 0.5, 
					GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE,
					new Insets(10, 10, 5, 10), 0, 0));
			domsatConfigTab.add(domsatHardwareLabel, 
				new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, 
					GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(5, 40, 5, 2), 0, 0));
			domsatConfigTab.add(getDomsatHardwareCombo(), 
				new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, 
					GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
					new Insets(5, 0, 5, 20), 0, 0));

			JLabel dpcHostLabel = new JLabel(
				labels.getString("LrgsConfigDialog.DOMSATDpcHost"));
			domsatConfigTab.add(dpcHostLabel,
				new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, 
					GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(5, 40, 5, 2), 0, 0));
			domsatConfigTab.add(domsatDpcHost, 
				new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0, 
					GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
					new Insets(5, 0, 5, 20), 0, 0));

			JLabel dpcPortLabel = new JLabel(
				labels.getString("LrgsConfigDialog.DOMSATDpcPort"));
			domsatConfigTab.add(dpcPortLabel,
				new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, 
					GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(5, 40, 5, 2), 0, 0));
			domsatConfigTab.add(domsatDpcPort, 
				new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0, 
					GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
					new Insets(5, 0, 5, 20), 0, 0));

			domsatConfigTab.add(getDomsatLinkCheck(), 
				new GridBagConstraints(0, 4, 2, 1, 0.0, 0.0, 
					GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(10, 10, 5, 10), 0, 0));
			domsatConfigTab.add(domsatTimeoutLabel, 
				new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0, 
					GridBagConstraints.NORTHEAST, GridBagConstraints.NONE,
					new Insets(5, 40, 10, 2), 0, 0));
			domsatConfigTab.add(getDomsatTimeoutField(), 
				new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0, 
					GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
					new Insets(5, 0, 10, 20), 50, 0));
			
			acceptDomsatARMsCheck.setText(
				labels.getString("LrgsConfigDialog.acceptDomsatARMs"));

			domsatConfigTab.add(acceptDomsatARMsCheck, 
				new GridBagConstraints(0, 6, 2, 1, 0.0, 0.5, 
					GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
					new Insets(10, 10, 5, 10), 0, 0));
			
		}
		return domsatConfigTab;
	}

	/**
	 * This method initializes ddsServerConfigTab	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getDdsServerConfigTab() 
	{
		if (ddsServerConfigTab == null) 
		{
			ddsServerConfigTab = new JPanel();
			ddsServerConfigTab.setLayout(new GridBagLayout());
			ddsServerConfigTab.setBorder(BorderFactory.createTitledBorder(null, 
					labels.getString("LrgsConfigDialog.ddsLRGSDDSTitle"), 
					TitledBorder.CENTER, TitledBorder.BELOW_TOP, new Font("Dialog", Font.BOLD, 14), new Color(51, 51, 51)));

			ddsServerConfigTab.add(new JLabel(labels.getString("LrgsConfigDialog.ddsListeningPort")), 
				new GridBagConstraints(0, 0, 1, 1, 0, .5,
					GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, 
					new Insets(6, 0, 6, 2), 0, 0));
			ddsServerConfigTab.add(getDdsListenPortField(), 
				new GridBagConstraints(1, 0, 1, 1, .25, 0.,
					GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL,
					new Insets(6, 0, 6, 0), 0, 0));
			
			ddsServerConfigTab.add(new JLabel(labels.getString("LrgsConfigDialog.ddsBindIPAddress")), 
				new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
					GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(6, 0, 6, 2), 0, 0));
			ddsServerConfigTab.add(getDdsBindAddrField(), 
				new GridBagConstraints(1, 1, 1, 1, 0.5, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
					new Insets(6, 0, 6, 0), 80, 0));
			ddsServerConfigTab.add(new JLabel(labels.getString("LrgsConfigDialog.ddsMultiNICSystems")), 
				new GridBagConstraints(2, 1, 1, 1, 0.5, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(6, 0, 6, 10), 0, 0));
			
			ddsServerConfigTab.add(new JLabel(labels.getString("LrgsConfigDialog.ddsMaxClients")),
				new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
					GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(6, 10, 6, 2), 0, 0));
			ddsServerConfigTab.add(getDdsMaxClientsField(),
				new GridBagConstraints(1, 2, 1, 1, 0.5, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
					new Insets(6, 0, 6, 0), 0, 0));
	
			ddsServerConfigTab.add(new JLabel(labels.getString("LrgsConfigDialog.ddsSandboxDir")), 
				new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
					GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(6, 10, 6, 2), 0, 0));
			ddsServerConfigTab.add(getDdsParentDirectoryField(), 
				new GridBagConstraints(1, 3, 2, 1, 1.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
					new Insets(6, 0, 6, 40), 0, 0));
			
			ddsServerConfigTab.add(new JLabel(labels.getString("LrgsConfigDialog.ddsUsageLogFile")), 
				new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, 
					GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(6, 10, 6, 2), 0, 0));
			ddsServerConfigTab.add(getDdsLogFileField(), 
				new GridBagConstraints(1, 4, 2, 1, 1.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
					new Insets(6, 0, 6, 40), 0, 0));

			ddsServerConfigTab.add(getDdsRequireAuthCheck(), 
				new GridBagConstraints(1, 5, 2, 1, 0.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(6, 0, 6, 0), 0, 0));

			localAdminOnlyCheck.setText("Local Administrators Only");
			localAdminOnlyCheck.setToolTipText(
				"Do not allow administration from remotely shared accounts.");
			ddsServerConfigTab.add(localAdminOnlyCheck,
				new GridBagConstraints(1, 6, 2, 1, 0.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(6, 0, 6, 5), 0, 0));

			requireStrongAuthCheck.setText("Require SHA-256 Authentication");
			ddsServerConfigTab.add(requireStrongAuthCheck,
				new GridBagConstraints(1, 7, 2, 1, 0.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(6, 0, 6, 5), 0, 0));
			
			JLabel lb = new JLabel("Local Sandbox Directory:");
			ddsServerConfigTab.add(lb,
				new GridBagConstraints(0, 8, 1, 1, 0.0, 0.5,
					GridBagConstraints.NORTHEAST, GridBagConstraints.NONE,
					new Insets(6, 10, 6, 2), 0, 0));
			ddsServerConfigTab.add(localSandboxDir,
				new GridBagConstraints(1, 8, 1, 1, 1.0, 0.5,
					GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
					new Insets(5, 0, 5, 40), 0, 0));
		}
		return ddsServerConfigTab;
	}

	/**
	 * This method initializes ddsRecvConfigTab	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getDdsRecvConfigTab() {
		if (ddsRecvConfigTab == null) {
			ddsRecvConfigTab = new JPanel();
			ddsRecvConfigTab.setLayout(new BoxLayout(getDdsRecvConfigTab(), BoxLayout.Y_AXIS));
			ddsRecvConfigTab.add(getRecoverPanel(), null);
			ddsRecvConfigTab.add(getConnectionsPanel(),null);
			ddsRecvConfigTab.add(getNetworkListsPanel(),null);
		}
		return ddsRecvConfigTab;
	}

	/**
	 * This method initializes drgsConfigTab	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getDrgsConfigTab() {
		if (drgsConfigTab == null) {
			drgsConfigTab = new JPanel();
			drgsConfigTab.setLayout(
				new BoxLayout(drgsConfigTab, BoxLayout.Y_AXIS));
			drgsConfigTab.add(getJPanel(), null);
		}
		return drgsConfigTab;
	}

	private JPanel getMiscConfigTab()
	{
		BorderLayout bl = new BorderLayout();
		miscConfigTab = new JPanel(bl);
		JLabel lab = new JLabel(labels.getString(
				"LrgsConfigDialog.miscPars"));
		miscConfigTab.add(lab, BorderLayout.NORTH);
		miscPanel = new PropertiesEditPanel(new Properties());
		miscConfigTab.add(miscPanel, BorderLayout.CENTER);
		return miscConfigTab;
	}
	
	private JPanel getNetworkDcpTab()
	{
		networkDcpTab = new JPanel(new BorderLayout());
		networkDcpCfgPanel = new NetworkDcpCfgPanel(this);
		networkDcpTab.add(networkDcpCfgPanel, BorderLayout.CENTER);
		return networkDcpTab;
	}
	
	/**
	 * This method initializes lrgsArchiveConfigPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getLrgsArchiveConfigPanel() {
		if (lrgsArchiveConfigPanel == null) {
			GridBagConstraints archiveDirectoryLabelConstraints = new GridBagConstraints();
			archiveDirectoryLabelConstraints.gridx = 0;
			archiveDirectoryLabelConstraints.gridy = 0;
			archiveDirectoryLabelConstraints.anchor = GridBagConstraints.SOUTHEAST;
			archiveDirectoryLabelConstraints.weightx = 1.0;
			archiveDirectoryLabelConstraints.weighty = 0.5;
			archiveDirectoryLabelConstraints.insets = new Insets(2, 0, 2, 2);
			archiveDirectoryLabelConstraints.fill = GridBagConstraints.NONE;
			archiveDirectoryLabel = new JLabel();
			archiveDirectoryLabel.setText(labels.getString(
					"LrgsConfigDialog.archiveDirectory"));
			//archiveDirectoryLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
			//archiveDirectoryLabel.setHorizontalAlignment(SwingConstants.RIGHT);

			GridBagConstraints archiveDirFieldConstraints = new GridBagConstraints();
			archiveDirFieldConstraints.gridx = 1;
			archiveDirFieldConstraints.gridy = 0;
			archiveDirFieldConstraints.fill = GridBagConstraints.HORIZONTAL;
			archiveDirFieldConstraints.weightx = 1.0;
			archiveDirFieldConstraints.weighty = 0.5;
			archiveDirFieldConstraints.gridwidth = 3;
			archiveDirFieldConstraints.anchor = GridBagConstraints.SOUTHWEST;
			archiveDirFieldConstraints.insets = new Insets(2, 0, 2, 40);

			GridBagConstraints emptyLabel2Constraints = new GridBagConstraints();
			emptyLabel2Constraints.gridx = 2;
			emptyLabel2Constraints.fill = GridBagConstraints.BOTH;
			emptyLabel2Constraints.weightx = 2.0D;
			emptyLabel2Constraints.gridy = 2;
			emptyLabel2 = new JLabel();
			emptyLabel2.setText("");

			GridBagConstraints emptyLabel1Constraints = new GridBagConstraints();
			emptyLabel1Constraints.gridx = 3;
			emptyLabel1Constraints.weightx = 2.0D;
			emptyLabel1Constraints.fill = GridBagConstraints.BOTH;
			emptyLabel1Constraints.gridwidth = 1;
			emptyLabel1Constraints.gridy = 1;
			emptyLabel1 = new JLabel();
			emptyLabel1.setText("");

			GridBagConstraints statusPeriodFieldConstraints = new GridBagConstraints();
			statusPeriodFieldConstraints.fill = GridBagConstraints.BOTH;
			statusPeriodFieldConstraints.gridy = 4;
			statusPeriodFieldConstraints.weightx = 1.0;
			statusPeriodFieldConstraints.anchor = GridBagConstraints.WEST;
			statusPeriodFieldConstraints.insets = new Insets(2, 0, 2, 0);
			statusPeriodFieldConstraints.gridx = 1;

			GridBagConstraints localStatusFileFieldConstraints = new GridBagConstraints();
			localStatusFileFieldConstraints.fill = GridBagConstraints.HORIZONTAL;
			localStatusFileFieldConstraints.gridy = 3;
			localStatusFileFieldConstraints.weightx = 2.0D;
			localStatusFileFieldConstraints.insets = new Insets(2, 0, 2, 40);
			localStatusFileFieldConstraints.gridwidth = 3;
			localStatusFileFieldConstraints.gridx = 1;

			GridBagConstraints archiveTimeoutFieldConstraints = new GridBagConstraints();
			archiveTimeoutFieldConstraints.fill = GridBagConstraints.BOTH;
			archiveTimeoutFieldConstraints.gridy = 2;
			archiveTimeoutFieldConstraints.weightx = 1.0;
			archiveTimeoutFieldConstraints.anchor = GridBagConstraints.WEST;
			archiveTimeoutFieldConstraints.insets = new Insets(2, 0, 2, 0);
			archiveTimeoutFieldConstraints.gridx = 1;

			GridBagConstraints archiveLengthFieldConstraints = new GridBagConstraints();
			archiveLengthFieldConstraints.fill = GridBagConstraints.BOTH;
			archiveLengthFieldConstraints.gridy = 1;
			archiveLengthFieldConstraints.weightx = 1.0;
			archiveLengthFieldConstraints.anchor = GridBagConstraints.WEST;
			archiveLengthFieldConstraints.insets = new Insets(2, 0, 2, 0);
			archiveLengthFieldConstraints.gridx = 1;


			GridBagConstraints statusPeriodLabelConstraints = new GridBagConstraints();
			statusPeriodLabelConstraints.gridx = 0;
			statusPeriodLabelConstraints.anchor = GridBagConstraints.EAST;
			statusPeriodLabelConstraints.weightx = 1.0D;
			statusPeriodLabelConstraints.insets = new Insets(2, 0, 3, 2);
			statusPeriodLabelConstraints.gridy = 4;
			statusPeriodLabel = new JLabel();
			statusPeriodLabel.setText(labels.getString(
					"LrgsConfigDialog.statusPeriod"));

			GridBagConstraints localStatusFileLabelConstraints = new GridBagConstraints();
			localStatusFileLabelConstraints.gridx = 0;
			localStatusFileLabelConstraints.anchor = GridBagConstraints.EAST;
			localStatusFileLabelConstraints.weightx = 1.0D;
			localStatusFileLabelConstraints.insets = new Insets(2, 0, 3, 2);
			localStatusFileLabelConstraints.gridy = 3;
			localStatusFileLabel = new JLabel();
			localStatusFileLabel.setText(labels.getString(
					"LrgsConfigDialog.localStatusFile"));

			GridBagConstraints archiveTimeoutLabelConstraints = new GridBagConstraints();
			archiveTimeoutLabelConstraints.gridx = 0;
			archiveTimeoutLabelConstraints.anchor = GridBagConstraints.EAST;
			archiveTimeoutLabelConstraints.weightx = 1.0D;
			archiveTimeoutLabelConstraints.insets = new Insets(2, 40, 3, 2);
			archiveTimeoutLabelConstraints.gridy = 2;
			archiveTimeoutLabel = new JLabel();
			archiveTimeoutLabel.setText(labels.getString(
					"LrgsConfigDialog.archiveTimeout"));

			GridBagConstraints archiveLengthLabelConstraints = new GridBagConstraints();
			archiveLengthLabelConstraints.gridx = 0;
			archiveLengthLabelConstraints.anchor = GridBagConstraints.EAST;
			archiveLengthLabelConstraints.weightx = 1.0D;
			archiveLengthLabelConstraints.insets = new Insets(2, 0, 3, 2);
			archiveLengthLabelConstraints.gridy = 1;
			archiveLengthLabel = new JLabel();
			archiveLengthLabel.setText(labels.getString(
					"LrgsConfigDialog.archiveLength"));
			archiveLengthLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
			archiveLengthLabel.setHorizontalAlignment(SwingConstants.RIGHT);

			GridBagConstraints sharedNetlistDirectoryLabelConstraints = new GridBagConstraints();
			sharedNetlistDirectoryLabelConstraints.gridx = 0;
			sharedNetlistDirectoryLabelConstraints.anchor = GridBagConstraints.EAST;
			sharedNetlistDirectoryLabelConstraints.weightx = 1.0D;
			sharedNetlistDirectoryLabelConstraints.weighty = 0.5;
			sharedNetlistDirectoryLabelConstraints.insets = new Insets(2, 0, 2, 2);
			sharedNetlistDirectoryLabelConstraints.gridy = 5;
			sharedNetlistDirectoryLabel = new JLabel();
			sharedNetlistDirectoryLabel.setText(labels.getString(
					"LrgsConfigDialog.sharedNetlistDir"));

			GridBagConstraints sharedNetlistDirectoryFieldConstraints = new GridBagConstraints();
			sharedNetlistDirectoryFieldConstraints.fill = GridBagConstraints.HORIZONTAL;
			sharedNetlistDirectoryFieldConstraints.anchor = GridBagConstraints.WEST;
			sharedNetlistDirectoryFieldConstraints.gridy = 5;
			sharedNetlistDirectoryFieldConstraints.weightx = 2.0D;
			sharedNetlistDirectoryFieldConstraints.weighty = 0.5;
			sharedNetlistDirectoryFieldConstraints.insets = new Insets(2, 0, 2, 40);
			sharedNetlistDirectoryFieldConstraints.gridwidth = 3;
			sharedNetlistDirectoryFieldConstraints.gridx = 1;

			lrgsArchiveConfigPanel = new JPanel();
			lrgsArchiveConfigPanel.setLayout(new GridBagLayout());
			lrgsArchiveConfigPanel.setBorder(BorderFactory.createTitledBorder(null,
					labels.getString("LrgsConfigDialog.archiveConfigTitle"), 
					TitledBorder.CENTER, TitledBorder.BELOW_TOP, new Font("Dialog", Font.BOLD, 14), new Color(51, 51, 51)));
			lrgsArchiveConfigPanel.add(archiveDirectoryLabel, archiveDirectoryLabelConstraints);
			lrgsArchiveConfigPanel.add(archiveLengthLabel, archiveLengthLabelConstraints);
			lrgsArchiveConfigPanel.add(archiveTimeoutLabel, archiveTimeoutLabelConstraints);
			lrgsArchiveConfigPanel.add(localStatusFileLabel, localStatusFileLabelConstraints);
			lrgsArchiveConfigPanel.add(statusPeriodLabel, statusPeriodLabelConstraints);
			lrgsArchiveConfigPanel.add(sharedNetlistDirectoryLabel, sharedNetlistDirectoryLabelConstraints);
			lrgsArchiveConfigPanel.add(getArchiveDirField(), archiveDirFieldConstraints);
			lrgsArchiveConfigPanel.add(getArchiveLengthField(), archiveLengthFieldConstraints);
			lrgsArchiveConfigPanel.add(getArchiveTimeoutField(), archiveTimeoutFieldConstraints);
			lrgsArchiveConfigPanel.add(getLocalStatusFileField(), localStatusFileFieldConstraints);
			lrgsArchiveConfigPanel.add(getStatusPeriodField(), statusPeriodFieldConstraints);
			lrgsArchiveConfigPanel.add(getSharedNetlistDirectoryField(), sharedNetlistDirectoryFieldConstraints);
			lrgsArchiveConfigPanel.add(emptyLabel1, emptyLabel1Constraints);
			lrgsArchiveConfigPanel.add(emptyLabel2, emptyLabel2Constraints);
			
			lrgsArchiveConfigPanel.add(pdtValidationCheck,
				new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(2, 0, 2, 15), 0, 0));

			lrgsArchiveConfigPanel.add(
				new JLabel(labels.getString("LrgsConfigDialog.PDTURL")),
				new GridBagConstraints(0, 7, 1, 1, 0.0, 0.0, 
					GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(2, 10, 2, 2), 0, 0));  

			lrgsArchiveConfigPanel.add(pdtUrlField,
				new GridBagConstraints(1, 7, 1, 1, 1.0, 0.0, 
					GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
					new Insets(2, 0, 2, 20), 0, 0));
			
			lrgsArchiveConfigPanel.add(
				new JLabel(labels.getString("LrgsConfigDialog.CDTURL")),
				new GridBagConstraints(0, 8, 1, 1, 0.0, 0.5, 
					GridBagConstraints.NORTHEAST, GridBagConstraints.NONE,
					new Insets(2, 10, 2, 2), 0, 0));  

			lrgsArchiveConfigPanel.add(cdtUrlField,
				new GridBagConstraints(1, 8, 1, 1, 1.0, 0.5, 
					GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
					new Insets(2, 0, 2, 20), 0, 0));  
		}
		return lrgsArchiveConfigPanel;
	}

	/**
	 * This method initializes dataSourcePrefPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getDataSourcePrefPanel() {
		if (dataSourcePrefPanel == null) {
			GridBagConstraints pref4ComboConstraints = new GridBagConstraints();
			pref4ComboConstraints.fill = GridBagConstraints.HORIZONTAL;
			pref4ComboConstraints.gridy = 4;
			pref4ComboConstraints.weightx = 1.0;
			pref4ComboConstraints.anchor = GridBagConstraints.WEST;
			pref4ComboConstraints.insets = new Insets(2, 0, 2, 10);
			pref4ComboConstraints.gridx = 1;
			pref4ComboConstraints.ipadx = 50;
			GridBagConstraints pref3ComboConstraints = new GridBagConstraints();
			pref3ComboConstraints.fill = GridBagConstraints.HORIZONTAL;
			pref3ComboConstraints.gridy = 3;
			pref3ComboConstraints.weightx = 1.0;
			pref3ComboConstraints.anchor = GridBagConstraints.WEST;
			pref3ComboConstraints.insets = new Insets(2, 0, 2, 10);
			pref3ComboConstraints.gridx = 1;
			pref3ComboConstraints.ipadx = 50;
			GridBagConstraints pref2ComboConstraints = new GridBagConstraints();
			pref2ComboConstraints.fill = GridBagConstraints.HORIZONTAL;
			pref2ComboConstraints.gridy = 2;
			pref2ComboConstraints.weightx = 1.0;
			pref2ComboConstraints.anchor = GridBagConstraints.WEST;
			pref2ComboConstraints.insets = new Insets(2, 0, 2, 10);
			pref2ComboConstraints.gridx = 1;
			pref2ComboConstraints.ipadx = 50;
			GridBagConstraints preference4LabelConstraints = new GridBagConstraints();
			preference4LabelConstraints.gridx = 0;
			preference4LabelConstraints.weightx = 0.5D;
			preference4LabelConstraints.anchor = GridBagConstraints.EAST;
			preference4LabelConstraints.insets = new Insets(0, 10, 0, 2);
			preference4LabelConstraints.gridy = 4;
			preference4Label = new JLabel();
			preference4Label.setText(labels.getString(
					"LrgsConfigDialog.preference4"));
			GridBagConstraints preference3LabelConstraints = new GridBagConstraints();
			preference3LabelConstraints.gridx = 0;
			preference3LabelConstraints.weightx = 0.5D;
			preference3LabelConstraints.anchor = GridBagConstraints.EAST;
			preference3LabelConstraints.insets = new Insets(0, 10, 0, 2);
			preference3LabelConstraints.gridy = 3;
			preference3Label = new JLabel();
			preference3Label.setText(labels.getString(
				"LrgsConfigDialog.preference3"));
			GridBagConstraints preference2LabelConstraints = new GridBagConstraints();
			preference2LabelConstraints.gridx = 0;
			preference2LabelConstraints.weightx = 0.5D;
			preference2LabelConstraints.anchor = GridBagConstraints.EAST;
			preference2LabelConstraints.insets = new Insets(0, 10, 0, 2);
			preference2LabelConstraints.gridy = 2;
			preference2Label = new JLabel();
			preference2Label.setText(labels.getString(
						"LrgsConfigDialog.preference2"));
			dataSourcePrefPanel = new JPanel();
			dataSourcePrefPanel.setLayout(new GridBagLayout());
			dataSourcePrefPanel.setBorder(
					BorderFactory.createTitledBorder(
						BorderFactory.createBevelBorder(BevelBorder.LOWERED),
						labels.getString("LrgsConfigDialog.dataSourcePref"),
						TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font("Dialog", Font.BOLD, 14), new Color(51, 51, 51)));

			preferredGoodCheck.setText(
				labels.getString("LrgsConfigDialog.preferGood"));
			dataSourcePrefPanel.add(preferredGoodCheck,
				new GridBagConstraints(0, 0, 2, 1, 0.5, 0.0,
					GridBagConstraints.SOUTH, GridBagConstraints.NONE,
					new Insets(5, 10, 10, 5), 0, 0));
			
			dataSourcePrefPanel.add(
				new JLabel(labels.getString("LrgsConfigDialog.preference1")),
				new GridBagConstraints(0, 1, 1, 1, 0.5, 0.0,
					GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(0, 10, 0, 2), 0, 0));
				
			dataSourcePrefPanel.add(getMergePref1Combo(),
				new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
					new Insets(2, 0, 2, 10), 50, 0));
		
			dataSourcePrefPanel.add(preference2Label, preference2LabelConstraints);
			dataSourcePrefPanel.add(preference3Label, preference3LabelConstraints);
			dataSourcePrefPanel.add(preference4Label, preference4LabelConstraints);
			dataSourcePrefPanel.add(getMergePref2Combo(), pref2ComboConstraints);
			dataSourcePrefPanel.add(getMergePref3Combo(), pref3ComboConstraints);
			dataSourcePrefPanel.add(getMergePref4Combo(), pref4ComboConstraints);
		}
		return dataSourcePrefPanel;
	}

	/**
	 * This method initializes archiveDirField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getArchiveDirField() {
		if (archiveDirField == null) {
			archiveDirField = new JTextField();
			//archiveDirField.setPreferredSize(new Dimension(500, 20));
		}
		return archiveDirField;
	}

	/**
	 * This method initializes archiveLengthField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getArchiveLengthField() {
		if (archiveLengthField == null) {
			archiveLengthField = new JTextField();
			//archiveLengthField.setPreferredSize(new Dimension(4, 20));
		}
		return archiveLengthField;
	}

	/**
	 * This method initializes archiveTimeoutField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getArchiveTimeoutField() {
		if (archiveTimeoutField == null) {
			archiveTimeoutField = new JTextField();
		}
		return archiveTimeoutField;
	}

	/**
	 * This method initializes localStatusFileField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getLocalStatusFileField() {
		if (localStatusFileField == null) {
			localStatusFileField = new JTextField();
		}
		return localStatusFileField;
	}

	/**
	 * This method initializes statusPeriodField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getStatusPeriodField() {
		if (statusPeriodField == null) {
			statusPeriodField = new JTextField();
		}
		return statusPeriodField;
	}

	/**
	 * This method initializes sharedNetlistDirectoryField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getSharedNetlistDirectoryField() {
		if (sharedNetlistDirectoryField == null) {
			sharedNetlistDirectoryField = new JTextField();
		}
		return sharedNetlistDirectoryField;
	}

	/**
	 * This method initializes mergePref1Combo	
	 * 	
	 * @return javax.swing.JComboBox	
	 */
	private JComboBox getMergePref1Combo() {
		if (mergePref1Combo == null) {
			mergePref1Combo = new JComboBox(mergePrefs);
		}
		return mergePref1Combo;
	}

	/**
	 * This method initializes mergePref2Combo	
	 * 	
	 * @return javax.swing.JComboBox	
	 */
	private JComboBox getMergePref2Combo() {
		if (mergePref2Combo == null) {
			mergePref2Combo = new JComboBox(mergePrefs);
		}
		return mergePref2Combo;
	}

	/**
	 * This method initializes mergePref3Combo	
	 * 	
	 * @return javax.swing.JComboBox	
	 */
	private JComboBox getMergePref3Combo() {
		if (mergePref3Combo == null) {
			mergePref3Combo = new JComboBox(mergePrefs);
		}
		return mergePref3Combo;
	}

	/**
	 * This method initializes mergePref4Combo	
	 * 	
	 * @return javax.swing.JComboBox	
	 */
	private JComboBox getMergePref4Combo() {
		if (mergePref4Combo == null) {
			mergePref4Combo = new JComboBox();
			mergePref4Combo.addItem("(unspecified)");
			mergePref4Combo.addItem("DRGS");
			mergePref4Combo.addItem("DOMSAT");
			mergePref4Combo.addItem("DDS Receive");
		}
		return mergePref4Combo;
	}

	/**
	 * This method initializes initializeDOMSATcheck	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getInitializeDOMSATcheck() {
		if (initializeDOMSATcheck == null) {
			initializeDOMSATcheck = new JCheckBox();
			initializeDOMSATcheck.setText(labels.getString(
					"LrgsConfigDialog.initDOMSATCheckBox"));
		}
		return initializeDOMSATcheck;
	}

	/**
	 * This method initializes domsatLinkCheck	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getDomsatLinkCheck() {
		if (domsatLinkCheck == null) {
			domsatLinkCheck = new JCheckBox();
			domsatLinkCheck.setText(labels.getString(
					"LrgsConfigDialog.enableDOMSATLink"));
		}
		return domsatLinkCheck;
	}

	/**
	 * This method initializes domsatHardwareCombo	
	 * 	
	 * @return javax.swing.JComboBox	
	 */
	private JComboBox getDomsatHardwareCombo()
	{
		if (domsatHardwareCombo == null)
		{
			domsatHardwareCombo = new JComboBox();
			domsatHardwareCombo.addItem("lrgs.domsatrecv.DomsatSangoma");
			domsatHardwareCombo.addItem("lrgs.domsatrecv.DomsatDpc");
			domsatHardwareCombo.addItem("lrgs.domsatrecv.DomsatFranklin");
			
			domsatHardwareCombo.addActionListener(
				new java.awt.event.ActionListener() 
				{
					public void actionPerformed(java.awt.event.ActionEvent e) 
					{
						chkDpcEnabled();
					}
				});

		}
		return domsatHardwareCombo;
	}
	
	private void chkDpcEnabled()
	{
		String cn = (String)domsatHardwareCombo.getSelectedItem();
		boolean isDpc = 
			cn != null && cn.equals("lrgs.domsatrecv.DomsatDpc");
		domsatDpcPort.setEnabled(isDpc);
		domsatDpcHost.setEnabled(isDpc);
	}

	/**
	 * This method initializes domsatTimeoutField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getDomsatTimeoutField() {
		if (domsatTimeoutField == null) {
			domsatTimeoutField = new JTextField();
		}
		return domsatTimeoutField;
	}

	/**
	 * This method initializes ddsListenPortField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getDdsListenPortField() {
		if (ddsListenPortField == null) {
			ddsListenPortField = new JTextField();
		}
		return ddsListenPortField;
	}

	/**
	 * This method initializes ddsBindAddrField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getDdsBindAddrField() {
		if (ddsBindAddrField == null) {
			ddsBindAddrField = new JTextField();
		}
		return ddsBindAddrField;
	}

	/**
	 * This method initializes ddsMaxClientsField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getDdsMaxClientsField() {
		if (ddsMaxClientsField == null) {
			ddsMaxClientsField = new JTextField();
		}
		return ddsMaxClientsField;
	}

	/**
	 * This method initializes ddsParentDirectoryField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getDdsParentDirectoryField() {
		if (ddsParentDirectoryField == null) {
			ddsParentDirectoryField = new JTextField();
		}
		return ddsParentDirectoryField;
	}

	/**
	 * This method initializes ddsLogFileField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getDdsLogFileField() {
		if (ddsLogFileField == null) {
			ddsLogFileField = new JTextField();
		}
		return ddsLogFileField;
	}

	/**
	 * This method initializes ddsRequireAuthCheck	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getDdsRequireAuthCheck() {
		if (ddsRequireAuthCheck == null) {
			ddsRequireAuthCheck = new JCheckBox();
			ddsRequireAuthCheck.setText(labels.getString(
					"LrgsConfigDialog.reqClientPasswdAuth"));
		}
		return ddsRequireAuthCheck;
	}

	/**
	 * This method initializes recoverPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getRecoverPanel() 
	{
		if (recoverPanel == null) 
		{
			recoverPanel = new JPanel(new GridBagLayout());
			recoverPanel.setBorder(BorderFactory.createTitledBorder(null, 
				labels.getString(
					"LrgsConfigDialog.LRGSDDSBackupTitle"), TitledBorder.CENTER, TitledBorder.BELOW_TOP, new Font("Dialog", Font.BOLD, 14), new Color(51, 51, 51)));
			recoverPanel.add(new JLabel(genericLabels.getString("timeout")),
				new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
					GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(10, 10, 4, 2), 0, 0));
			recoverPanel.add(ddsTimeoutField,
				new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(10, 0, 4, 10), 0, 0));
			recoverPanel.add(new JLabel(labels.getString("minHourly")),
				new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
					GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(4, 10, 4, 2), 0, 0));
			recoverPanel.add(ddsMinHourlyField,
				new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(4, 0, 4, 10), 0, 0));
			
			recoverPanel.add(getEnableDDSReceiveCheck(), 
				new GridBagConstraints(0, 3, 2, 1, 0.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(0, 40, 0, 0), 0, 0));
		}
		return recoverPanel;
	}

//	/**
//	 * This method initializes recoveryCombo	
//	 * 	
//	 * @return javax.swing.JComboBox	
//	 */
//	private JComboBox getRecoveryCombo() {
//		if (recoveryCombo == null) {
//			recoveryCombo = new JComboBox();
//			recoveryCombo.addItem("Real - Time Stream");
//			recoveryCombo.addItem("Recover from Outages");
//		}
//		return recoveryCombo;
//	}

	/**
	 * This method initializes enableDDSReceiveCheck	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getEnableDDSReceiveCheck() {
		if (enableDDSReceiveCheck == null) {
			enableDDSReceiveCheck = new JCheckBox();
			enableDDSReceiveCheck.setText(labels.getString(
					"LrgsConfigDialog.enableDDSReceivConn"));
		}
		return enableDDSReceiveCheck;
	}

	/**
	 * This method initializes connectionsPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getConnectionsPanel() {
		if (connectionsPanel == null) {
			connectionsPanel = new JPanel();
			connectionsPanel.setLayout(new BorderLayout());
			connectionsPanel.setSize(new Dimension(150, 129));
			connectionsPanel.add(getConnectionsButtonPanel(), BorderLayout.EAST);
			connectionsPanel.add(getDdsReceiveScrollPane(), BorderLayout.CENTER);
		}
		return connectionsPanel;
	}

	/**
	 * This method initializes connectionsButtonPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getConnectionsButtonPanel() {
		if (connectionsButtonPanel == null) {
			GridBagConstraints ddsConMoveDownButtonConstraints = new GridBagConstraints();
			ddsConMoveDownButtonConstraints.gridx = 0;
			ddsConMoveDownButtonConstraints.insets = new Insets(2, 5, 2, 5);
			ddsConMoveDownButtonConstraints.weighty = 0.0D;
			//ddsConMoveDownButtonConstraints.anchor = GridBagConstraints.NORTH;
			ddsConMoveDownButtonConstraints.gridy = 4;
			GridBagConstraints ddsConMoveUpButtonConstraints = new GridBagConstraints();
			ddsConMoveUpButtonConstraints.gridx = 0;
			ddsConMoveUpButtonConstraints.insets = new Insets(2, 5, 2, 5);
			ddsConMoveUpButtonConstraints.gridy = 3;
			GridBagConstraints ddsConDeleteButtonConstraints = new GridBagConstraints();
			ddsConDeleteButtonConstraints.gridx = 0;
			ddsConDeleteButtonConstraints.insets = new Insets(2, 5, 2, 5);
			ddsConDeleteButtonConstraints.gridy = 2;
			GridBagConstraints ddsConEditButtonConstraints = new GridBagConstraints();
			ddsConEditButtonConstraints.gridx = 0;
			ddsConEditButtonConstraints.insets = new Insets(2, 5, 2, 5);
			ddsConEditButtonConstraints.gridy = 1;
			GridBagConstraints addButtonConstraints = new GridBagConstraints();
			addButtonConstraints.gridx = 0;
			addButtonConstraints.gridwidth = 1;
			addButtonConstraints.insets = new Insets(20, 5, 2, 5);
			addButtonConstraints.weighty = 0.0D;
			addButtonConstraints.gridy = 0;
			
			GridBagConstraints testButtonConstraints = new GridBagConstraints();
			testButtonConstraints.gridx = 0;
			testButtonConstraints.gridwidth = 1;
			testButtonConstraints.insets = new Insets(2, 5, 2, 5);
			testButtonConstraints.weighty = 1.0D;
			testButtonConstraints.anchor = GridBagConstraints.NORTH;
			testButtonConstraints.gridy = 5;

			connectionsButtonPanel = new JPanel();
			connectionsButtonPanel.setLayout(new GridBagLayout());
			connectionsButtonPanel.add(getConnectionsButtonAdd(), addButtonConstraints);
			connectionsButtonPanel.add(getConnectionsButtonEdit(), ddsConEditButtonConstraints);
			connectionsButtonPanel.add(getConnectionsButtonDelete(), ddsConDeleteButtonConstraints);
			connectionsButtonPanel.add(getConnectionsButtonMoveUp(), ddsConMoveUpButtonConstraints);
			connectionsButtonPanel.add(getConnectionsButtonMoveDown(), ddsConMoveDownButtonConstraints);
			connectionsButtonPanel.add(getConnectionsButtonTest(),testButtonConstraints);
		}
		return connectionsButtonPanel;
	}

	private JButton getConnectionsButtonTest() {
		if (ddsConTestButton == null) {
			ddsConTestButton = new JButton();
			ddsConTestButton.setText(genericLabels.getString("test"));
			ddsConTestButton.setMnemonic(KeyEvent.VK_UNDEFINED);
			ddsConTestButton.addActionListener(
				new java.awt.event.ActionListener() 
			{
				public void actionPerformed(java.awt.event.ActionEvent e) 
				{
					testButtonPressed();
				}
			});
		}
		return ddsConTestButton;
	}
	private void testButtonPressed()
	{
		int idx = ddsConTable.getSelectedRow();
		if (idx == -1)
		{
			showError(labels.getString(
					"LrgsConfigDialog.selectConnEditErr"));
			return;
		}
		DdsRecvConnectCfg cfg = (DdsRecvConnectCfg)ddsTableModel.getRowObject(idx);
		
		LddsClient myClient = new LddsClient(cfg.host,cfg.port);
		
		//TODO add option for password sending
		LrgsConnectionTest myTester = new LrgsConnectionTest(this, myClient, cfg.username,null);
		myTester.startConnect();
		
	}
	
	/**
	 * This method initializes ddsConAddButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getConnectionsButtonAdd() {
		if (ddsConAddButton == null) {
			ddsConAddButton = new JButton();
			ddsConAddButton.setText(genericLabels.getString("add"));
			ddsConAddButton.setMnemonic(KeyEvent.VK_UNDEFINED);
			ddsConAddButton.addActionListener(
				new java.awt.event.ActionListener() 
			{
				public void actionPerformed(java.awt.event.ActionEvent e) 
				{
					launchDdsRecvConDialog(false);
				}
			});
		}
		return ddsConAddButton;
	}

	private void launchDdsRecvConDialog(boolean edit)
	{
		DdsRecvConDialog dlg = new DdsRecvConDialog(this);
		DdsRecvConnectCfg cfg = null;
		if (edit)
		{
			int idx = ddsConTable.getSelectedRow();
			if (idx == -1)
			{
				showError(labels.getString(
						"LrgsConfigDialog.selectConnEditErr"));
				return;
			}
			cfg = (DdsRecvConnectCfg)ddsTableModel.getRowObject(idx);
		}
		else // add
		{
			cfg = new DdsRecvConnectCfg(ddsTableModel.getRowCount(), "");
		}
		dlg.setInfo(cfg);
		launchDialog(dlg);
		if (dlg.okPressed())
		{
			if (!edit)
				ddsTableModel.add(cfg);
			else
				ddsTableModel.modified();
		}
	}

	private void deleteDdsCon()
	{
		int idx = ddsConTable.getSelectedRow();
		if (idx == -1)
		{
			showError(labels.getString(
					"LrgsConfigDialog.selectDDSConnDelErr"));
			return;
		}
		DdsRecvConnectCfg cfg = 
			(DdsRecvConnectCfg)ddsTableModel.getRowObject(idx);
		if( JOptionPane.showConfirmDialog(this,
				LoadResourceBundle.sprintf(labels.getString(
				"LrgsConfigDialog.DDSConnDel"),cfg.name),
			labels.getString(
			"LrgsConfigDialog.confirmDelete"), JOptionPane.YES_NO_OPTION)
			== JOptionPane.YES_OPTION)
		{
			ddsTableModel.deleteAt(idx);
		}
	}

	private void moveDdsConUp()
	{
		int idx = ddsConTable.getSelectedRow();
		if (idx == -1)
		{
			showError(labels.getString(
					"LrgsConfigDialog.selectDDSConnMoveErr"));
			return;
		}
		if (ddsTableModel.moveUpAt(idx))
			ddsConTable.setRowSelectionInterval(idx-1, idx-1);
	}

	private void moveDdsConDown()
	{
		int idx = ddsConTable.getSelectedRow();
		if (idx == -1)
		{
			showError(labels.getString(
			"LrgsConfigDialog.selectDDSConnMoveErr"));
			return;
		}
		if (ddsTableModel.moveDownAt(idx))
			ddsConTable.setRowSelectionInterval(idx+1, idx+1);
	}

	/**
	 * This method initializes ddsConEditButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getConnectionsButtonEdit() {
		if (ddsConEditButton == null) {
			ddsConEditButton = new JButton();
			ddsConEditButton.setText(genericLabels.getString("edit"));
//			ddsConEditButton.setPreferredSize(new Dimension(110, 26));
			ddsConEditButton.addActionListener(
				new java.awt.event.ActionListener() 
				{
					public void actionPerformed(java.awt.event.ActionEvent e) 
					{
						launchDdsRecvConDialog(true);
					}
				});
		}
		return ddsConEditButton;
	}

	/**
	 * This method initializes ddsConDeleteButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getConnectionsButtonDelete() {
		if (ddsConDeleteButton == null) {
			ddsConDeleteButton = new JButton();
			ddsConDeleteButton.setText(genericLabels.getString("delete"));
//			ddsConDeleteButton.setPreferredSize(new Dimension(110, 26));
			ddsConDeleteButton.addActionListener(
				new java.awt.event.ActionListener() 
				{
					public void actionPerformed(java.awt.event.ActionEvent e) 
					{
						deleteDdsCon();
					}
				});
		}
		return ddsConDeleteButton;
	}

	/**
	 * This method initializes ddsConMoveUpButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getConnectionsButtonMoveUp() 
	{
		if (ddsConMoveUpButton == null) {
			ddsConMoveUpButton = new JButton();
			ddsConMoveUpButton.setText(labels.getString(
					"LrgsConfigDialog.moveUp"));
//			ddsConMoveUpButton.setPreferredSize(new Dimension(110, 26));
			ddsConMoveUpButton.addActionListener(
				new java.awt.event.ActionListener() 
				{
					public void actionPerformed(java.awt.event.ActionEvent e) 
					{
						moveDdsConUp();
					}
				});
		}
		return ddsConMoveUpButton;
	}

	/**
	 * This method initializes ddsConMoveDownButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getConnectionsButtonMoveDown() {
		if (ddsConMoveDownButton == null) {
			ddsConMoveDownButton = new JButton();
			ddsConMoveDownButton.setText(labels.getString(
					"LrgsConfigDialog.moveDn"));
//			ddsConMoveDownButton.setPreferredSize(new Dimension(110, 26));
			ddsConMoveDownButton.addActionListener(
				new java.awt.event.ActionListener() 
				{
					public void actionPerformed(java.awt.event.ActionEvent e) 
					{
						moveDdsConDown();
					}
				});
		}
		return ddsConMoveDownButton;
	}

	/**
	 * This method initializes networkListsPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getNetworkListsPanel() {
		if (networkListsPanel == null) {
			networkListsPanel = new JPanel();
			networkListsPanel.setLayout(new BorderLayout());
			networkListsPanel.setSize(new Dimension(176, 177));
			networkListsPanel.setBorder(BorderFactory.createTitledBorder(null, 
					labels.getString("LrgsConfigDialog.DDSNetListTextArea"),
					TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font("Dialog", Font.BOLD, 12), new Color(51, 51, 51)));
			networkListsPanel.add(getNetworkListsButtonPanel(), BorderLayout.EAST);
			networkListsPanel.add(getNetworkListsScrollPane(), BorderLayout.CENTER);
		}
		return networkListsPanel;
	}

	/**
	 * This method initializes networkListsButtonPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getNetworkListsButtonPanel() {
		if (networkListsButtonPanel == null) {
			GridBagConstraints networkListsDeleteButtonConstraints = new GridBagConstraints();
			networkListsDeleteButtonConstraints.gridx = 0;
			networkListsDeleteButtonConstraints.insets = new Insets(2, 5, 2, 5);
			networkListsDeleteButtonConstraints.weighty = 1.0D;
			networkListsDeleteButtonConstraints.anchor = GridBagConstraints.NORTH;
			networkListsDeleteButtonConstraints.gridy = 1;
			GridBagConstraints networkListsAddButtonConstraints = new GridBagConstraints();
			networkListsAddButtonConstraints.gridx = 0;
			networkListsAddButtonConstraints.insets = new Insets(2, 5, 2, 5);
			networkListsAddButtonConstraints.gridy = 0;
			networkListsButtonPanel = new JPanel();
			networkListsButtonPanel.setLayout(new GridBagLayout());
			networkListsButtonPanel.add(getNetworkListsAddButton(), networkListsAddButtonConstraints);
			networkListsButtonPanel.add(getNetworkListsDeleteButton(), networkListsDeleteButtonConstraints);
		}
		return networkListsButtonPanel;
	}

	/**
	 * This method initializes networkListsAddButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getNetworkListsAddButton() {
		if (networkListsAddButton == null) {
			networkListsAddButton = new JButton();
			networkListsAddButton.setText(genericLabels.getString("add"));
//			networkListsAddButton.setPreferredSize(new Dimension(110, 26));
			networkListsAddButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					
					//Now metwork list can be associated with thr groups, primary, secondary or both
					JPanel panel = new JPanel(new GridLayout(4,1));
					panel.add(new JLabel(labels.getString("LrgsConfigDialog.enterNLToAdd")));
					JTextField netlist = new JTextField();
					panel.add(netlist);
					Object[] items = {"Primary", "Secondary","Both"};
				
					JComboBox jcb = new JComboBox(items);

					jcb.setEditable(false);
					panel.add(new JLabel("Group"));
					panel.add(jcb);

					Integer res = (Integer)JOptionPane.showOptionDialog(new JFrame(), panel, "Network List",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE,null,null,null);				
					if(res.intValue()==0 && netlist.getText()!=null && netlist.getText().trim().length()>0 )
					{
						Object[] rowData = {netlist.getText(), jcb.getSelectedItem()};
						netlistTableModel.addRow(rowData);						
						netlistsModified = true;
					}
					/*
					JOptionPane mypane = new JOptionPane();
					netlistListModel.addElement(mypane.showInputDialog(
					labels.getString("LrgsConfigDialog.enterNLToAdd")));
					netlistsModified = true;*/
				}
			});
		}
		return networkListsAddButton;
	}

	/**
	 * This method initializes networkListsDeleteButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getNetworkListsDeleteButton() {
		if (networkListsDeleteButton == null) {
			networkListsDeleteButton = new JButton();
			networkListsDeleteButton.setText(
					genericLabels.getString("delete"));
//			networkListsDeleteButton.setPreferredSize(new Dimension(110, 26));
			networkListsDeleteButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) 
				{
					//removeNetworkListAt(getNetworkList().getSelectedIndex());
					removeNetworkListAt(getNetworkListTable().getSelectedRow());
					netlistsModified = true;
				}
			});
		}
		return networkListsDeleteButton;
	}

	/**
	 * This method initializes jPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel() {
		if (jPanel == null) {
			jPanel = new JPanel();
			jPanel.setLayout(new BoxLayout(getJPanel(), BoxLayout.Y_AXIS));
			jPanel.add(getDrgsConfigPanel(), null);
		}
		return jPanel;
	}

	/**
	 * This method initializes drgsConfigPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getDrgsConfigPanel() 
	{
		if (drgsConfigPanel == null) {
			drgsConfigPanel = new JPanel();
			drgsConfigPanel.setLayout(new BorderLayout());
			drgsConfigPanel.setBorder(
				BorderFactory.createTitledBorder(null, 
				labels.getString("LrgsConfigDialog.DRGSConfTitle"),
				TitledBorder.CENTER, TitledBorder.BELOW_TOP, 
				new Font("Dialog", Font.BOLD, 14), new Color(51, 51, 51)));
			drgsConfigPanel.add(getDrgsTablePanel(), BorderLayout.CENTER);
		}
		return drgsConfigPanel;
	}
	
	/**
	 * This method initializes drgsButtonPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getDrgsButtonPanel() {
		if (drgsButtonPanel == null) {

			GridBagConstraints addButtonConstraints = 
				new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
					new Insets(10, 4, 2, 4), 0, 0);

			GridBagConstraints drgsEditButtonConstraints = 
				new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
					new Insets(2, 4, 2, 4), 0, 0);

			GridBagConstraints drgsDeleteButtonConstraints = 
				new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
					new Insets(2, 4, 2, 4), 0, 0);
			GridBagConstraints drgsTestButtonConstraints = 
				new GridBagConstraints(0, 3, 1, 1, 0.0, 1.0,
					GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
					new Insets(2, 4, 2, 4), 0, 0);
			

			drgsButtonPanel = new JPanel();
			drgsButtonPanel.setLayout(new GridBagLayout());
			drgsButtonPanel.add(getDrgsAddButton(), addButtonConstraints);
			drgsButtonPanel.add(getDrgsEditButton(), drgsEditButtonConstraints);
			drgsButtonPanel.add(getDrgsDeleteButton(), drgsDeleteButtonConstraints);
			drgsButtonPanel.add(getDrgsTestButton(), drgsTestButtonConstraints);
		}
		return drgsButtonPanel;
	}

	private JButton getDrgsTestButton() {
		if (drgsTestButton == null) {
			drgsTestButton = new JButton();
			drgsTestButton.setText(genericLabels.getString("test"));
			drgsTestButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) 
				{
					drgsTestButtonPressed();
				}
			});
		}
		return drgsTestButton;
	}
	
	private void drgsTestButtonPressed()
	{
		int idx = drgsConTable.getSelectedRow();
		if (idx == -1)
		{
			showError(labels.getString(
					"LrgsConfigDialog.selectConnEditErr"));
			return;
		}
		DrgsConnectCfg cfg = (DrgsConnectCfg)drgsTableModel.getRowObject(idx);
		
		BasicClient myClient = new BasicClient(cfg.host,cfg.msgPort);
		
		//TODO add option for password sending
		LrgsConnectionTest myTester = new LrgsConnectionTest(this, myClient);
		myTester.startConnect();
	}
	
	/**
	 * This method initializes drgsAddButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getDrgsAddButton() {
		if (drgsAddButton == null) {
			drgsAddButton = new JButton();
			drgsAddButton.setText(genericLabels.getString("add"));
			drgsAddButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) 
				{
					launchDrgsConDialog(false);
				}
			});
		}
		return drgsAddButton;
	}

	/**
	 * This method initializes drgsEditButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getDrgsEditButton() {
		if (drgsEditButton == null) {
			drgsEditButton = new JButton();
			drgsEditButton.setText(genericLabels.getString("edit"));
//			drgsEditButton.setPreferredSize(new Dimension(82, 26));
			drgsEditButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) 
				{
					launchDrgsConDialog(true);
				}
			});
		}
		return drgsEditButton;
	}

	private void launchDrgsConDialog(boolean edit)
	{
		DrgsConDialog dlg = new DrgsConDialog(this);
		DrgsConnectCfg cfg = null;
		if (edit)
		{
			int idx = drgsConTable.getSelectedRow();
			if (idx == -1)
			{
				showError(labels.getString(
						"LrgsConfigDialog.selectConnEditErr"));
				return;
			}
			cfg = (DrgsConnectCfg)drgsTableModel.getRowObject(idx);
			dlg.setInfo(cfg);
		}
		else // add
		{
			cfg = new DrgsConnectCfg(drgsConTable.getRowCount(), "");
			dlg.setInfo(cfg);
		}
		launchDialog(dlg);
		if (dlg.okPressed())
		{
			if (!edit)
				drgsTableModel.add(cfg);
			else
				drgsTableModel.modified();
		}
	}

	private void deleteDrgsCon()
	{
		int idx = drgsConTable.getSelectedRow();
		if (idx == -1)
		{
			showError(labels.getString(
					"LrgsConfigDialog.selectDRGSConnDelErr"));
			return;
		}
		DrgsConnectCfg cfg = 
			(DrgsConnectCfg)drgsTableModel.getRowObject(idx);
		if( JOptionPane.showConfirmDialog(this,
				LoadResourceBundle.sprintf(
				labels.getString("LrgsConfigDialog.DRGSConnDel"),
				cfg.name),
				labels.getString("LrgsConfigDialog.confirmDelete"),
					JOptionPane.YES_NO_OPTION)
			== JOptionPane.YES_OPTION)
		{
			drgsTableModel.deleteAt(idx);
		}
	}



	/**
	 * This method initializes drgsDeleteButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getDrgsDeleteButton() {
		if (drgsDeleteButton == null) {
			drgsDeleteButton = new JButton();
			drgsDeleteButton.setText(genericLabels.getString("delete"));
//			drgsDeleteButton.setPreferredSize(new Dimension(82, 26));
			drgsDeleteButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) 
				{
					deleteDrgsCon();
				}
			});
		}
		return drgsDeleteButton;
	}

	/**
	 * This method initializes networkListsScrollPane	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getNetworkListsScrollPane() {
		if (networkListsScrollPane == null) {
			networkListsScrollPane = new JScrollPane();
			//networkListsScrollPane.setViewportView(getNetworkList());
			networkListsScrollPane.setViewportView(getNetworkListTable());
		}
		return networkListsScrollPane;
	}

	/**
	 * This method initializes networkList	
	 * 	
	 * @return javax.swing.JList	
	 */
	private JList getNetworkList() {
		if (networkList == null) {
			networkList = new JList(netlistListModel);
		}
		return networkList;
	}

	
	/**
	 * This method initializes networkList	
	 * 	
	 * @return javax.swing.JList	
	 */
	private JTable getNetworkListTable() {
	
		
		
		
		
		if (networkListTable == null) {
			networkListTable = new JTable(netlistTableModel);
			TableColumn tblCol = new TableColumn();
			netlistTableModel.addColumn("Network List Name");
			netlistTableModel.addColumn("Group");
			JTableHeader header =networkListTable.getTableHeader();
			header.setBorder( BorderFactory.createRaisedBevelBorder());
			header.setEnabled(false);
			header.setBackground(Color.white);
			header.setFocusable(false);
			networkListTable.setTableHeader(header);
			
			networkListTable.setVisible(true);
			
		}
		return networkListTable;
	}

	
	/**
	 * This method initializes drgsTablePanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getDrgsTablePanel() 
	{
		if (drgsTablePanel == null) 
		{
			drgsTablePanel = new JPanel(new GridBagLayout());
			
			drgsTablePanel.add(new JLabel(labels.getString("minHourly")),
				new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 
					GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(10, 10, 2, 0), 0, 0));
			drgsTablePanel.add(drgsMinHourlyField,
				new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, 
					GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(10, 1, 2, 10), 0, 0));

			drgsTablePanel.add(getEnableDRGSCheck(), 
				new GridBagConstraints(0, 1, 2, 1, 0.0, 0.0, 
					GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(4, 10, 2, 10), 0, 0));
				
			drgsTablePanel.add(getConnectionsScrollPane(), 
				new GridBagConstraints(0, 2, 2, 1, 1.0, 1.0, 
					GridBagConstraints.WEST, GridBagConstraints.BOTH,
					new Insets(5, 5, 5, 5), 0, 0));  

				
			drgsTablePanel.add(getDrgsButtonPanel(), 
				new GridBagConstraints(2, 2, 1, 1, 0.0, 1.0, 
					GridBagConstraints.NORTH, GridBagConstraints.NONE,
					new Insets(5, 5, 5, 5), 0, 0));
		}
		return drgsTablePanel;
	}

	/**
	 * This method initializes enableDRGSCheck	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getEnableDRGSCheck() {
		if (enableDRGSCheck == null) {
			enableDRGSCheck = new JCheckBox();
			enableDRGSCheck.setText(labels.getString(
					"LrgsConfigDialog.enableDRGSConn"));
		}
		return enableDRGSCheck;
	}

	
	
	/**
	 * This method initializes drgsConTable	
	 * 	
	 * @return javax.swing.JTable	
	 */
	private JTable getConnectionsTable() {
		if (drgsConTable == null) {
			drgsTableModel = new DrgsSortingListTableModel();
			drgsConTable = new SortingListTable(drgsTableModel,drgsTableModel.columnWidths);

			
		}
		return drgsConTable;
	}

	
	
	
	/**
	 * This method initializes ddsConTable	
	 * 	
	 * @return javax.swing.JTable	
	 */
	private JTable getDdsReceiveTable() {
		if (ddsConTable == null) {
			ddsTableModel = new DdsSortingListTableModel();
			ddsConTable = new SortingListTable(ddsTableModel,ddsTableModel.columnWidths);
			
		}
		return ddsConTable;
	}

	/**
	 * This method initializes SqlDatabasePanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getSqlDatabasePanel() {
		if (SqlDatabasePanel == null) {

			GridBagConstraints sqlUrlFieldConstraints = new GridBagConstraints();
			sqlUrlFieldConstraints.fill = GridBagConstraints.HORIZONTAL;
			sqlUrlFieldConstraints.anchor = GridBagConstraints.SOUTHWEST;
			sqlUrlFieldConstraints.gridx = 1;
			sqlUrlFieldConstraints.gridy = 0;
			sqlUrlFieldConstraints.weightx = 1.0;
			sqlUrlFieldConstraints.weighty = 0.5;
			sqlUrlFieldConstraints.insets = new Insets(2, 0, 2, 10);
			sqlUrlFieldConstraints.gridwidth = 2;

			GridBagConstraints sqlKeyFieldConstraints = new GridBagConstraints();
			sqlKeyFieldConstraints.fill = GridBagConstraints.HORIZONTAL;
			sqlKeyFieldConstraints.anchor = GridBagConstraints.WEST;
			sqlKeyFieldConstraints.gridy = 5;
			sqlKeyFieldConstraints.weightx = 1.0;
			sqlKeyFieldConstraints.weighty = 0.5;
			sqlKeyFieldConstraints.insets = new Insets(2, 0, 2, 10);
			sqlKeyFieldConstraints.gridx = 2;

			GridBagConstraints sqlDriverFieldConstraints = new GridBagConstraints();
			sqlDriverFieldConstraints.fill = GridBagConstraints.HORIZONTAL;
			sqlDriverFieldConstraints.gridy = 4;
			sqlDriverFieldConstraints.weightx = 1.0;
			sqlDriverFieldConstraints.insets = new Insets(2, 0, 2, 10);
			sqlDriverFieldConstraints.gridx = 2;

			GridBagConstraints sqlTimeZoneComboConstraints = new GridBagConstraints();
			sqlTimeZoneComboConstraints.fill = GridBagConstraints.HORIZONTAL;
			sqlTimeZoneComboConstraints.gridy = 3;
			sqlTimeZoneComboConstraints.weightx = 1.0;
			sqlTimeZoneComboConstraints.insets = new Insets(2, 0, 2, 10);
			sqlTimeZoneComboConstraints.gridx = 2;

			GridBagConstraints sqlWriteFieldConstraints = new GridBagConstraints();
			sqlWriteFieldConstraints.fill = GridBagConstraints.HORIZONTAL;
			sqlWriteFieldConstraints.gridy = 2;
			sqlWriteFieldConstraints.weightx = 1.0;
			sqlWriteFieldConstraints.insets = new Insets(2, 0, 2, 10);
			sqlWriteFieldConstraints.gridx = 2;

			GridBagConstraints sqlReadFieldConstraints = new GridBagConstraints();
			sqlReadFieldConstraints.fill = GridBagConstraints.HORIZONTAL;
			sqlReadFieldConstraints.gridy = 1;
			sqlReadFieldConstraints.weightx = 1.0;
			sqlReadFieldConstraints.insets = new Insets(2, 0, 2, 10);
			sqlReadFieldConstraints.gridx = 2;

			GridBagConstraints sqlKeyLabelConstraints = new GridBagConstraints();
			sqlKeyLabelConstraints.gridx = 0;
			sqlKeyLabelConstraints.gridwidth = 2;
			sqlKeyLabelConstraints.anchor = GridBagConstraints.EAST;
			sqlKeyLabelConstraints.weighty = 0.5;
			sqlKeyLabelConstraints.insets = new Insets(2, 2, 3, 2);
			sqlKeyLabelConstraints.gridy = 5;
			sqlKeyLabel = new JLabel();
			sqlKeyLabel.setText(labels.getString(
					"LrgsConfigDialog.keyGeneratorClass"));

			GridBagConstraints sqlTimeZoneLabelConstraints = new GridBagConstraints();
			sqlTimeZoneLabelConstraints.gridx = 0;
			sqlTimeZoneLabelConstraints.anchor = GridBagConstraints.EAST;
			sqlTimeZoneLabelConstraints.gridwidth = 2;
			sqlTimeZoneLabelConstraints.insets = new Insets(2, 2, 3, 2);
			sqlTimeZoneLabelConstraints.gridy = 3;
			sqlTimeZoneLabel = new JLabel();
			sqlTimeZoneLabel.setText(labels.getString(
					"LrgsConfigDialog.SQLTimeZone"));

			GridBagConstraints sqlDriverLabelConstraints = new GridBagConstraints();
			sqlDriverLabelConstraints.gridx = 0;
			sqlDriverLabelConstraints.anchor = GridBagConstraints.EAST;
			sqlDriverLabelConstraints.gridwidth = 2;
			sqlDriverLabelConstraints.insets = new Insets(2, 2, 3, 2);
			sqlDriverLabelConstraints.gridy = 4;
			sqlDriverLabel = new JLabel();
			sqlDriverLabel.setText(labels.getString(
					"LrgsConfigDialog.JDBCDriverClass"));


			GridBagConstraints sqlWriteLabelConstraints = new GridBagConstraints();
			sqlWriteLabelConstraints.gridx = 0;
			sqlWriteLabelConstraints.anchor = GridBagConstraints.EAST;
			sqlWriteLabelConstraints.gridwidth = 2;
			sqlWriteLabelConstraints.insets = new Insets(2, 2, 3, 2);
			sqlWriteLabelConstraints.gridy = 2;
			sqlWriteLabel = new JLabel();
			sqlWriteLabel.setText(labels.getString(
					"LrgsConfigDialog.writeTimeFormat"));
			GridBagConstraints sqlReadLabelConstraints = new GridBagConstraints();
			sqlReadLabelConstraints.gridx = 0;
			sqlReadLabelConstraints.insets = new Insets(2, 40, 3, 2);
			sqlReadLabelConstraints.anchor = GridBagConstraints.EAST;
			sqlReadLabelConstraints.gridwidth = 2;
			sqlReadLabelConstraints.gridy = 1;
			sqlReadLabel = new JLabel();
			sqlReadLabel.setText(labels.getString(
					"LrgsConfigDialog.readTimeFormat"));

			GridBagConstraints sqlUrlLabelConstraints = new GridBagConstraints();
			sqlUrlLabelConstraints.gridx = 0;
			sqlUrlLabelConstraints.anchor = GridBagConstraints.SOUTHEAST;
			sqlUrlLabelConstraints.insets = new Insets(2, 40, 3, 2);
			sqlUrlLabelConstraints.gridy = 0;
			sqlUrlLabelConstraints.weighty = 0.5;
			sqlUrlLabel = new JLabel();
			sqlUrlLabel.setText(labels.getString(
					"LrgsConfigDialog.URL"));

			SqlDatabasePanel = new JPanel();
			SqlDatabasePanel.setLayout(new GridBagLayout());
			SqlDatabasePanel.setBorder(BorderFactory.createTitledBorder(
					BorderFactory.createBevelBorder(BevelBorder.LOWERED),
					labels.getString("LrgsConfigDialog.SQLDBPref"), 
					TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font("Dialog", Font.BOLD, 14), new Color(51, 51, 51)));
			SqlDatabasePanel.add(sqlUrlLabel, sqlUrlLabelConstraints);
			SqlDatabasePanel.add(sqlReadLabel, sqlReadLabelConstraints);
			SqlDatabasePanel.add(sqlWriteLabel, sqlWriteLabelConstraints);
			SqlDatabasePanel.add(getSqlUrlField(), sqlUrlFieldConstraints);
			SqlDatabasePanel.add(getSqlReadField(), sqlReadFieldConstraints);
			SqlDatabasePanel.add(getSqlWriteField(), sqlWriteFieldConstraints);
			SqlDatabasePanel.add(sqlTimeZoneLabel, sqlTimeZoneLabelConstraints);
			SqlDatabasePanel.add(getSqlTimeZoneCombo(), sqlTimeZoneComboConstraints);
			SqlDatabasePanel.add(sqlDriverLabel, sqlDriverLabelConstraints);
			SqlDatabasePanel.add(sqlKeyLabel, sqlKeyLabelConstraints);
			SqlDatabasePanel.add(getSqlDriverField(), sqlDriverFieldConstraints);
			SqlDatabasePanel.add(getSqlKeyField(), sqlKeyFieldConstraints);
			
			SqlDatabasePanel.add(goesXmitCheck,
				new GridBagConstraints(0, 6, 2, 1, 0.0, 0.5,
					GridBagConstraints.CENTER, GridBagConstraints.NORTH,
					new Insets(5, 20, 5, 2), 0, 0));
		}
		return SqlDatabasePanel;
	}







	/**
	 * This method initializes containerPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getContainerPanel() {
		if (containerPanel == null) {
			containerPanel = new JPanel();
			containerPanel.setLayout(new BoxLayout(getContainerPanel(), BoxLayout.X_AXIS));
			containerPanel.setSize(new Dimension(230, 145));
			containerPanel.add(getDataSourcePrefPanel(), null);
			containerPanel.add(getSqlDatabasePanel(),null);
		}
		return containerPanel;
	}







	/**
	 * This method initializes sqlUrlField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getSqlUrlField() {
		if (sqlUrlField == null) {
			sqlUrlField = new JTextField();
		}
		return sqlUrlField;
	}







	/**
	 * This method initializes sqlReadField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getSqlReadField() {
		if (sqlReadField == null) {
			sqlReadField = new JTextField();
		}
		return sqlReadField;
	}







	/**
	 * This method initializes sqlWriteField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getSqlWriteField() {
		if (sqlWriteField == null) {
			sqlWriteField = new JTextField();
		}
		return sqlWriteField;
	}







	/**
	 * This method initializes sqlTimeZoneCombo	
	 * 	
	 * @return javax.swing.JComboBox	
	 */
	private JComboBox getSqlTimeZoneCombo() 
	{
		if (sqlTimeZoneCombo == null) 
		{
			sqlTimeZoneCombo = new TimeZoneSelector();
			sqlTimeZoneCombo.setTZ("UTC");
		}
		return sqlTimeZoneCombo;
	}

	/**
	 * This method initializes sqlDriverField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getSqlDriverField() {
		if (sqlDriverField == null) {
			sqlDriverField = new JTextField();
		}
		return sqlDriverField;
	}

	/**
	 * This method initializes sqlKeyField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getSqlKeyField() {
		if (sqlKeyField == null) {
			sqlKeyField = new JTextField();
		}
		return sqlKeyField;
	}

	/**
	 * This method initializes connectionsScrollPane	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getConnectionsScrollPane() {
		if (connectionsScrollPane == null) {
			connectionsScrollPane = new JScrollPane();
			connectionsScrollPane.setViewportView(getConnectionsTable());
		}
		return connectionsScrollPane;
	}

	/**
	 * This method initializes ddsReceiveScrollPane	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getDdsReceiveScrollPane() {
		if (ddsReceiveScrollPane == null) {
			ddsReceiveScrollPane = new JScrollPane(getDdsReceiveTable());
			//ddsReceiveScrollPane.setViewportView(getDdsReceiveTable());
			
		}
		return ddsReceiveScrollPane;
	}

	private void removeNetworkListAt(int idx)
	{
		if (idx == -1)
			return;
		//netlistListModel.remove(idx);
		netlistTableModel.removeRow(idx);
	}

	private void okPressed()
	{
		if (applyPressed(false))
			setVisible(false);
	}

	private void cancelPressed()
	{
		setVisible(false);
	}

	private boolean applyPressed(boolean force)
	{
		try
		{
			boolean extraChanges = false;

			if (noaaConfigTab.hasChanged())
			{
				extraChanges = true;
				noaaConfigTab.saveChanges();
			}
			if (iridiumCfgTab.hasChanged())
			{
				extraChanges = true;
				iridiumCfgTab.saveChanges();
			}
			if (lritCfgPanel.hasChanged())
			{
				extraChanges = true;
				lritCfgPanel.saveChanges();
			}
			if (edlConfigPanel.hasChanged())
			{
				extraChanges = true;
				edlConfigPanel.saveChanges();
			}
			if (copyBackLrgsConfig() || force || extraChanges)
				ddsClientIf.applyLrgsConfig(lrgsConfig);
			
			if (copyBackDdsSettings() || force)
				ddsClientIf.applyDdsRecvSettings(ddsSettings);

			if (copyBackDrgsConfig() || force)
				ddsClientIf.applyDrgsInputSettings(drgsSettings);

			if (copyBackNetworkDcpConfig() || force)
				ddsClientIf.applyNetworkDcpSettings(networkDcpSettings);

			return true;
		}
		catch(ParseException ex)
		{
			showError(labels.getString(
					"LrgsConfigDialog.correctErrRetry"));
			return false;
		}
		catch(AuthException ex)
		{
			showError(labels.getString(
				"LrgsConfigDialog.applyChangesErr") + ex);
			return false;
		}
	}
}

class DdsSortingListTableModel 
	extends AbstractTableModel implements SortingListTableModel
{
	private static ResourceBundle labels = 
		RtStat.getLabels();
	String columnNames[] = null;
	int columnWidths[] = { 10, 20, 35, 15, 20, 10, 10, 10,10};
	ArrayList<DdsRecvConnectCfg> cons;
	boolean modified = false;

	public DdsSortingListTableModel()
	{
		columnNames = new String[9];
		columnNames[0] = labels.getString("LrgsConfigDialog.useColumn");
		columnNames[1] = labels.getString("LrgsConfigDialog.nameColumn");
		columnNames[2] = labels.getString("LrgsConfigDialog.hostIPAddrColumn");
		columnNames[3] = labels.getString("LrgsConfigDialog.portColumn");
		columnNames[4] = labels.getString("LrgsConfigDialog.userColumn");
		columnNames[5] = labels.getString("LrgsConfigDialog.pwColumn");
		columnNames[6] = labels.getString("LrgsConfigDialog.seqColumn");
		columnNames[7] = "ARMs?";
		columnNames[8] = labels.getString("LrgsConfigDialog.group");
		
		cons = new ArrayList<DdsRecvConnectCfg>();
	}

	public void clear() 
	{
		cons.clear();
	}

	public void add(DdsRecvConnectCfg cfg)
	{
		cons.add(cfg);
		modified();
	}

	public void modified()
	{
		super.fireTableDataChanged();
		modified = true;
	}

	public void deleteAt(int idx)
	{
		if (idx < 0 || idx >= cons.size())
			return;
		cons.remove(idx);
		for(int i=idx; i<cons.size(); i++)
			cons.get(i).connectNum = i;
		modified();
	}

	public boolean moveUpAt(int idx)
	{
		if (idx < 1 || idx >= cons.size())
			return false;
		DdsRecvConnectCfg con = cons.remove(idx);
		cons.add(idx-1, con);
		for(int i=0; i<cons.size(); i++)
			cons.get(i).connectNum = i;
		modified();
		return true;
	}

	public boolean moveDownAt(int idx)
	{
		if (idx < 0 || idx >= cons.size()-1)
			return false;
		DdsRecvConnectCfg con = cons.remove(idx);
		cons.add(idx+1, con);
		for(int i=0; i<cons.size(); i++)
			cons.get(i).connectNum = i;
		modified();
		return true;
	}

	public void setContents(DdsRecvSettings settings)
	{
		cons.clear();
		for(DdsRecvConnectCfg con : settings.connectCfgs)
			cons.add(new DdsRecvConnectCfg(con));
		Collections.sort(cons);
	}

	public int getRowCount()
	{
		return cons.size();
	}

	public Object getValueAt(int r, int c)
	{
		DdsRecvConnectCfg cfg = (DdsRecvConnectCfg)getRowObject(r);
		if (cfg == null)
			return "";

		switch(c)
		{
		case 0: return "" + cfg.enabled;
		case 1: return cfg.name == null ? "" : cfg.name;
		case 2: return cfg.host == null ? "" : cfg.host;
		case 3: return "" + cfg.port;
		case 4: return cfg.username == null ? "" : cfg.username;
		case 5: return "" + cfg.authenticate;
		case 6: return "" + cfg.hasDomsatSeqNums;
		case 7: return "" + cfg.acceptARMs;
		case 8: return "" + cfg.group;
		default: return "";
		}
	}

	public void sortByColumn(int c)
	{
		// NO SORTING, always sorted by connection number.
	}
	
	public Object getRowObject(int r)
	{
		if (r >=0 && r < cons.size())
			return cons.get(r);
		return null;
	}
	
	public String getColumnName(int col)
	{
		return columnNames[col];
	}
	

	public int getColumnCount() 
	{
		return columnNames.length;
	}
}

class DrgsSortingListTableModel 
	extends AbstractTableModel implements SortingListTableModel
{
	private static ResourceBundle labels = 
		RtStat.getLabels();
	
	String columnNames[] = null;
	int columnWidths[] = { 10, 20, 30, 10, 10, 20};
	ArrayList<DrgsConnectCfg> cons;
	boolean modified = false;
	
	public DrgsSortingListTableModel()
	{
		columnNames = new String[6];
		columnNames[0] = labels.getString("LrgsConfigDialog.useColumn");
		columnNames[1] = labels.getString("LrgsConfigDialog.nameColumn");
		columnNames[2] = labels.getString("LrgsConfigDialog.hostIPAddrColumn");
		columnNames[3] = labels.getString("LrgsConfigDialog.msgPortColumn");
		columnNames[4] = labels.getString("LrgsConfigDialog.evtsColumn");
		columnNames[5] = labels.getString("LrgsConfigDialog.chanCfgColumn");

		cons = new ArrayList<DrgsConnectCfg>();
	}

	public void setContents(DrgsInputSettings settings)
	{
		cons.clear();
		for(DrgsConnectCfg con : settings.connections)
			cons.add(new DrgsConnectCfg(con));
		Collections.sort(cons);
	}

	public void clear()
	{
		cons.clear();
	}

	public void add(DrgsConnectCfg cfg)
	{
		cons.add(cfg);
		modified();
	}

	public void modified()
	{
		super.fireTableDataChanged();
		modified = true;
	}

	public void deleteAt(int idx)
	{
		if (idx < 0 || idx >= cons.size())
			return;
		cons.remove(idx);
		for(int i=idx; i<cons.size(); i++)
			cons.get(i).connectNum = i;
		modified();
	}

	public int getRowCount()
	{
		return cons.size();
	}

	public void sortByColumn(int c)
	{
		// No Sorting!
	}
	
	public Object getRowObject(int r)
	{
		if (r >=0 && r < cons.size())
			return cons.get(r);
		return null;
	}
	
	public String getColumnName(int col)
	{
		return columnNames[col];
	}
	
	public Object getValueAt(int r, int c)
	{
		DrgsConnectCfg cfg = (DrgsConnectCfg)getRowObject(r);
		if (cfg == null)
			return "";

		switch(c)
		{
		case 0: return "" + cfg.msgEnabled;
		case 1: return cfg.name == null ? "" : cfg.name;
		case 2: return cfg.host == null ? "" : cfg.host;
		case 3: return "" + cfg.msgPort;
		case 4: return "" + cfg.evtEnabled;
		case 5: return cfg.cfgFile == null ? "" : cfg.cfgFile;
		default: return "";
		}
	}

	public int getColumnCount() {
		return columnNames.length;
	}
}
