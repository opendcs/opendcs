/*
*  $Id$
*/
package lrgs.gui;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;

import decodes.util.CmdLineArgs;

import java.net.URL;
import java.net.MalformedURLException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import ilex.gui.*;
import ilex.cmdline.*;
import ilex.util.IDateFormat;
import ilex.util.FileExceptionList;
import ilex.util.LoadResourceBundle;
import ilex.util.TextUtil;
import ilex.corba.ClientAppSettings;

import lrgs.common.*;

/**
This frame implements the search criteria editor
*/
public class SearchCriteriaEditor extends MenuFrame
	implements Editor, SearchCriteriaEditorIF
{
	private static ResourceBundle labels = null;
	private static ResourceBundle genericLabels = null;
	
	private static final String TITLE = "Search Criteria Editor: ";
	private File scfile;
	private SearchCriteria searchcrit;
	private EditorMenuSet menuset;
	private SearchCritEditorParent parent;

	private JTextField lsince, luntil, dsince, duntil, netlist, names,
		channels, addresses;
	private JCheckBox email, dcpbull, globbull;
	private JComboBox retrans, status;
	private String combochoice[] = { "Yes", "No", "Only" };

	// Saved state variables to detect changes:
	private String lsincev, luntilv, dsincev, duntilv, netlistv, namesv,
		channelsv, addressesv;
	private boolean emailv, dcpbullv, globbullv;
	private int retransv, statusv, spacecraftv;
	private int baudv;
	private String dataSourcev;

	private static JFileChooser filechooser;
	private boolean autoSave;
	private String scStrings[] = { "Any", "East", "West" };
	private JComboBox scCombo;
	private String baudStrings[] = { "Any", "100 Only", "300 Only", "1200 Only",
		"100-300", "100,1200", "300-1200" };
	private JComboBox baudCombo;
	private JTextField dataSourceField;

	public SearchCriteriaEditor()
	{
		super(TITLE);
		
		labels = MessageBrowser.getLabels();
    	genericLabels = MessageBrowser.getGenericLabels();
    	setTitle(labels.getString("SearchCriteriaEditor.frameTitle"));
    	filechooser = new JFileChooser();
    	dataSourceField = new JTextField();
		scfile = null;
		parent = null;
		init();
		filechooser.setCurrentDirectory(
			new File(System.getProperty("user.dir")));
		autoSave = false;
	}

	/**
	  Sets the autosave flag, meaning file will be saved when editor exits.
	  @param tf the flag value.
	*/
	public void setAutoSave(boolean tf)
	{
		autoSave = tf;
	}

	public SearchCriteriaEditor(File scfile)
		throws IOException
	{
		super(TITLE + scfile.getName());
		labels = MessageBrowser.getLabels();
    	genericLabels = MessageBrowser.getGenericLabels();
    	filechooser = new JFileChooser();
    	dataSourceField = new JTextField();
    	setTitle(labels.getString("SearchCriteriaEditor.frameTitle")
    			+ scfile.getName());
    	
		this.scfile = scfile;
		parent = null;
		init();
		if (scfile != null)
		{
			if (scfile.canRead())
			{
				try { searchcrit.parseFile(scfile); }
				catch(SearchSyntaxException ex)
				{
					System.out.println(ex.toString());
				}
			}
		}
		fillFields();
	}

	//public void setParent(MessageBrowser parent)
	public void setParent(SearchCritEditorParent parent)
	{
		this.parent = parent;
	}

	private static void initProperties()
	{
//		GuiApp.getProperty("SearchCritEditor.Help",
//			"http://www.ilexeng.com/"
//			+ LrgsApp.SubDir + "/help/SearchCritEditor.html");
	}

	private void init()
	{
		initProperties();
		searchcrit = new SearchCriteria();

		menuset = new EditorMenuSet(this, EditorMenuSet.ALL);
		buildMenuBar();

		Container contpane = getContentPane();
		contpane.setLayout(new BorderLayout());

		// South contains 2 panels, left=special addresses, right=combos.
		JPanel south = new JPanel(new GridLayout(1, 2, 4, 4));
		JPanel left = new JPanel(new GridLayout(3, 1, 4, 4));
		left.setBorder(new TitledBorder(
				labels.getString("SearchCriteriaEditor.specialAddr")));
		email = new JCheckBox(
				labels.getString("SearchCriteriaEditor.dcsUserEmail"));

		JPanel emailCont = new JPanel();
		FlowLayout leftFlowLayout = new FlowLayout(FlowLayout.LEFT);
		emailCont.setLayout(leftFlowLayout);
		emailCont.add(email = new JCheckBox(
				labels.getString("SearchCriteriaEditor.dcsUserEmail")));
		left.add(emailCont);

		dcpbull = new JCheckBox(
				labels.getString("SearchCriteriaEditor.dcpBulletins"));
		JPanel dcpbullCont = new JPanel();
		dcpbullCont.setLayout(leftFlowLayout);
		dcpbullCont.add(dcpbull);
		left.add(dcpbullCont);

		globbull= new JCheckBox(
				labels.getString("SearchCriteriaEditor.globalBulletins"));
		JPanel globbullCont = new JPanel();
		globbullCont.setLayout(leftFlowLayout);
		globbullCont.add(globbull);
		left.add(globbullCont);

		south.add(left);
		JPanel right = new JPanel(new GridLayout(2, 1, 4, 4));
		right.setBorder(new TitledBorder(
				labels.getString("SearchCriteriaEditor.messageQualifiers")));
		JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		p.add(new JLabel(
				labels.getString("SearchCriteriaEditor.retransmissions")));
		p.add(retrans = new JComboBox(combochoice));
		right.add(p);
		p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		p.add(new JLabel(
				labels.getString("SearchCriteriaEditor.dapsStatusMessages")));
		p.add(status = new JComboBox(combochoice));
		right.add(p);
		south.add(right);
		contpane.add(south, BorderLayout.SOUTH);

		JPanel center = new JPanel(new GridBagLayout());
		center.add(new JLabel(
				labels.getString("SearchCriteriaEditor.lrgsTimeRangeSince")),
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.5,
				GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE,
				new Insets(15, 10, 6, 2), 0, 0));
		center.add(lsince = new JTextField(16),
			new GridBagConstraints(1, 0, 1, 1, 0.5, 0.5,
				GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
				new Insets(15, 0, 4, 10), 0, 0));
		center.add(new JLabel(labels.getString("SearchCriteriaEditor.until")),
			new GridBagConstraints(2, 0, 1, 1, 0.0, 0.5,
				GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE,
				new Insets(15, 10, 6, 2), 0, 0));
		center.add(luntil = new JTextField(16),
			new GridBagConstraints(3, 0, 3, 1, 0.5, 0.5,
				GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL,
				new Insets(15, 0, 4, 15), 0, 0));

		center.add(new JLabel(
				labels.getString("SearchCriteriaEditor.dapsTimeRangeSince")),
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(4, 10, 4, 2), 0, 0));
		center.add(dsince = new JTextField(16),
			new GridBagConstraints(1, 1, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(4, 0, 4, 10), 0, 0));
		center.add(new JLabel(labels.getString("SearchCriteriaEditor.until")),
			new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(4, 10, 4, 2), 0, 0));
		center.add(duntil = new JTextField(16),
			new GridBagConstraints(3, 1, 3, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(4, 0, 4, 15), 0, 0));

		center.add(new JLabel(
				labels.getString("SearchCriteriaEditor.networkLists")),
			new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(4, 10, 4, 2), 0, 0));
		center.add(netlist = new JTextField(48),
			new GridBagConstraints(1, 2, 5, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(4, 0, 4, 15), 0, 0));

		center.add(new JLabel(labels.getString("SearchCriteriaEditor.dcpNames")),
			new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(4, 10, 4, 2), 0, 0));
		center.add(names = new JTextField(48),
			new GridBagConstraints(1, 3, 5, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(4, 0, 4, 15), 0, 0));

		center.add(new JLabel(
				labels.getString("SearchCriteriaEditor.dcpAddresses")),
			new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(4, 10, 4, 2), 0, 0));
		center.add(addresses = new JTextField(48),
			new GridBagConstraints(1, 4, 5, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(4, 0, 4, 15), 0, 0));

		center.add(new JLabel(
				labels.getString("SearchCriteriaEditor.goesChannels")),
			new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(4, 10, 4, 2), 0, 0));
		center.add(channels = new JTextField(48),
			new GridBagConstraints(1, 5, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(4, 0, 4, 10), 0, 0));
		center.add(new JLabel(
				labels.getString("SearchCriteriaEditor.spacecraft")),
			new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(4, 10, 4, 2), 0, 0));
		center.add(scCombo = new JComboBox(scStrings),
			new GridBagConstraints(3, 5, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(4, 0, 4, 5), 0, 0));
		center.add(new JLabel(labels.getString("SearchCriteriaEditor.baud")),
			new GridBagConstraints(4, 5, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(4, 10, 4, 2), 0, 0));
		center.add(baudCombo = new JComboBox(baudStrings),
			new GridBagConstraints(5, 5, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(4, 0, 4, 15), 0, 0));

		center.add(new JLabel(
				labels.getString("SearchCriteriaEditor.dataSource")),
			new GridBagConstraints(0, 6, 1, 1, 0.0, 0.5,
				GridBagConstraints.NORTHEAST, GridBagConstraints.NONE,
				new Insets(4, 10, 15, 2), 0, 0));
		center.add(dataSourceField,
			new GridBagConstraints(1, 6, 5, 1, 1.0, 0.5,
				GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
				new Insets(4, 0, 15, 15), 0, 0));

		contpane.add(center, BorderLayout.CENTER);
	}

	/** Sets the screen size. */
	public void setSize(Dimension d)
	{
		if (d.height < 420)
			d.height = 420;
		GuiApp.setProperty("SearchCritEditor.height", ""+d.height);
		GuiApp.setProperty("SearchCritEditor.width", ""+d.width);
	}

	/** Called when screen moved. Saves location in properties. */
	public void movedTo(Point p)
	{
		GuiApp.setProperty("SearchCritEditor.x", ""+p.x);
		GuiApp.setProperty("SearchCritEditor.y", ""+p.y);
	}

	/** Starts the GUI at the specified location. */
	public void startup(int x, int y)
	{
		int width = GuiApp.getIntProperty("SearchCritEditor.width", 750);
		int height = GuiApp.getIntProperty("SearchCritEditor.height", 420);
		if (height < 420)
			height = 420;
		x = GuiApp.getIntProperty("SearchCritEditor.x", x);
		y = GuiApp.getIntProperty("SearchCritEditor.y", y);
		launch(x, y, width, height);
	}

	protected AbstractAction[] getFileMenuActions()
	{
		return menuset.getFileActions();
	}

	protected AbstractAction[] getEditMenuActions()
	{
		return null;
//		return menuset.getEditActions();
	}

	/**
	 * Returns true if a 'Help' menu is to be included.
	 * The default implementation here returns false.
	 */
	protected boolean isHelpMenuEnabled()
	{
		return true;
	}

	protected String getHelpFileName() { return "SearchCritEditor.html"; }

	/**
	 * Performs clean up before TopLevel exits.
	 * Override this method in your subclass. It is called when the user selects
	 * 'Exit' from the file menu. Do any cleanup and resource releasing necessary.
	 * The default implementation here does nothing.
	 */
	public void cleanupBeforeExit()
	{
		checkDiscard();
		if (parent != null)
			parent.closingSearchCritEditor();
	}

	// Populate the GUI fields from the contents of the searchcrit.
	private void fillFields()
	{
		clearFields();

		lsince.setText(searchcrit.getLrgsSince());
		luntil.setText(searchcrit.getLrgsUntil());
		dsince.setText(searchcrit.getDapsSince());
		duntil.setText(searchcrit.getDapsUntil());

		StringBuilder sb = new StringBuilder();
		if (searchcrit.NetlistFiles != null)
			for(int i = 0; i<searchcrit.NetlistFiles.size(); ++i)
				sb.append((String)searchcrit.NetlistFiles.get(i) + ' ');
		netlist.setText(sb.toString());

		sb.setLength(0);
		if (searchcrit.DcpNames != null)
			for(int i = 0; i<searchcrit.DcpNames.size(); ++i)
				sb.append((String)searchcrit.DcpNames.get(i) + ' ');
		names.setText(sb.toString());

		sb.setLength(0);
		if (searchcrit.channels != null)
			for(int i = 0; i < searchcrit.channels.length; ++i)
			{
				if ((searchcrit.channels[i] & SearchCriteria.CHANNEL_AND) != 0)
					sb.append('&');
				sb.append(searchcrit.channels[i] & (~SearchCriteria.CHANNEL_AND));
				sb.append(' ');
			}
		channels.setText(sb.toString());

		sb.setLength(0);
		if (searchcrit.ExplicitDcpAddrs != null)
			for(int i = 0; i<searchcrit.ExplicitDcpAddrs.size(); ++i)
				sb.append(""+(DcpAddress)searchcrit.ExplicitDcpAddrs.get(i) + ' ');
		addresses.setText(sb.toString());

		if (searchcrit.DomsatEmail == SearchCriteria.YES
		 || searchcrit.DomsatEmail == SearchCriteria.ACCEPT)
			email.setSelected(true);
		if (searchcrit.DcpBul == SearchCriteria.YES
		 || searchcrit.DcpBul == SearchCriteria.ACCEPT)
			dcpbull.setSelected(true);
		if (searchcrit.GlobalBul == SearchCriteria.YES
		 || searchcrit.GlobalBul == SearchCriteria.ACCEPT)
			globbull.setSelected(true);

		switch(searchcrit.Retrans)
		{
		case SearchCriteria.UNSPECIFIED:
		case SearchCriteria.ACCEPT:
		case SearchCriteria.YES:
			retrans.setSelectedIndex(0); break; // YES
		case SearchCriteria.REJECT:
		case SearchCriteria.NO:
			retrans.setSelectedIndex(1); break; // NO
		case SearchCriteria.EXCLUSIVE:
			retrans.setSelectedIndex(2); break; // ONLY
		}
		switch(searchcrit.DapsStatus)
		{
		case SearchCriteria.UNSPECIFIED:
		case SearchCriteria.ACCEPT:
		case SearchCriteria.YES:
			status.setSelectedIndex(0); break; // YES
		case SearchCriteria.REJECT:
		case SearchCriteria.NO:
			status.setSelectedIndex(1); break; // NO
		case SearchCriteria.EXCLUSIVE:
			status.setSelectedIndex(2); break; // ONLY
		}

		scCombo.setSelectedIndex(
			searchcrit.spacecraft == SearchCriteria.SC_EAST ? 1 :
			searchcrit.spacecraft == SearchCriteria.SC_WEST ? 2 : 0);
			
		if (searchcrit.baudRates == null 
		 || searchcrit.baudRates.equalsIgnoreCase("any"))
			baudCombo.setSelectedIndex(0);
		else
		{
			boolean incl100 = searchcrit.baudRates.contains("100");
			boolean incl300 = searchcrit.baudRates.contains("300");
			boolean incl1200 = searchcrit.baudRates.contains("1200");
			if (incl100 && !incl300 && !incl1200)
				baudCombo.setSelectedIndex(1);
			else if (!incl100 && incl300 && !incl1200)
				baudCombo.setSelectedIndex(2);
			else if (!incl100 && !incl300 && incl1200)
				baudCombo.setSelectedIndex(3);
			else if (incl100 && incl300 && !incl1200)
				baudCombo.setSelectedIndex(4);
			else if (incl100 && !incl300 && incl1200)
				baudCombo.setSelectedIndex(5);
			else if (!incl100 && incl300 && incl1200)
				baudCombo.setSelectedIndex(6);
			else
				baudCombo.setSelectedIndex(0);
		}

		if (searchcrit.numSources == 0)
			dataSourceField.setText("");
		else
		{
			sb = new StringBuilder();
			for(int i=0; i<searchcrit.numSources; i++)
			{
				String sourceName = DcpMsgFlag.sourceValue2Name(searchcrit.sources[i]);
				if (sourceName != null)
					sb.append(sourceName + " ");
			}
			dataSourceField.setText(sb.toString().trim());
		}

		saveStateVariables();
	}

	private void saveStateVariables()
	{
		lsincev = lsince.getText();
		luntilv = luntil.getText();
		dsincev = dsince.getText();
		duntilv = duntil.getText();
		netlistv = netlist.getText();
		namesv = names.getText();
		channelsv = channels.getText();
		addressesv = addresses.getText();

		emailv = email.isSelected();
		dcpbullv = dcpbull.isSelected();
		globbullv = globbull.isSelected();

		retransv = retrans.getSelectedIndex();
		statusv = status.getSelectedIndex();
		spacecraftv = scCombo.getSelectedIndex();
		baudv = baudCombo.getSelectedIndex();
		dataSourcev = dataSourceField.getText();
	}

	// Populate the searchcrit from the contents of the GUI fields.
	public void fillSearchCrit(SearchCriteria searchcrit)
	{
		searchcrit.clear();

		searchcrit.setLrgsSince(lsince.getText());
		searchcrit.setLrgsUntil(luntil.getText());
		searchcrit.setDapsSince(dsince.getText());
		searchcrit.setDapsUntil(duntil.getText());

		StringTokenizer st = new StringTokenizer(netlist.getText());
		while(st.hasMoreTokens())
			searchcrit.addNetworkList(st.nextToken());

		st = new StringTokenizer(names.getText());
		while(st.hasMoreTokens())
			searchcrit.addDcpName(st.nextToken());

		st = new StringTokenizer(channels.getText());
		while(st.hasMoreTokens())
			searchcrit.addChannelToken(st.nextToken());

		st = new StringTokenizer(addresses.getText());
		while(st.hasMoreTokens())
		{
			String t = st.nextToken();
			try { searchcrit.addDcpAddress(new DcpAddress(t)); }
			catch(NumberFormatException e)
			{
				showError(LoadResourceBundle.sprintf(
					labels.getString("SearchCriteriaEditor.illegalDcpAddrErr"),
					t));
			}
		}

		if (email.isSelected())
			searchcrit.DomsatEmail = SearchCriteria.YES;
		if (dcpbull.isSelected())
			searchcrit.DcpBul = SearchCriteria.YES;
		if (globbull.isSelected())
			searchcrit.GlobalBul = SearchCriteria.YES;
		int idx = retrans.getSelectedIndex();
		searchcrit.Retrans = idx==1 ? SearchCriteria.NO :
			idx == 0 ? SearchCriteria.YES : SearchCriteria.EXCLUSIVE;

		idx = status.getSelectedIndex();
		searchcrit.DapsStatus = idx==1 ? SearchCriteria.NO :
			idx == 0 ? SearchCriteria.YES : SearchCriteria.EXCLUSIVE;

		idx = scCombo.getSelectedIndex();
		searchcrit.spacecraft = 
			idx == 1 ? SearchCriteria.SC_EAST :
			idx == 2 ? SearchCriteria.SC_WEST : SearchCriteria.SC_ANY;

		idx = baudCombo.getSelectedIndex();
		if (idx == 0)
			searchcrit.baudRates = null;
		else
			searchcrit.baudRates = (String)baudCombo.getSelectedItem();

		String v = dataSourceField.getText().trim();
		searchcrit.numSources = 0;
		if (v.length() > 0)
		{
			st = new StringTokenizer(v);
			searchcrit.numSources = 0;
			while(st.hasMoreTokens())
			{
				int si = DcpMsgFlag.sourceName2Value(st.nextToken());
				if (si != -1)
					searchcrit.sources[searchcrit.numSources++] = si;
			}
		}
	}

	private void clearFields()
	{
		lsince.setText("");
		luntil.setText("");
		dsince.setText("");
		duntil.setText("");
		netlist.setText("");
		names.setText("");
		channels.setText("");
		addresses.setText("");
		email.setSelected(false);
		dcpbull.setSelected(false);
		globbull.setSelected(false);
		retrans.setSelectedIndex(0);
		status.setSelectedIndex(0);
		scCombo.setSelectedIndex(0);
		baudCombo.setSelectedIndex(0);
		dataSourceField.setText("");
	}

	private boolean allFieldsEmpty()
	{
		String s = lsince.getText();
		if (s != null && s.trim().length() > 0) return false;
		s = luntil.getText();
		if (s != null && s.trim().length() > 0) return false;
		s = dsince.getText();
		if (s != null && s.trim().length() > 0) return false;
		s = duntil.getText();
		if (s != null && s.trim().length() > 0) return false;
		s = netlist.getText();
		if (s != null && s.trim().length() > 0) return false;
		s = names.getText();
		if (s != null && s.trim().length() > 0) return false;
		s = channels.getText();
		if (s != null && s.trim().length() > 0) return false;
		s = addresses.getText();
		if (s != null && s.trim().length() > 0) return false;
		if (email.isSelected()
		 || dcpbull.isSelected()
		 || globbull.isSelected())
		 	return false;
		if (retrans.getSelectedIndex() != 0
		  || status.getSelectedIndex() != 0)
		 	return false;
		if (scCombo.getSelectedIndex() != 0)
		 	return false;
		if (baudCombo.getSelectedIndex() != 0)
		 	return false;
		if (dataSourceField.getText().trim().length() != 0)
			return false;
//System.out.println("All fields are empty!");
		return true;
	}

	public boolean isChanged()
	{
		if (!lsincev.equals(lsince.getText())
		 || !luntilv.equals(luntil.getText())
		 || !dsincev.equals(dsince.getText())
		 || !duntilv.equals(duntil.getText())
		 || !netlistv.equals(netlist.getText())
		 || !namesv.equals(names.getText())
		 || !channelsv.equals(channels.getText())
		 || !addressesv.equals(addresses.getText())
		 || emailv != email.isSelected()
		 || dcpbullv != dcpbull.isSelected()
		 || globbullv != globbull.isSelected()
		 || retransv != retrans.getSelectedIndex()
		 || statusv != status.getSelectedIndex()
		 || spacecraftv != scCombo.getSelectedIndex()
		 || baudv != baudCombo.getSelectedIndex())
		 	return true;
		String v = dataSourceField.getText().trim();
		if (v.length() == 0) v = null;
		if (!TextUtil.strEqualIgnoreCase(dataSourcev, v))
		 	return true;
		return false;
	}

	private void showParseErrors(FileExceptionList warnings)
	{
		System.out.println(warnings);
	}

	/**
	 * Return true if it's OK to discard the current edit fields.
	 */
	private boolean checkDiscard()
	{
		if ((scfile == null && allFieldsEmpty()) || !isChanged())
			return true;

		int choice = JOptionPane.CANCEL_OPTION;
		if (autoSave)
			choice = JOptionPane.YES_OPTION;
		else
			choice = JOptionPane.showConfirmDialog(getContentPane(),
					genericLabels.getString("saveChanges"));

		if (choice == JOptionPane.CANCEL_OPTION)
			return false;
		if (choice == JOptionPane.NO_OPTION)
			return true;
		if (searchcrit == null || scfile == null)
			saveAsPress();
		else
			savePress();
		return true;
	}

	// File menu actions:
	public void newPress()
	{
		if (!checkDiscard())
			return;
		searchcrit.clear();
		clearFields();
		saveStateVariables();
	}

	public void openPress()
	{
		if (!checkDiscard())
			return;
		int option = filechooser.showOpenDialog(this);
		if (option == JFileChooser.APPROVE_OPTION)
		{
			scfile = filechooser.getSelectedFile();
			searchcrit.clear();
			try { searchcrit.parseFile(scfile); }
			catch(IOException ioe)
			{
				showError(LoadResourceBundle.sprintf(
				labels.getString("SearchCriteriaEditor.cannotReadFileErr"),
				scfile.getPath()) + ioe);
				return;
			}
			catch(SearchSyntaxException ex)
			{
				System.out.println(ex.toString());
			}
			fillFields();
		}
	}

	public void savePress()
	{
		if (scfile == null)
		{
			saveAsPress();
			return;
		}
		fillSearchCrit(searchcrit);
		saveStateVariables();

		try { searchcrit.saveFile(scfile); }
		catch(IOException ioe)
		{
			showError(LoadResourceBundle.sprintf(
				labels.getString("SearchCriteriaEditor.cannotWriteFileErr"),
				scfile.getPath()) + ioe);
			return;
		}
	}

	public void saveAsPress()
	{
		int option = filechooser.showSaveDialog(this);
		if (option == JFileChooser.APPROVE_OPTION)
		{
			scfile = filechooser.getSelectedFile();
			fillSearchCrit(searchcrit);
			saveStateVariables();

			try { searchcrit.saveFile(scfile); }
			catch(IOException ioe)
			{
				showError(LoadResourceBundle.sprintf(
					labels.getString("SearchCriteriaEditor.cannotWriteFileErr"),
					scfile.getPath()) + ioe);
				return;
			}
		}
	}

	// Edit menu actions:
	public void undoPress()
	{
		System.out.println("undoPress");
	}

	public void cutPress()
	{
		System.out.println("cutPress");
	}

	public void copyPress()
	{
		System.out.println("copyPress");
	}

	public void pastePress()
	{
		System.out.println("pastePress");
	}

	public void deletePress()
	{
		System.out.println("deletePress");
	}

	static CmdLineArgs settings = new CmdLineArgs();
	static StringToken filename_arg= new StringToken(
		"", "Filename", "", TokenOptions.optArgument, "");
	static
	{
		settings.addToken(filename_arg);
	}


	/**
	Test main that can be started from command line.
	Usage: SearchCriteriaEditor -f Filename.
	*/
	public static void main(String args[])
		throws Exception
	{
		// Parse command line args & get argument values:
		settings.parseArgs(args);

		GuiApp.setAppName(LrgsApp.ShortID);
		GeneralProperties.init();
		SearchCriteriaEditor me;
		String filename = filename_arg.getValue();
		if (filename != null && filename.length() > 0)
			me = new SearchCriteriaEditor(new File(filename));
		else
			me = new SearchCriteriaEditor();

		GuiApp.setTopFrame(me);
		me.startup(100, 100);
	}

}
