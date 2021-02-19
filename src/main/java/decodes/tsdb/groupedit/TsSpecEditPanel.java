/**
 * $Id: TsSpecEditPanel.java,v 1.4 2020/03/10 18:58:00 mmaloney Exp $
 * 
 * $Log: TsSpecEditPanel.java,v $
 * Revision 1.4  2020/03/10 18:58:00  mmaloney
 * dev
 *
 * Revision 1.3  2020/03/10 16:30:02  mmaloney
 * Updates
 *
 * Revision 1.2  2020/02/27 22:10:12  mmaloney
 * Fixed imports
 *
 * Revision 1.1  2020/01/31 19:41:00  mmaloney
 * Implement new TS Definition Panel for OpenTSDB and CWMS.
 *
 * Revision 1.15  2019/12/11 14:43:38  mmaloney
 * Don't explicitly require CwmsTimeSeriesDb. This module is also used with OpenTsdb.
 *
 * Revision 1.14  2018/02/05 15:52:39  mmaloney
 * Added ObjectType filter for USBR HDB.
 *
 * Revision 1.13  2017/05/31 21:34:27  mmaloney
 * GUI improvements for HDB
 *
 * Revision 1.12  2017/05/08 12:34:53  mmaloney
 * For CWMS, TSIDs may contain param values that CCP does not have in its DataType
 * table. If this is the case, save as an other "param" value.
 *
 * Revision 1.11  2017/04/19 19:27:45  mmaloney
 * CWMS-10609 nested group evaluation in group editor bugfix.
 *
 * Revision 1.10  2017/01/10 21:11:35  mmaloney
 * Enhanced wildcard processing for CWMS as per punchlist for comp-depends project
 * for NWP.
 *
 * Revision 1.9  2016/11/21 16:04:03  mmaloney
 * Code Cleanup.
 *
 * Revision 1.8  2016/11/03 19:08:05  mmaloney
 * Implement new Location, Param, and Version dialogs for CWMS.
 *
 * Revision 1.7  2016/10/11 17:40:36  mmaloney
 * Final GUI Prototype
 *
 * Revision 1.6  2016/07/20 15:45:46  mmaloney
 * Special code for HDB to show data type common names.
 *
 * Revision 1.5  2016/04/22 14:42:53  mmaloney
 * Code cleanup.
 *
 * Revision 1.4  2015/10/22 14:04:55  mmaloney
 * Clean up debug: Old code was saying no match for site even when it did find match.
 *
 * Revision 1.3  2015/08/04 18:54:03  mmaloney
 * CWMS-6388 Display should show location ID, not public name. This fixes the issue
 * with evaluation.
 *
 * Revision 1.2  2014/09/25 18:10:40  mmaloney
 * Enum fields encapsulated.
 *
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * Revision 1.12  2013/03/21 18:27:40  mmaloney
 * DbKey Implementation
 *
 * Revision 1.11  2012/09/19 15:33:53  mmaloney
 * "StatCode" needs to be "StatisticsCode"
 *
 * Revision 1.10  2012/09/19 15:07:41  mmaloney
 * Fix bugs in detecting whether changes have occured.
 *
 * Revision 1.9  2012/08/01 16:55:58  mmaloney
 * dev
 *
 * Revision 1.8  2012/07/30 20:04:21  mmaloney
 * Don't ask 'are you sure' when removing time-series from list.
 *
 * Revision 1.7  2012/07/24 15:48:41  mmaloney
 * Cosmetic group-editor bugs for HDB.
 *
 * Revision 1.6  2012/07/24 15:15:47  mmaloney
 * Cosmetic group-editor bugs for HDB.
 *
 * Revision 1.5  2012/07/24 13:40:14  mmaloney
 * groupedit cosmetic bugs
 *
 * Revision 1.4  2012/07/18 20:41:44  mmaloney
 * Updated for USBR HDB.
 *
 * Revision 1.3  2011/10/19 14:27:04  gchen
 * Modify the addQueryParam() to use InputDialog (other than MessageDialog) for version to allow the cancel button displayed on the screen.
 *
 * Revision 1.2  2011/02/04 21:30:33  mmaloney
 * Intersect groups
 *
 * Revision 1.1  2011/02/03 20:00:23  mmaloney
 * Time Series Group Editor Mods
 *
 */
package decodes.tsdb.groupedit;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.TimeZone;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.TitledBorder;

import opendcs.dai.DataTypeDAI;
import opendcs.dai.IntervalDAI;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.opentsdb.Interval;
import opendcs.opentsdb.OffsetErrorAction;
import opendcs.opentsdb.OpenTimeSeriesDAO;
import ilex.util.AsciiUtil;
import ilex.util.EnvExpander;
import ilex.util.LoadResourceBundle;
import ilex.util.Logger;
import ilex.util.TextUtil;
import ilex.var.NoConversionException;
import ilex.var.TimedVariable;
import decodes.cwms.CwmsTsId;
import decodes.db.Constants;
import decodes.db.DataPresentation;
import decodes.db.DataType;
import decodes.db.EngineeringUnit;
import decodes.db.IntervalList;
import decodes.db.PresentationGroup;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.dbeditor.SiteSelectDialog;
import decodes.gui.EUSelectDialog;
import decodes.sql.DbKey;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.VarFlags;
import decodes.util.DecodesSettings;

/**
 * This class is the Ts Group tab of the Time Series GUI.
 * 
 */
@SuppressWarnings({ "rawtypes", "serial", "unchecked" })
public class TsSpecEditPanel
	extends JPanel
{
	private String module = "TsSpecEditPanel";
	
	private ResourceBundle genericResources = null;
	private ResourceBundle groupResources = null;
	
	private JTextField keyField = new JTextField(5);
	private JTextField tsidField = new JTextField(20);
	private JTextField lastModifiedField = new JTextField(20);
	private JTextArea descArea = new JTextArea(3, 0);
	private JTextField locationField = new JTextField(5);
	private JTextField paramField = new JTextField(5);
	private JComboBox statCodeCombo = new JComboBox();
	private JComboBox intervalCombo = new JComboBox();
	private JComboBox durationCombo = new JComboBox();
	private JComboBox versionCombo = new JComboBox();
	private JCheckBox activeCheck = new JCheckBox();
	private JTextField unitsField = new JTextField(5);
	private JCheckBox allowOffsetVarCheck = new JCheckBox();
	
	private JTextField utcOffsetField = new JTextField(8);
	
	private JComboBox onOffsetErrCombo = new JComboBox(OffsetErrorAction.values());
	
	private JComboBox numericStringCombo = new JComboBox(
		new String[] { "Numeric", "String" });
	private JTextField dataTableField = new JTextField(4);
	private JTextField numValuesField = new JTextField(8);
	private JTextField minField = new JTextField(16);
	private JTextField maxField = new JTextField(16);
	private JTextField oldestField = new JTextField(16);
	private JTextField newestField = new JTextField(16);
	
	private TsListFrame parent = null;
	private CwmsTsId tsid = null;
	private CTimeSeries tsidData = null;
	
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MMM/dd-HH:mm:ss");

	private ArrayList<String> statcodes = null;

	private String intervalCodes[];

	private String[] durationCodes;

	private ArrayList<String> versions;
	private JFileChooser fileChooser = null;
	private SimpleDateFormat exportSdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
	private NumberFormat exportNumFmt = NumberFormat.getNumberInstance();
	private Site selectedSite = null;
	
	public TsSpecEditPanel(TsListFrame parent)
	{	
		this.parent = parent;
		groupResources = LoadResourceBundle.getLabelDescriptions(
			"decodes/resources/groupedit", DecodesSettings.instance().language);
		genericResources = LoadResourceBundle.getLabelDescriptions(
			"decodes/resources/generic", DecodesSettings.instance().language);

		sdf.setTimeZone(TimeZone.getTimeZone(DecodesSettings.instance().guiTimeZone));
		exportSdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		fileChooser = new JFileChooser(EnvExpander.expand("$DCSTOOL_USERDIR"));
		exportNumFmt.setGroupingUsed(false);
		exportNumFmt.setMaximumFractionDigits(5);
		guiInit();
//		pack();
		
	}

	private void guiInit()
	{
		// Initialize this panel
		setLayout(new BorderLayout());
		
		// North Panel is Key, TSID, and Description
		JPanel northPanel = new JPanel(new GridBagLayout());
		this.add(northPanel, BorderLayout.NORTH);
		northPanel.add(new JLabel(genericResources.getString("key") + ":"),
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 10, 2, 0), 0, 0));
		keyField.setEditable(false);
		northPanel.add(keyField,
			new GridBagConstraints(1, 0, 1, 1, 0.1, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 1, 2, 5), 5, 0));
		northPanel.add(new JLabel("TSID:"),
			new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 5, 2, 0), 0, 0));
		tsidField.setEditable(false);
		northPanel.add(tsidField,
			new GridBagConstraints(3, 0, 1, 1, 0.6, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 1, 2, 5), 15, 0));
		northPanel.add(new JLabel("Last Modified:"),
			new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 10, 2, 0), 0, 0));
		lastModifiedField.setEditable(false);
		northPanel.add(lastModifiedField,
			new GridBagConstraints(5, 0, 1, 1, 0.3, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 1, 2, 5), 4, 0));
		
		JScrollPane descPane = new JScrollPane();
		descPane.setBorder(new TitledBorder(genericResources.getString("description")));
		descArea.setLineWrap(true);
		descPane.getViewport().add(descArea, null);
		descPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		northPanel.add(descPane,
			new GridBagConstraints(0, 1, 4, 1, 1.0, 0.5, 
				GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(2, 5, 2, 5), 0, 0));

		// South Panel is just commit and close buttons
		JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,10,2));
		JButton commitButton = new JButton(genericResources.getString("commit"));
		commitButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					commitPressed();
				}
			});
		southPanel.add(commitButton);
		JButton closeButton = new JButton(genericResources.getString("close"));
		closeButton.addActionListener(
				new ActionListener()
				{
					@Override
					public void actionPerformed(ActionEvent e)
					{
						closePressed();
					}
				});
		southPanel.add(closeButton);

		// Center Panel has most of the info. It has a left pane and a right pane
		JPanel centerPanel = new JPanel(new GridBagLayout());
		JPanel centerLeft = new JPanel(new GridBagLayout());
		JPanel centerRight = new JPanel(new GridBagLayout());
		centerPanel.add(centerLeft,
			new GridBagConstraints(0, 0, 1, 1, 0.5, .2, 
				GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(2, 2, 2, 2), 0, 0));
		centerPanel.add(centerRight,
				new GridBagConstraints(1, 0, 1, 1, 0.5, .2, 
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(2, 2, 2, 2), 0, 0));

		// Center Left has settable params for the TS_SPEC
		centerLeft.add(new JLabel(groupResources.getString("TsEditPanel.location") + ":"),
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 5, 2, 0), 0, 0));
		centerLeft.add(locationField,
				new GridBagConstraints(1, 0, 1, 1, 0.5, 0.0, 
					GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
					new Insets(2, 1, 2, 5), 0, 0));
		JButton selectLocationButton = new JButton(genericResources.getString("select"));
		selectLocationButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					selectLocationPressed();
				}
			});
		centerLeft.add(selectLocationButton,
			new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(2, 1, 2, 5), 0, 0));
		
		centerLeft.add(new JLabel(groupResources.getString("TsEditPanel.param") + ":"),
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 5, 2, 0), 0, 0));
		centerLeft.add(paramField,
				new GridBagConstraints(1, 1, 1, 1, 0.5, 0.0, 
					GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
					new Insets(2, 1, 2, 5), 0, 0));
//		JButton selectParamButton = new JButton(genericResources.getString("select"));
//		selectParamButton.addActionListener(
//			new ActionListener()
//			{
//				@Override
//				public void actionPerformed(ActionEvent e)
//				{
//					selectParamPressed();
//				}
//			});
//		centerLeft.add(selectParamButton,
//			new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, 
//				GridBagConstraints.WEST, GridBagConstraints.NONE,
//				new Insets(2, 1, 2, 5), 0, 0));
		
		// Load statCodeCombo, first item is no selection
		try
		{
			statcodes = parent.getTsDb().listParamTypes();
			statcodes.add(0, "");
			for(String sc : statcodes)
				statCodeCombo.addItem(sc);
		}
		catch (DbIoException e1)
		{
			Logger.instance().warning("Error getting stat code list: " + e1);
		}
		statCodeCombo.setEditable(true);
		
		
		centerLeft.add(new JLabel(groupResources.getString("TsEditPanel.statcode") + ":"),
			new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 5, 2, 0), 0, 0));
		centerLeft.add(statCodeCombo,
			new GridBagConstraints(1, 2, 1, 1, 0.5, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 1, 2, 5), 0, 0));
		
		
		// Load interval and duration Combos. First item is no selection.
		IntervalDAI intervalDAO = parent.getTsDb().makeIntervalDAO();
		try
		{
			intervalCombo.addItem("");
			intervalCodes = intervalDAO.getValidIntervalCodes();
			for(String s : intervalCodes)
				intervalCombo.addItem(s);
			durationCombo.addItem("");
			durationCodes = intervalDAO.getValidDurationCodes();
			for(String s : durationCodes)
				durationCombo.addItem(s);
Interval zeroInt = IntervalList.instance().getByName("0");
Logger.instance().debug2(module + " guiInit after populating intv & dur combos, '0' has key="
+  zeroInt.getKey());

		}
		finally
		{
			intervalDAO.close();
		}
		intervalCombo.setEditable(false);
		durationCombo.setEditable(false);
		
		centerLeft.add(new JLabel(groupResources.getString("TsEditPanel.interval") + ":"),
			new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 5, 2, 0), 0, 0));
		centerLeft.add(intervalCombo,
			new GridBagConstraints(1, 3, 1, 1, 0.5, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 1, 2, 5), 0, 0));
		
		centerLeft.add(new JLabel(groupResources.getString("TsEditPanel.duration") + ":"),
			new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 5, 2, 0), 0, 0));
		centerLeft.add(durationCombo,
			new GridBagConstraints(1, 4, 1, 1, 0.5, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 1, 2, 5), 0, 0));
		
		// Load versionCombo. First item is no selection.
		try
		{
			versions = parent.getTsDb().listVersions();
			if (versions != null)
			{
				versions.add(0, "");
				for(String v : versions)
					versionCombo.addItem(v);
			}
		}
		catch (DbIoException e1)
		{
			Logger.instance().warning("Error listing versions: " + e1);
		}
		versionCombo.setEditable(true);
		
		centerLeft.add(new JLabel(groupResources.getString("TsEditPanel.version") + ":"),
			new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 5, 2, 0), 0, 0));
		centerLeft.add(versionCombo,
			new GridBagConstraints(1, 5, 1, 1, 0.5, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 1, 2, 5), 0, 0));

		activeCheck.setText(genericResources.getString("active"));
		centerLeft.add(activeCheck,
			new GridBagConstraints(0, 6, 3, 1, 0.0, 0.0, 
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(2, 5, 2, 5), 0, 0));
		
		centerLeft.add(new JLabel(genericResources.getString("units") + ":"),
				new GridBagConstraints(0, 7, 1, 1, 0.0, 0.0, 
					GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(2, 5, 2, 0), 0, 0));
		centerLeft.add(unitsField,
			new GridBagConstraints(1, 7, 1, 1, 0.5, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 1, 2, 5), 0, 0));
		unitsField.setEditable(false);
		JButton unitsSelectButton = new JButton(genericResources.getString("select"));
		unitsSelectButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					selectUnitsPressed();
				}
			});
		centerLeft.add(unitsSelectButton,
			new GridBagConstraints(2, 7, 1, 1, 0.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(2, 1, 2, 5), 0, 0));

		allowOffsetVarCheck.setText(groupResources.getString("TsEditPanel.allowOffsetVar"));
		centerLeft.add(allowOffsetVarCheck,
			new GridBagConstraints(0, 8, 3, 1, 0.0, 0.0, 
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(2, 5, 2, 5), 0, 0));

		centerLeft.add(new JLabel(groupResources.getString("TsEditPanel.utcOffset") + ":"),
			new GridBagConstraints(0, 9, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 5, 2, 0), 0, 0));
		centerLeft.add(utcOffsetField,
			new GridBagConstraints(1, 9, 1, 1, 0.5, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 1, 2, 5), 0, 0));


		centerLeft.add(new JLabel(groupResources.getString("TsEditPanel.onError") + ":"),
			new GridBagConstraints(0, 10, 1, 1, 0.0, 0.0, 
				GridBagConstraints.NORTHEAST, GridBagConstraints.NONE,
				new Insets(2, 5, 2, 0), 0, 0));
		centerLeft.add(onOffsetErrCombo,
			new GridBagConstraints(1, 10, 1, 1, 0.5, 1.0, 
				GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 1, 2, 5), 0, 0));

		// Center Right has non-settable statistics
		centerRight.add(new JLabel(genericResources.getString("type") + ":"),
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 5, 2, 0), 0, 0));
		centerRight.add(numericStringCombo,
			new GridBagConstraints(1, 0, 1, 1, 0.5, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 1, 2, 5), 0, 0));

		centerRight.add(new JLabel(groupResources.getString("TsEditPanel.tableNum") + ":"),
				new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, 
					GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(2, 5, 2, 0), 0, 0));
		dataTableField.setEditable(false);
		centerRight.add(dataTableField,
			new GridBagConstraints(1, 1, 1, 1, 0.5, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 1, 2, 5), 0, 0));

		centerRight.add(new JLabel(groupResources.getString("TsEditPanel.numValues") + ":"),
				new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, 
					GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(2, 5, 2, 0), 0, 0));
		numValuesField.setEditable(false);
		centerRight.add(numValuesField,
			new GridBagConstraints(1, 2, 1, 1, 0.5, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 1, 2, 5), 0, 0));

		centerRight.add(new JLabel("Min:"),
				new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, 
					GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(2, 5, 2, 0), 0, 0));
		minField.setEditable(false);
		centerRight.add(minField,
			new GridBagConstraints(1, 3, 1, 1, 0.5, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 1, 2, 5), 0, 0));

		centerRight.add(new JLabel("Max:"),
				new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, 
					GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(2, 5, 2, 0), 0, 0));
		maxField.setEditable(false);
		centerRight.add(maxField,
			new GridBagConstraints(1, 4, 1, 1, 0.5, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 1, 2, 5), 0, 0));

		centerRight.add(new JLabel(groupResources.getString("TsEditPanel.oldest") + ":"),
				new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0, 
					GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(2, 5, 2, 0), 0, 0));
		oldestField.setEditable(false);
		centerRight.add(oldestField,
			new GridBagConstraints(1, 5, 1, 1, 0.5, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 1, 2, 5), 0, 0));
	
		centerRight.add(new JLabel(groupResources.getString("TsEditPanel.newest") + ":"),
			new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 5, 2, 0), 0, 0));
		newestField.setEditable(false);
		centerRight.add(newestField,
			new GridBagConstraints(1, 6, 1, 1, 0.5, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 1, 2, 5), 0, 0));
		
		JButton exportValuesButton = 
			new JButton(groupResources.getString("TsEditPanel.exportValues"));
		exportValuesButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					exportValuesPressed();
				}
			});
		centerRight.add(exportValuesButton,
			new GridBagConstraints(1, 7, 1, 1, 0.0, 1.0, 
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(2, 0, 2, 0), 0, 0));

		
		// Add the northPanel, centerPanel, and tsEntityOpsPanel into this panel
		this.add(northPanel, BorderLayout.NORTH);
		this.add(centerPanel, BorderLayout.CENTER);
		this.add(southPanel, BorderLayout.SOUTH);
	}

	protected void exportValuesPressed()
	{
		if (tsidData == null || tsidData.size() == 0)
		{
			JOptionPane.showMessageDialog(this, "No data to export.");
			return;
		}
		File outputFile = null;
		while(outputFile == null)
		{
			int fcr = 0;
			if ((fcr = fileChooser.showSaveDialog(this)) == JFileChooser.APPROVE_OPTION)
			{
				outputFile = fileChooser.getSelectedFile();
				if (outputFile.exists())
				{
					int opr = JOptionPane.showConfirmDialog(this, "That file exists. Overwrite?");
					if (opr == JOptionPane.CANCEL_OPTION)
						return;
					else if (opr == JOptionPane.YES_OPTION)
						break;
					else
						outputFile = null;
				}
			}
			else if (fcr == JFileChooser.CANCEL_OPTION)
				return;
			PrintWriter pw = null;
			try
			{
				pw = new PrintWriter(outputFile);
				pw.println("SET:TZ=UTC");
				pw.println("TSID:" + tsid.getUniqueString());
				pw.println("SET:UNITS=" + tsidData.getUnitsAbbr());
				for(int idx = 0; idx < tsidData.size(); idx++)
				{
					TimedVariable tv = tsidData.sampleAt(idx);
					String v = "";
					if (tsid.getStorageType() == 'N')
					{
						try { v = exportNumFmt.format(tv.getDoubleValue()); }
						catch(NoConversionException ex) { continue; }
					}
					else
						v = tv.getStringValue();
					pw.println(exportSdf.format(tv.getTime()) + "," + v + ","
						+ Integer.toHexString(tv.getFlags()));
				}
			}
			catch (FileNotFoundException ex)
			{
				parent.showError("Cannot open export file: " + ex);
			}
			finally
			{
				if (pw != null)
					try { pw.close(); } catch(Exception ex) {}
			}
		}
		
		if (!outputFile.canWrite())
		{
			JOptionPane.showMessageDialog(this, "File '" + outputFile.getPath() + " is not writable.");
			return;
		}
		
		
	}

	protected void selectUnitsPressed()
	{
		EUSelectDialog dlg = new EUSelectDialog(parent);
		parent.launchDialog(dlg);
		EngineeringUnit eu = dlg.getSelection();
		if (eu != null && !eu.getAbbr().equalsIgnoreCase(unitsField.getText()))
			unitsField.setText(eu.getAbbr());
	}

	protected void selectLocationPressed()
	{
		SiteSelectDialog dlg = new SiteSelectDialog(this);
		parent.launchDialog(dlg);
		Site site = dlg.getSelectedSite();
		if (site != null)
		{
			selectedSite = site;
			SiteName sn = site.getName(Constants.snt_CWMS);
			if (sn == null)
				sn = site.getPreferredName();
			locationField.setText(sn.getNameValue());
		}
	}

	protected void closePressed()
	{
		CwmsTsId tmpTsid = new CwmsTsId();
		if (!getDataFromFields(tmpTsid))
		{
			if (JOptionPane.showConfirmDialog(this, "Close without save?", "Close Without Save", 
				JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION)
				return;
		}
		else if (changesMade(tsid, tmpTsid))
		{
			int r = JOptionPane.showConfirmDialog(this, genericResources.getString("saveChanges"));
			if (r == JOptionPane.CANCEL_OPTION)
				return;
			else if (r == JOptionPane.YES_OPTION)
			{
//				getDataFromFields(tsid);
				commitPressed();
			} 
		}
		
		// close the panel and remove from tabbed pane.
		parent.removeEditPane(this);
	}
	
	/**
	 * Compare the editable fields in the two TSIDs.
	 * @param orig
	 * @param tmp
	 * @return true if there are any differences, false if they are the same.
	 */
	private boolean changesMade(CwmsTsId orig, CwmsTsId tmp)
	{
//System.out.println("cm1");
		if (!TextUtil.strEqual(orig.getDescription(), tmp.getDescription()))
			return true;
//System.out.println("cm2 orig '" + orig.getUniqueString() + "' tmp '" + tmp.getUniqueString() + "'");
		
		// This will get all 6 of the TSID fields:
		if (!TextUtil.strEqual(orig.getUniqueString(), tmp.getUniqueString()))
			return true;
//System.out.println("cm3");
		if (orig.isActive() != tmp.isActive())
			return true;
//System.out.println("cm4");
		if (!TextUtil.strEqual(orig.getStorageUnits(), tmp.getStorageUnits()))
			return true;
//System.out.println("cm5");
		if (orig.isAllowDstOffsetVariation() != tmp.isAllowDstOffsetVariation())
			return true;
//System.out.println("cm6");
		if (!TextUtil.intEqual(orig.getUtcOffset(), tmp.getUtcOffset()))
			return true;
//System.out.println("cm7");
		if (!orig.getOffsetErrorAction().equals(tmp.getOffsetErrorAction()))
			return true;

		// All tests pass. No changes made.
		return false;
	}

	protected void commitPressed()
	{
		CwmsTsId tmpTsid = new CwmsTsId();
		if (!getDataFromFields(tmpTsid))
			return;
Logger.instance().debug2("commitPressed - passed validation. tsid.getKey()=" + tsid.getKey()
+ ", tsid='" + tmpTsid.getUniqueString() + "'");
		DataType dt = tmpTsid.getDataType();
		if (DbKey.isNull(dt.getKey())) // This will be the case if datatype didn't previously exist.
		{
			int r = JOptionPane.showConfirmDialog(this, "CWMS Param '" 
				+ dt.getCode() + "' does not yet exist in this database. Create?",
				"New Datatype", JOptionPane.YES_NO_OPTION);
			if (r == JOptionPane.NO_OPTION)
				return;
			DataTypeDAI dtDAO = parent.getTsDb().makeDataTypeDAO();
			try
			{
				dtDAO.writeDataType(dt);
			}
			catch (DbIoException ex)
			{
				parent.showError("Cannot save data type '" + dt + "': " + ex);
				return;
			}
			finally
			{
				dtDAO.close();
			}
		}
		String origUnits = tsid.getStorageUnits();

		TimeSeriesDAI tsDAO = parent.getTsDb().makeTimeSeriesDAO();
		LoadingAppDAI appDAO = null;
		String action = "testing uniqueness";
		try
		{
			// If there is an existing ID with this unique string
			TimeSeriesIdentifier existingTsid = tsDAO.getTimeSeriesIdentifier(tmpTsid.getUniqueString());
			if (existingTsid != null
			 // AND either this a new TSID OR it has a different key
			 && (DbKey.isNull(tsid.getKey())
			  || !tsid.getKey().equals(existingTsid.getKey())))
			{
				parent.showError("There is already an existing TSID with key=" 
					+ existingTsid.getKey() + " that has this unique identifier '"
					+ tmpTsid.getUniqueString() + "'");
				return;
			}
			
			action = "doing setup";
			getDataFromFields(tsid);
	Logger.instance().debug2("commitPressed - after getDataFF. tsid.getKey()=" + tsid.getKey());
			
			// If there is existing data in the DB and the units have changed, give the user
			// the option to convert existing data to the new units.
			boolean saveData = false;
	//System.out.println("commitPressed tsidData " +
	//(tsidData==null ? "is null" : ("size=" + tsidData.size()+",units=" + tsid.getStorageUnits()))
	//+ ", origUnits=" + origUnits);
			if (!DbKey.isNull(tsid.getKey()) && tsidData != null && tsidData.size() > 0 
			 && !tsid.getStorageUnits().equalsIgnoreCase(origUnits))
			{
				String msg = "There are currently " + tsidData.size() + " values stored "
					+ "for this time series in the database. You have changed the units "
					+ "from " + origUnits + " to " + unitsField.getText() + ". "
					+ "Do you want to convert the values currently stored in the database?";
				int r = JOptionPane.showConfirmDialog(this, AsciiUtil.wrapString(msg, 60));
				
				if (r == JOptionPane.CANCEL_OPTION)
					return; // Abort COMMIT
				else if (r == JOptionPane.NO_OPTION)
				{
					// Don't Save the data. Changing the units identifier only.
				}
				else if (r == JOptionPane.YES_OPTION)
				{
					// Change the tsid storage units, but leave the units identifier in the
					// CTimeSeries alone. This will cause saveTimeSeries to do a conversion to
					// the new storage units.
					// Set the TO_WRITE flag on every value.
					for(int idx = 0; idx < tsidData.size(); idx++)
						VarFlags.setToWrite(tsidData.sampleAt(idx));
					saveData = true;
				}
				tsid.setStorageUnits(unitsField.getText());
				Logger.instance().debug2("Units different, changed tsid units to " + tsid.getStorageUnits());
			}

			if (saveData)
			{
				// App ID is required to write time series data.
				if (DbKey.isNull(parent.getTsDb().getAppId()))
				{
					action = "setting parent app ID to 'utility'";
					appDAO = parent.getTsDb().makeLoadingAppDAO();
					parent.getTsDb().setAppId(appDAO.lookupAppId("utility"));
				}
				action = "converting units and saving time series data";
				tsDAO.saveTimeSeries(tsidData);
			}
			if (DbKey.isNull(tsid.getKey()))
			{
				action = "creating time series";
				tsDAO.createTimeSeries(tsid);
				parent.setTabLabel(this, tsid.getKey().toString());
			}
			else
			{
				action = "modifying time series";
				tsDAO.modifyTSID(tsid);
			}
		}
		catch (Exception ex)
		{
			String msg = "Error while " + action + ": " + ex;
			Logger.instance().warning(msg);
			if (Logger.instance().getLogOutput() != null)
				ex.printStackTrace(Logger.instance().getLogOutput());
			parent.showError(msg);
		}
		finally
		{
			if (appDAO != null)
				appDAO.close();
			tsDAO.close();
		}
		
		// call setTsSpec again to set key, TSID and stat fields.
		setTsSpec(tsid);
	}

	/**
	 * Set the TSID to be edited or created
	 * @param tsid the time series ID
	 * @param stats If this is an existing time series it will have first, last, min, max.
	 */
	public void setTsSpec(CwmsTsId tsid)
	{
		this.tsid = tsid;
		boolean newTsid = DbKey.isNull(tsid.getKey());
		TimeSeriesDAI tsDao = parent.getTsDb().makeTimeSeriesDAO();
		String tableName = "";
		if (!newTsid)
		{
			try
			{
				tsidData = tsDao.makeTimeSeries(tsid);
				tsDao.fillTimeSeriesMetadata(tsidData);
				tsDao.fillTimeSeries(tsidData, null, null);
			
				if (tsDao instanceof OpenTimeSeriesDAO)
				{
					tableName = ((OpenTimeSeriesDAO)tsDao).makeDataTableName(tsid);
				}
			}
			catch (DbIoException ex)
			{
				parent.showError("Error while filling time series and metadata: " + ex);
			}
			catch (NoSuchObjectException ex)
			{
				Logger.instance().warning("setTsSpec: " + ex);
			}
			catch (BadTimeSeriesException ex)
			{
				Logger.instance().warning("setTsSpec: " + ex);
			}
			finally
			{
				tsDao.close();
			}
		}
		else
			tsidData = null;

		keyField.setText(newTsid ? "" : (""+tsid.getKey()));
		tsidField.setText(newTsid ? "" : tsid.getUniqueString());
		descArea.setText(tsid.getDescription() == null ? "" : tsid.getDescription());
		if (tsid.getLastModified() == null)
			lastModifiedField.setText("");
		else
			lastModifiedField.setText(sdf.format(tsid.getLastModified()));
		
		String s = tsid.getSiteName();
		locationField.setText(s == null ? "" : s);
		selectedSite = tsid.getSite();
		s = tsid.getPart("param");
		paramField.setText(s == null ? "" : s);
		s = tsid.getParamType();
		if (s == null)
			statCodeCombo.setSelectedIndex(0); // the top is always no-selection
		else
		{
			boolean found = false;
			for(int idx = 0; idx < statcodes.size(); idx++)
				if (s.equalsIgnoreCase(statcodes.get(idx)))
				{
					statCodeCombo.setSelectedIndex(idx);
					found = true;
					break;
				}
			if (!found)
				statCodeCombo.setSelectedItem(s);
		}
		s = tsid.getInterval();
		if (s == null)
			intervalCombo.setSelectedIndex(0); // the top is always no-selection
		else
		{
			for(int idx = 0; idx < intervalCodes.length; idx++)
				if (s.equalsIgnoreCase(intervalCodes[idx]))
				{
					intervalCombo.setSelectedIndex(idx+1); // +1 because combo has blank in the front.
					break;
				}
		}
		s = tsid.getDuration();
		if (s == null)
			durationCombo.setSelectedIndex(0); // the top is always no-selection
		else
		{
			for(int idx = 0; idx < durationCodes.length; idx++)
				if (s.equalsIgnoreCase(durationCodes[idx]))
				{
					durationCombo.setSelectedIndex(idx+1); // +1 because combo has blank in the front.
					break;
				}
		}
		
		s = tsid.getVersion();
		if (s == null)
			versionCombo.setSelectedIndex(0); // the top is always no-selection
		else
		{
			boolean found = false;
			for(int idx = 0; idx < versions.size(); idx++)
				if (s.equals(versions.get(idx)))
				{
					versionCombo.setSelectedIndex(idx);
					found = true;
					break;
				}
			if (!found)
				versionCombo.setSelectedItem(s);
		}

		activeCheck.setSelected(tsid.isActive());
		s = tsid.getStorageUnits();
		unitsField.setText(s == null ? "" : s);
		
		allowOffsetVarCheck.setSelected(tsid.isAllowDstOffsetVariation());
		utcOffsetField.setText("" + 
			(tsid.getUtcOffset() == null ? "" : tsid.getUtcOffset()));
		onOffsetErrCombo.setSelectedItem(tsid.getOffsetErrorAction().toString());
		
		boolean isNumeric = tsid.getStorageType() == 'N';
		numericStringCombo.setSelectedIndex(isNumeric ? 0 : 1);
		
		// Can only set numeric/string if this is a new TSID
		numericStringCombo.setEditable(false);
		numericStringCombo.setEnabled(newTsid);
		
		dataTableField.setText(tableName);
		
		numValuesField.setText("0");
		minField.setText("");
		maxField.setText("");
		oldestField.setText("");
		newestField.setText("");
		
		// Note: tsidData from above may be null!
		if (tsidData != null)
		{
			int n = tsidData.size();
			numValuesField.setText("" + n);
			if (n > 0)
			{
				TimedVariable oldest = tsidData.sampleAt(0);
				TimedVariable newest = tsidData.sampleAt(n - 1);
//System.out.println("n=" + n + ", oldest=" + oldest + ", newest=" + newest);
				TimedVariable min = null, max = null;
				double maxV = Double.NEGATIVE_INFINITY, minV = Double.POSITIVE_INFINITY;
				String maxS = null, minS = null;
				for(int idx = 0; idx < n; idx++)
				{
					TimedVariable tv = tsidData.sampleAt(idx);
					if (idx == 0)
					{
						min = max = tv;
						if (isNumeric)
						{
							try { maxV = minV = tv.getDoubleValue(); } catch(Exception ex) {}
						}
						else
						{
							maxS = minS = tv.getStringValue();
						}
					}
					else // Not first element -- compare.
					{
						if (isNumeric)
						{
							try
							{
								double v = tv.getDoubleValue();
								if (v > maxV)
								{
									max = tv;
									maxV = v;
								}
								else if (v < minV)
								{
									min = tv;
									minV = v;
								}
							}
							catch(NoConversionException ex) {}
						}
						else // string value
						{
							String v = tv.getStringValue();
							if (v.compareTo(maxS) > 0)
							{
								maxS = v;
								max = tv;
							}
							else if (v.compareTo(minS) < 0)
							{
								minS = v;
								min = tv;
							}
						}
					}
				}
				oldestField.setText(formatTV(oldest));
				newestField.setText(formatTV(newest));
				maxField.setText(formatTV(max));
				minField.setText(formatTV(min));
			}
		}
	}
	
	private String formatTV(TimedVariable tv)
	{
		String s = "";
		try
		{
			double d = tv.getDoubleValue();
			s = exportNumFmt.format(d);
		}
		catch (NoConversionException e)
		{
			s = tv.getStringValue();
		}
		return s + " @ " + sdf.format(tv.getTime());
	}

	/**
	 * Collect data from the GUI controls and store into editedGroup.
	 * @return true if passed validation. False if fields have errors or are incomplete.
	 */
	private boolean getDataFromFields(CwmsTsId tsid)
	{
		String s = descArea.getText().trim();
		if (s.length() == 0)
			s = null;
		tsid.setDescription(s);
		
		s = locationField.getText().trim();
		if (s.length() == 0)
		{
			parent.showError("Location cannot be blank!");
			return false;
		}
		tsid.setSite(selectedSite);
		tsid.setSiteName(locationField.getText().trim());
		
		
		s = paramField.getText().trim();
		if (s.length() == 0)
		{
			parent.showError("Param cannot be blank!");
			return false;
		}
		tsid.setPart("param", s);
		DataType dt = DataType.getDataType(Constants.datatype_CWMS, s);
		tsid.setDataType(dt);
		
		s = (String)statCodeCombo.getSelectedItem();
		if (s == null || s.trim().length() == 0)
		{
			parent.showError("Statistics Code (CWMS Param Type) cannot be blank.");
			return false;
		}
		tsid.setPart("statcode", s.trim());

		s = (String)intervalCombo.getSelectedItem();
		if (s == null || s.trim().length() == 0)
		{
			parent.showError("Interval cannot be blank!");
			return false;
		}
		tsid.setInterval(s.trim());
		
		s = (String)durationCombo.getSelectedItem();
		if (s == null || s.trim().length() == 0)
		{
			parent.showError("Duration cannot be blank!");
			return false;
		}
		tsid.setPart("duration", s.trim());
		
		s = (String)versionCombo.getSelectedItem();
		if (s == null || s.trim().length() == 0)
		{
			parent.showError("Version cannot be blank!");
			return false;
		}
		tsid.setPart("version", s.trim());

		tsid.setActive(activeCheck.isSelected());
		
		s = unitsField.getText().trim();
		if (s.length() == 0)
		{
			String spg = parent.getTsDb().getProperty("storagePresentationGroup");
			if (spg == null)
				spg = "CWMS-English";
			PresentationGroup pg = decodes.db.Database.getDb().getPresentationGroupList().find(spg);
			if (pg == null)
			{
				parent.showError("No Units specified and cannot get presentation group '" + spg + "'!");
				return false;
			}
			DataPresentation dp = pg.findDataPresentation(tsid.getDataType());
			if (dp == null)
			{
				parent.showError("No Units specified and cannot find presentation group entry for '"
					+ tsid.getDataType() + "!");
				return false;
			}
			s = dp.getUnitsAbbr();
			unitsField.setText(s);
		}
		tsid.setStorageUnits(s);

		tsid.setAllowDstOffsetVariation(allowOffsetVarCheck.isSelected());
		
		Integer offset = 0;
		s = utcOffsetField.getText().trim();
		if (s.length() > 0)
		{
			try { offset = Integer.parseInt(s); }
			catch(NumberFormatException ex)
			{
				parent.showError("UTC Offset must be integer number of seconds!");
				return false;
			}
		}
		else
			offset = null;
		tsid.setUtcOffset(offset);
		
		OffsetErrorAction oea = (OffsetErrorAction)onOffsetErrCombo.getSelectedItem();
		tsid.setOffsetErrorAction(oea);

		if (numericStringCombo.isEditable())
		{
			s = (String)numericStringCombo.getSelectedItem();
			tsid.setStorageType(s.charAt(0));
		}
			
		return true;
	}

	public CwmsTsId getTsid()
	{
		return tsid;
	}


}