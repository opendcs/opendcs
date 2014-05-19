package decodes.decwiz;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;
import javax.swing.*;
import javax.swing.border.*;

import ilex.util.ArrayUtil;
import ilex.util.EnvExpander;
import ilex.util.Logger;
import decodes.db.*;
import decodes.gui.TopFrame;
import decodes.gui.EnumComboBox;
import decodes.dbeditor.SiteSelectDialog;
import decodes.dbeditor.PlatformSelectDialog;
import decodes.datasource.DataSourceException;
import decodes.datasource.PMParser;
import decodes.datasource.RawMessage;
import decodes.datasource.HeaderParseException;
import decodes.datasource.UnknownPlatformException;
import decodes.util.DecodesSettings;

/**
This panel is the first panel in the decoding wizard. Here the
user enters the file and if necessary, the platform and site
identifications.
*/
public class FileIdPanel 
	extends DecWizPanel
{
	private JLabel filenameLabel = new JLabel();
	private JTextField filenameField = new JTextField();
	private JButton browseButton = new JButton();
	private JButton scanButton = new JButton();
	private JLabel mediumTypeLabel = new JLabel();
	private EnumComboBox mediumTypeCombo = 
		new EnumComboBox(Constants.enum_TMType, "data-logger");
	private JLabel siteLabel = new JLabel();
	private JTextField siteField = new JTextField();
	private JButton siteSelectButton = new JButton();
	private JLabel platformLabel = new JLabel();
	private JTextField platformField = new JTextField();
	private JButton platformSelectButton = new JButton();
	private JLabel fileSizeLabel = new JLabel();
	private JTextField fileSizeField = new JTextField();
	private JLabel bytesLabel = new JLabel();
	private JLabel modifiedLabel = new JLabel();
	private JTextField lastModifiedField = new JTextField();
	private JLabel debugLevelLabel = new JLabel();
	private String dblev[] = { "No Debug Info", "Least Verbose", 
		"More Verbose", "Most Verbose" };
	private JComboBox debugLevelCombo = new JComboBox(dblev);
	private JLabel scriptLabel = new JLabel("Decoding Script:");
	private JLabel presentationGroupLabel = new JLabel("Presentation Group:");
	private JComboBox pgCombo = new JComboBox();
	private JComboBox scriptCombo = new JComboBox();
	private GridBagLayout fieldsLayout = new GridBagLayout();
	private JLabel formatLabel = new JLabel();
	private EnumComboBox decodeFormatCombo = 
		new EnumComboBox(Constants.enum_OutputFormat, "stdmsg");
	private BorderLayout rawDataLayout = new BorderLayout();
	private JPanel rawDataPanel = new JPanel(rawDataLayout);
	private Border rawDataBorder = 
		new TitledBorder(
			BorderFactory.createLineBorder(new Color(153, 153, 153),2),
			"Raw Data");
	private JScrollPane rawDataScrollPane = new JScrollPane();
	private JTextArea rawDataArea = new JTextArea();

	static private JFileChooser fileChooser = new JFileChooser(
		EnvExpander.expand(System.getProperty("user.dir")));

	Site selectedSite = null;
	Platform selectedPlatform = null;
	RawMessage rawMessage = null;
	TransportMedium transportMedium = null;
	private String lastFile = null;
	boolean fileLoaded = false;

	/** Constructor. */
	public FileIdPanel()
	{
		super();
		try
		{
			jbInit();
			fillPresentationCombo();
		}
		catch (Exception exception)
		{
			exception.printStackTrace();
		}
		String fmt = DecodesSettings.instance().decwizOutputFormat;
		if (fmt != null && fmt.length() > 0)
			decodeFormatCombo.setSelection(fmt);
		debugLevelCombo.setSelectedIndex(
			DecodesSettings.instance().decwizDebugLevel);
	}

	private void jbInit() throws Exception
	{
		this.setLayout(fieldsLayout);
		filenameLabel.setText("Input File:");
		//filenameField.setPreferredSize(new Dimension(180, 27));
		filenameField.setToolTipText("Name of file to decode.");
		filenameField.setText("");
		filenameField.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				filenameField_actionPerformed(e);
			}
		});
		browseButton.setPreferredSize(new Dimension(100, 27));
		browseButton.setText("Browse");
		browseButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				browseButtonPressed();
			}
		});
		scanButton.setPreferredSize(new Dimension(100, 27));
		scanButton.setText("Scan");
		scanButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				scanFile();
			}
		});
		mediumTypeLabel.setText("Medium Type:");
		//mediumTypeCombo.setPreferredSize(new Dimension(150, 27));
		//decodeFormatCombo.setPreferredSize(new Dimension(150, 27));
		siteLabel.setText("Site:");
		//siteField.setPreferredSize(new Dimension(150, 27));
		siteField.setToolTipText("Name of site from which data originates.");
		siteField.setText("");
		siteSelectButton.setPreferredSize(new Dimension(100, 27));
		siteSelectButton.setText("Select");
		siteSelectButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				siteSelectButtonPressed();
			}
		});
		platformLabel.setToolTipText("Platform that generated this data.");
		platformLabel.setText("Platform:");
		//platformField.setPreferredSize(new Dimension(150, 27));
		platformSelectButton.setPreferredSize(new Dimension(100, 27));
		platformSelectButton.setText("Select");
		platformSelectButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				platformSelectButtonPressed();
			}
		});
		debugLevelCombo.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					debugLevelComboSelected();
				}
			});

		fileSizeLabel.setText("File Size:");
		//fileSizeField.setPreferredSize(new Dimension(150, 27));
		fileSizeField.setEditable(false);
		fileSizeField.setText("0");
		bytesLabel.setText("(bytes)");
		modifiedLabel.setText("Last Modified:");
		//lastModifiedField.setPreferredSize(new Dimension(150, 27));
		lastModifiedField.setEditable(false);
		lastModifiedField.setText("");
		debugLevelLabel.setText("Debug Level:");
		debugLevelCombo.setPreferredSize(new Dimension(150, 27));
		formatLabel.setText("Decode Format:");

		rawDataPanel.setBorder(rawDataBorder);
		rawDataPanel.add(rawDataScrollPane, BorderLayout.CENTER);
		rawDataScrollPane.getViewport().add(rawDataArea, null);
		rawDataArea.setEditable(true);
		Font oldfont = rawDataArea.getFont();
		rawDataArea.setFont(
			new Font("Monospaced", Font.PLAIN, oldfont.getSize()));

		this.add(filenameLabel, 
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(10, 15, 3, 2), 0, 0));
		this.add(filenameField, 
			new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(10, 0, 3, 0), 0, 0));
		this.add(browseButton, 
			new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(10, 5, 3, 5), 0, 0));
		this.add(scanButton, 
			new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(10, 5, 3, 10), 0, 0));

		this.add(fileSizeLabel, 
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3, 15, 3, 2), 0, 0));
		this.add(fileSizeField, 
			new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(3, 0, 3, 0), 0, 0));
		this.add(bytesLabel, 
			new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(3, 5, 3, 0), 0, 0));

		this.add(modifiedLabel, 
			new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3, 15, 3, 2), 0, 0));
		this.add(lastModifiedField, 
			new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(3, 0, 3, 0), 0, 0));

		this.add(mediumTypeLabel, 
			new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3, 15, 3, 2), 0, 0));
		this.add(mediumTypeCombo, 
			new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(3, 0, 3, 0), 0, 0));

		this.add(siteLabel, 
			new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3, 15, 3, 2), 0, 0));
		this.add(siteField, 
			new GridBagConstraints(1, 4, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(3, 0, 3, 0), 0, 0));
		this.add(siteSelectButton, 
			new GridBagConstraints(2, 4, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(3, 5, 3, 0), 0, 0));

		this.add(platformLabel, 
			new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3, 15, 3, 2), 0, 0));
		this.add(platformField, 
			new GridBagConstraints(1, 5, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(3, 0, 3, 0), 0, 0));
		this.add(platformSelectButton, 
			new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(3, 5, 3, 0), 0, 0));

		this.add(scriptLabel, 
			new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3, 15, 3, 2), 0, 0));
		this.add(scriptCombo, 
			new GridBagConstraints(1, 6, 1, 1, 1.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(3, 0, 3, 0), 0, 0));

		this.add(formatLabel, 
			new GridBagConstraints(0, 7, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3, 15, 3, 2), 0, 0));
		this.add(decodeFormatCombo, 
			new GridBagConstraints(1, 7, 1, 1, 1.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(3, 0, 3, 0), 0, 0));

		this.add(presentationGroupLabel, 
			new GridBagConstraints(0, 8, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3, 15, 3, 2), 0, 0));
		this.add(pgCombo, 
			new GridBagConstraints(1, 8, 1, 1, 1.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(3, 0, 3, 0), 0, 0));

		this.add(debugLevelLabel, 
			new GridBagConstraints(0, 9, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3, 15, 3, 2), 0, 0));
		this.add(debugLevelCombo, 
			new GridBagConstraints(1, 9, 1, 1, 1.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(3, 0, 3, 0), 0, 0));

		this.add(rawDataPanel,
			new GridBagConstraints(0, 10, 4, 1, 1.0, 1.0, 
				GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH,
				new Insets(10, 5, 5, 5), 0, 0));
	}

	/** @return displayable title for the header area. */
	public String getTitle()
	{
		return "Specify File and Site Identification";
	}

	/**
	 * Called when the 'Browse' button is pressed to navigate for a file.
	 */
	private void browseButtonPressed()
	{
		fileLoaded = false;
		selectedSite = null;
		siteField.setText("");
		selectedPlatform = null;
		platformField.setText("");
		transportMedium = null;
		fileChooser.setDialogTitle("Select Raw Input File");
		if (fileChooser.showOpenDialog(this) == fileChooser.APPROVE_OPTION)
		{
			File f = fileChooser.getSelectedFile();
			filenameField.setText(f.getPath());
			scanFile();
		}
	}

	public String getFileName()
	{
		String ret = filenameField.getText();
		int idx = ret.lastIndexOf(File.separator);
		if (idx > 0)
			ret = ret.substring(idx+1);
		return ret;
	}

	public String getFilePath()
	{
		return filenameField.getText();
	}

	/**
	 * Called when the 'Browse' button is pressed to read selected file.
	 */
	private void scanFile()
	{
		byte fileBytes[] = null;
		String currentFile = filenameField.getText();
		if ( fileLoaded ) {
			if ( lastFile == null || !currentFile.equals(lastFile) ) 
				fileLoaded = false;
		}
		if ( !fileLoaded ) {
			lastFile = currentFile;
			File f = new File(filenameField.getText());
			if (!f.canRead())
				showError("The selected file '" + f.getPath() 
					+ "' is not readable.");
			lastModifiedField.setText("" + new Date(f.lastModified()));
			FileReader fr = null;
			rawDataArea.setText("");
			fileBytes = new byte[(int)f.length()];
			try
			{
				fr = new FileReader(f);
				char cbuf[] = new char[256];
				int totalBytes = 0;
				int n;
				int fileBytesIndex = 0;
				boolean initialWhiteSpace = true;
				while((n = fr.read(cbuf)) > 0)
				{
					for(int i=0; i<n; i++)
					{
						if (initialWhiteSpace
						 && !Character.isWhitespace(cbuf[i]))
							initialWhiteSpace = false;
	
						if (!initialWhiteSpace)
							fileBytes[fileBytesIndex++] = (byte)cbuf[i];
	
						if (cbuf[i] == '\r')
							cbuf[i] = (char)0x00AE;
					}
					totalBytes += n;
					rawDataArea.append(new String(cbuf, 0, n));
				}
				String s = "" + totalBytes;
				if (fileBytesIndex < totalBytes)
				{
					s = s + " (" + (totalBytes-fileBytesIndex) + " skipped)";
					fileBytes = ArrayUtil.getField(fileBytes, 0, fileBytesIndex);
				}
				fileSizeField.setText(s);
			}
			catch(Exception ex)
			{
				showError("Error reading " + f.getPath()
					+ ": " + ex);
			}
			finally
			{
				if (fr != null)
					try { fr.close(); } catch(Exception ex) {}
			}
			fileLoaded = true;
		} else {
			String out = getRawData().replaceAll("\u00AE","\r");
			fileBytes =  out.getBytes();
		}
		rawMessage = null;
		try 
		{
			ByteArrayDataSource bads = new ByteArrayDataSource(fileBytes);
			rawMessage = bads.getRawMessage();
			if ( transportMedium != null ) {
				rawMessage.setTransportMedium(transportMedium);		
				bads.parseHeader();
			} else {
				String boxTM = (String)mediumTypeCombo.getSelectedItem();
				bads.parseHeader(boxTM);
			}
			bads.associatePlatform();
			selectedPlatform = rawMessage.getPlatform();
			platformField.setText(selectedPlatform.makeFileName());
			selectedSite = selectedPlatform.getSite();
			if (selectedSite != null)
				siteField.setText(selectedSite.getDisplayName());
			transportMedium = rawMessage.getTransportMedium();
			mediumTypeCombo.setSelectedItem(transportMedium.getMediumType());
			fillScriptCombo();
			scriptCombo.setSelectedItem(transportMedium.scriptName);
		}
		catch(HeaderParseException ex)
		{
			if ( transportMedium == null && selectedSite == null ) {
				showError("Cannot parse header as a USGS-EDL or GOES message."
				+ " You must set the Medium Type, Platform, and Script "
				+ "manually.");
			}
		}
		catch(UnknownPlatformException ex)
		{
			if ( transportMedium == null && selectedSite == null ) {
				showError(ex.getMessage() 
					+ " You must set the Medium Type, Platform, and Site "
					+ "manually.");
			}
		}
		catch(DataSourceException ex)
		{
			if ( transportMedium == null && selectedSite == null ) {
				showError("Database IO Error reading platform record: " + ex
					+ " You must set the Medium Type, Platform, and Site "
					+ "manually.");
			}
		}

//		getSavePanel().setChoosers();
		getDecodePanel().clearData();
	}

	private void filenameField_actionPerformed(ActionEvent e)
	{

	}

	private void siteSelectButtonPressed()
	{
		SiteSelectDialog dlg = new SiteSelectDialog((JPanel)this);
		dlg.setMultipleSelection(false);
		launchDialog(dlg);
		Site site = dlg.getSelectedSite();
		selectedSite = site;
		if (site == null) {
			return;
		}
		siteField.setText(selectedSite.getDisplayName());
		Vector<Platform> pv = Database.getDb().platformList.getPlatforms( selectedSite);
		if ( pv.size() == 1 ) {
			Platform plat = pv.elementAt(0);
			if ( plat != null ) {
				selectedPlatform = plat;
				setTransportMediumInfo();
			}
		} else
			platformField.setText("");
	}

	private void platformSelectButtonPressed()
	{
		PlatformSelectDialog dlg;
		String mediumType = null;
		if ( transportMedium != null )
			mediumType = transportMedium.getMediumType();
		dlg = new PlatformSelectDialog(selectedSite, mediumType);
		dlg.setMultipleSelection(false);
		launchDialog(dlg);
		Platform plat = dlg.getSelectedPlatform();
		if (plat == null)
			return;
		selectedPlatform = plat;
		setTransportMediumInfo();
	}
	private void setTransportMediumInfo()
	{
		try { selectedPlatform.read(); }
		catch(DatabaseException ex)
		{
			showError("Cannot read platform data: " + ex);
			return;
		}
		selectedSite = selectedPlatform.getSite();
		siteField.setText(selectedSite.getDisplayName());
		platformField.setText(selectedPlatform.makeFileName());
		fillScriptCombo();
		String mt = (String)mediumTypeCombo.getSelectedItem();
		TransportMedium tm = selectedPlatform.getTransportMedium(mt);
		if (tm != null) {
			transportMedium = tm;
			scriptCombo.setSelectedItem(tm.scriptName);
			scanFile();
		}
	}

	/** Called when this panel is activated. */
	public void activate()
	{
	}

	/** 
	 * Called when this panel is de-activated. 
	 * @return true if a valid file has been loaded and associated.
	 */
	public boolean deactivate()
	{
		return true;
	}

	String getSelectedFormat()
	{
		return (String)decodeFormatCombo.getSelectedItem();
	}

	private void debugLevelComboSelected()
	{
		int lev = debugLevelCombo.getSelectedIndex();
		Logger.instance().setMinLogPriority(
			lev == 0 ? Logger.E_INFORMATION :
			lev == 1 ? Logger.E_DEBUG1 :
			lev == 2 ? Logger.E_DEBUG2 : Logger.E_DEBUG3);
	}
	private void fillPresentationCombo()
	{
		pgCombo.removeAllItems();
		for(Iterator it=Database.getDb().presentationGroupList.iterator();
					 it.hasNext(); ) {
			PresentationGroup pg = (PresentationGroup)it.next();
			if ( pg.getDisplayName().equalsIgnoreCase("default") ) {
				pgCombo.addItem(pg.getDisplayName());
				pgCombo.setSelectedItem(pg);
			}
		}
		for(Iterator it=Database.getDb().presentationGroupList.iterator();
					 it.hasNext(); ) {
			PresentationGroup pg = (PresentationGroup)it.next();
			if ( !pg.getDisplayName().equalsIgnoreCase("default") )
				pgCombo.addItem(pg.getDisplayName());
		}
	}
	private void fillScriptCombo()
	{
		scriptCombo.removeAllItems();
		PlatformConfig pc = selectedPlatform.getConfig();
		for(Iterator it = pc.getScripts(); it.hasNext(); )
			scriptCombo.addItem(((DecodesScript)it.next()).scriptName);
	}

	public String getRawData()
	{
		return rawDataArea.getText();
	}

	public String getSiteDisplayName()
	{
		return siteField.getText();
	}

	public Site getSelectedSite()
	{
		return selectedSite;
	}

	public Platform getSelectedPlatform()
	{
		return selectedPlatform;
	}
	public TransportMedium getTransportMedium()
	{
		return transportMedium;
	}

	public DecodesScript getSelectedScript()
	{
		if (selectedPlatform == null)
			return null;
		String scriptName = (String)scriptCombo.getSelectedItem();
		PlatformConfig pc = selectedPlatform.getConfig();
		return pc.getScript(scriptName);
	}
	public PresentationGroup getPresentationGroup() {

		String pgName = (String)pgCombo.getSelectedItem();
		PresentationGroup pg = Database.getDb().presentationGroupList.find(pgName);
		return(pg);
	}
}
