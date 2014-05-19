package decodes.dbeditor;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.StringTokenizer;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JFileChooser;
import java.util.ResourceBundle;

import ilex.util.LoadResourceBundle;

import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.xml.XmlOutputStream;
import ilex.xml.XmlObjectWriter;

import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.NetworkList;
import decodes.db.NetworkListEntry;
import decodes.db.NetworkListList;
import decodes.db.Platform;
import decodes.db.PlatformList;
import decodes.gui.TopFrame;
import decodes.gui.GuiDialog;
import decodes.xml.DatabaseParser;
import decodes.xml.PlatformParser;
import decodes.xml.XmlDbTags;


/**
This class implements the dialog displayed for File - Export.
*/
public class ExportDialog extends GuiDialog
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

	private JPanel panel1 = new JPanel();
	private BorderLayout borderLayout1 = new BorderLayout();
	private JPanel southButtonPanel = new JPanel();
	private JButton quitButton = new JButton();
	private FlowLayout flowLayout1 = new FlowLayout();
	private JPanel centerPanel = new JPanel();
	private JPanel selectionPanel = new JPanel();
	private BorderLayout borderLayout2 = new BorderLayout();
	private JPanel exportButtonPanel = new JPanel();
	private FlowLayout flowLayout2 = new FlowLayout();
	private JButton exportButton = new JButton();
	private JPanel selectionPanel2 = new JPanel();
	private BorderLayout borderLayout3 = new BorderLayout();
	private JPanel fileSelectPanel = new JPanel();
	private JLabel outputFileLabel = new JLabel();
	private JTextField outputFileField = new JTextField();
	private JButton chooseButton = new JButton();
	private JPanel jPanel1 = new JPanel();
	private TitledBorder titledBorder1 = new TitledBorder("");
	private Border border1 = BorderFactory.createEtchedBorder(Color.white,
		new Color(165, 163, 151));
	private Border border2 = new TitledBorder(border1, 
		dbeditLabels.getString("ExportDialog.what"));
	private GridLayout gridLayout1 = new GridLayout();
	private JRadioButton entireDbRadio = new JRadioButton();
	private ButtonGroup radioButtonGroup = new ButtonGroup();
	private JRadioButton allPlatformsRadio = new JRadioButton();
	private JPanel platByNetlistPanel = new JPanel();
	private FlowLayout flowLayout4 = new FlowLayout();
	private JRadioButton platformsInNetlistRadio = new JRadioButton();
	private JComboBox netlistCombo = new JComboBox();
	private JPanel platByNamePanel = new JPanel();
	private JRadioButton platformsByNameRadio = new JRadioButton();
	private JTextField platformNameField = new JTextField();
	private TitledBorder titledBorder2 = new TitledBorder("");
	private Border border3 = BorderFactory.createEtchedBorder(Color.white,
		new Color(165, 163, 151));
	private Border border4 = new TitledBorder(border3, 
		dbeditLabels.getString("ExportDialog.where"));
	private BorderLayout borderLayout4 = new BorderLayout();
	private JPanel resultsPanel = new JPanel();
	private BorderLayout borderLayout5 = new BorderLayout();
	private TitledBorder titledBorder3 = new TitledBorder("");
	private Border border5 = BorderFactory.createEtchedBorder(Color.white,
		new Color(165, 163, 151));
	private Border border6 = new TitledBorder(border5, 
		dbeditLabels.getString("ExportDialog.results"));
	private JScrollPane resultsScrollPane = new JScrollPane();
	private JTextArea resultsArea = new JTextArea();
	private GridBagLayout gridBagLayout1 = new GridBagLayout();
	private GridBagLayout gridBagLayout2 = new GridBagLayout();
	private JButton selectPlatformButton = new JButton();

	static private JFileChooser fileChooser = new JFileChooser(
		EnvExpander.expand("$DCSTOOL_USERDIR"));

	/**
	 * Constructor.
	 * @param owner the db editor top-frame.
	 * @param title
	 */
	public ExportDialog()
	{
		super(getDbEditFrame(), 
			dbeditLabels.getString("ExportDialog.title"), true);
		try
		{
			setDefaultCloseOperation(DISPOSE_ON_CLOSE);
			jbInit();
			pack();
		}
		catch (Exception exception)
		{
			exception.printStackTrace();
		}
		entireDbRadio.setSelected(true);
		allPlatformsRadio.setSelected(false);
		platformsInNetlistRadio.setSelected(false);
		platformsByNameRadio.setSelected(false);
		radioButtonGroup.add(entireDbRadio);
		radioButtonGroup.add(allPlatformsRadio);
		radioButtonGroup.add(platformsInNetlistRadio);
		radioButtonGroup.add(platformsByNameRadio);
		for(Iterator it = Database.getDb().networkListList.iterator();
			it.hasNext(); )
		{
			NetworkList nl = (NetworkList)it.next();
			netlistCombo.addItem(nl.name);
		}
	}

	private void jbInit() throws Exception
	{
		panel1.setLayout(borderLayout1);
		quitButton.setPreferredSize(new Dimension(100, 27));
		quitButton.setText(genericLabels.getString("quit"));
		quitButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				quitButton_actionPerformed(e);
			}
		});
		southButtonPanel.setLayout(flowLayout1);
		flowLayout1.setHgap(20);
		centerPanel.setLayout(borderLayout4);
		selectionPanel.setLayout(borderLayout2);
		exportButtonPanel.setLayout(flowLayout2);
		exportButton.setEnabled(true);
		exportButton.setPreferredSize(new Dimension(100, 27));
		exportButton.setText(genericLabels.getString("export"));
		exportButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				exportButton_actionPerformed(e);
			}
		});
		selectionPanel2.setLayout(borderLayout3);
		fileSelectPanel.setLayout(gridBagLayout2);
		outputFileLabel.setText(
			dbeditLabels.getString("ExportDialog.output"));
		outputFileField.setPreferredSize(new Dimension(260, 23));
		outputFileField.setText("");
		outputFileField.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				outputFileField_actionPerformed(e);
			}
		});
		chooseButton.setPreferredSize(new Dimension(100, 27));
		chooseButton.setText(genericLabels.getString("choose"));
		chooseButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				chooseButton_actionPerformed(e);
			}
		});
		jPanel1.setBorder(border2);
		jPanel1.setLayout(gridLayout1);
		gridLayout1.setColumns(1);
		gridLayout1.setRows(4);
		entireDbRadio.setText(
			dbeditLabels.getString("ExportDialog.entireDb"));
		allPlatformsRadio.setText(
			dbeditLabels.getString("ExportDialog.allPlat"));
		allPlatformsRadio.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				allPlatformsRadio_actionPerformed(e);
			}
		});
		platByNetlistPanel.setLayout(flowLayout4);
		flowLayout4.setAlignment(FlowLayout.LEFT);
		flowLayout4.setHgap(0);
		platformsInNetlistRadio.setText(
			dbeditLabels.getString("ExportDialog.netlist"));
		platformsInNetlistRadio.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				platformsInNetlistRadio_actionPerformed(e);
			}
		});
		netlistCombo.setEnabled(false);
		netlistCombo.setPreferredSize(new Dimension(150, 22));
		platformsByNameRadio.setText(
			dbeditLabels.getString("ExportDialog.platByName"));
		platformsByNameRadio.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				platformsByNameRadio_actionPerformed(e);
			}
		});
		platByNamePanel.setLayout(gridBagLayout1);
		platformNameField.setEnabled(false);
		platformNameField.setPreferredSize(new Dimension(250, 23));
		platformNameField.setText("");
		fileSelectPanel.setBorder(border4);
		resultsPanel.setLayout(borderLayout5);
		resultsPanel.setBorder(border6);
		resultsScrollPane.setPreferredSize(new Dimension(0, 120));
		resultsArea.setEditable(false);
		resultsArea.setText("");
		resultsArea.setLineWrap(true);
		resultsArea.setWrapStyleWord(true);
		this.setTitle(
			dbeditLabels.getString("ExportDialog.platByName"));
		entireDbRadio.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				entireDbRadio_actionPerformed(e);
			}
		});
//		selectPlatformButton.setPreferredSize(new Dimension(100, 27));
		selectPlatformButton.setText(genericLabels.getString("select"));
		selectPlatformButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				selectPlatformButton_actionPerformed(e);
			}
		});
		exportButtonPanel.setPreferredSize(new Dimension(110, 50));
//		resultsScrollPane.setVerticalScrollBarPolicy(JScrollPane.
//			VERTICAL_SCROLLBAR_ALWAYS);
//		resultsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.
//			HORIZONTAL_SCROLLBAR_NEVER);
		getContentPane().add(panel1);
		panel1.add(southButtonPanel, java.awt.BorderLayout.SOUTH);
		southButtonPanel.add(quitButton);
		panel1.add(centerPanel, java.awt.BorderLayout.CENTER);
		selectionPanel.add(exportButtonPanel, java.awt.BorderLayout.SOUTH);
		exportButtonPanel.add(exportButton);
		selectionPanel.add(selectionPanel2, java.awt.BorderLayout.CENTER);
		selectionPanel2.add(fileSelectPanel, java.awt.BorderLayout.SOUTH);
		selectionPanel2.add(jPanel1, java.awt.BorderLayout.CENTER);
		jPanel1.add(entireDbRadio);
		jPanel1.add(allPlatformsRadio);
		jPanel1.add(platByNetlistPanel);
		jPanel1.add(platByNamePanel);
		centerPanel.add(resultsPanel, java.awt.BorderLayout.CENTER);
		resultsPanel.add(resultsScrollPane, java.awt.BorderLayout.CENTER);
		resultsScrollPane.getViewport().add(resultsArea);
		platByNetlistPanel.add(platformsInNetlistRadio);
		platByNetlistPanel.add(netlistCombo);
		centerPanel.add(selectionPanel, java.awt.BorderLayout.NORTH);
		fileSelectPanel.add(outputFileLabel,
							new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
			, GridBagConstraints.WEST, GridBagConstraints.NONE,
			new Insets(5, 5, 5, 2), 0, 0));
		fileSelectPanel.add(outputFileField,
							new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
			, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
			new Insets(5, 0, 5, 5), 0, 0));
		platByNamePanel.add(platformsByNameRadio,
							new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
			, GridBagConstraints.CENTER, GridBagConstraints.NONE,
			new Insets(5, 0, 5, 0), 0, 0));
		platByNamePanel.add(platformNameField,
							new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
			, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
			new Insets(5, 0, 5, 5), 0, 0));
		platByNamePanel.add(selectPlatformButton,
							new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0
			, GridBagConstraints.CENTER, GridBagConstraints.NONE,
			new Insets(5, 5, 5, 5), 0, 0));
		fileSelectPanel.add(chooseButton,
							new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0
			, GridBagConstraints.WEST, GridBagConstraints.NONE,
			new Insets(5, 5, 5, 5), 0, 0));
	}

	public void quitButton_actionPerformed(ActionEvent e)
	{
		closeDlg();
	}

	public void exportButton_actionPerformed(ActionEvent e)
	{
		String fn = outputFileField.getText().trim();
		if (fn.length() == 0)
		{
			showError(
				dbeditLabels.getString("ExportDialog.nameBeforeExport"));
			return;
		}
		File f = new File(fn);
		if (f.exists())
		{
			int r =  JOptionPane.showConfirmDialog(this, 
				LoadResourceBundle.sprintf(
					genericLabels.getString("overwriteConfirm"), fn));
			if (r != JOptionPane.YES_OPTION)
				return;
		}
		FileOutputStream fos = null;
		try
		{
			resultsArea.setText("");
			fos = new FileOutputStream(f);
			XmlOutputStream xos = new XmlOutputStream(fos, 
				XmlDbTags.Database_el);
			xos.writeXmlHeader();
			if (entireDbRadio.isSelected())
			{
				for(Iterator it = Database.getDb().platformList.iterator();
					it.hasNext(); )
				{
					Platform p = (Platform)it.next();
					result(
						LoadResourceBundle.sprintf(
							dbeditLabels.getString("ExportDialog.readingPlat"),
								p.makeFileName()));
					try { p.read(); }
					catch(DatabaseException ex)
					{
						Logger.instance().warning(
							"Export Cannot read platform '" 
							+ p.makeFileName() + "' -- skipped.");
						continue;
					}
				}
				result(
					LoadResourceBundle.sprintf(
						dbeditLabels.getString("ExportDialog.savingDb"), fn));
				XmlObjectWriter xow = new DatabaseParser(Database.getDb());
				xow.writeXml(xos);
				result(genericLabels.getString("done"));
				result(dbeditLabels.getString("ExportDialog.toContinue"));
				return;
			}
			// Else write the <Database> envelope manually.
			xos.startElement(XmlDbTags.Database_el);

			if (allPlatformsRadio.isSelected())
			{
				result(
					LoadResourceBundle.sprintf(
						dbeditLabels.getString("ExportDialog.savingAllPlat"), fn));
				for(Iterator it = Database.getDb().platformList.iterator();
					it.hasNext(); )
				{
					Platform p = (Platform)it.next();
					try { p.read(); }
					catch(DatabaseException ex)
					{
						Logger.instance().warning(
							"Export Cannot read platform '" + p.makeFileName() 
							+ "' -- skipped.");
						continue;
					}
					PlatformParser pp = new PlatformParser(p);
					result(
						LoadResourceBundle.sprintf(
							dbeditLabels.getString("ExportDialog.savingPlat"),
								p.makeFileName()));
					pp.writeXml(xos);
				}
				result(genericLabels.getString("done"));
			}
			else if (platformsInNetlistRadio.isSelected())
			{
				String nlname = (String)netlistCombo.getSelectedItem();
				NetworkList nl = 
					Database.getDb().networkListList.getNetworkList(nlname);
				PlatformList pl = Database.getDb().platformList;
				for(Iterator it = nl.iterator(); it.hasNext(); )
				{
					NetworkListEntry nle = (NetworkListEntry)it.next();
					Platform p = null;
					try 
					{
						p = pl.getPlatform(nl.transportMediumType,
							nle.transportId, new Date());
						p.read();
					}
					catch(DatabaseException ex) { p = null; }
					if (p == null)
						result(
							LoadResourceBundle.sprintf(
								dbeditLabels.getString("ExportDialog.noMatchingPlat"),
									nl.transportMediumType, nle.transportId));
					else
					{
						PlatformParser pp = new PlatformParser(p);
						result(
							LoadResourceBundle.sprintf(
								dbeditLabels.getString("ExportDialog.savingPlat"),
									p.makeFileName()));
						pp.writeXml(xos);
					}
				}
			}
			else if (platformsByNameRadio.isSelected())
			{
				StringTokenizer st = new StringTokenizer(
					platformNameField.getText().trim());
				PlatformList pl = Database.getDb().platformList;
				while(st.hasMoreTokens())
				{
					String t = st.nextToken();
					for(Iterator it = pl.iterator(); it.hasNext(); )
					{
						Platform p = (Platform)it.next();
						if (p.makeFileName().equals(t))
						{
							result(
								LoadResourceBundle.sprintf(
									dbeditLabels.getString("ExportDialog.savingPlat"),
										p.makeFileName()));
							try { p.read(); }
							catch(DatabaseException ex)
							{
								Logger.instance().warning(
									"Export Cannot read platform '" 
									+ p.makeFileName() + "' -- skipped.");
								break;
							}
							PlatformParser pp = new PlatformParser(p);
							pp.writeXml(xos);
							break;
						}
					}
				}
			}
			xos.endElement(XmlDbTags.Database_el);
		}
		catch(IOException ex)
		{
			result(
				dbeditLabels.getString("ExportDialog.ioError") + ex);
		}
		finally
		{
			if (fos != null)
				try { fos.close(); }
				catch(Exception ex) {}
		}
	}

	public void chooseButton_actionPerformed(ActionEvent e)
	{
		if (fileChooser.showSaveDialog(this)
			== fileChooser.APPROVE_OPTION)
		{
			File f = fileChooser.getSelectedFile();
			outputFileField.setText(f.getPath());
			outputFileField_actionPerformed(null);
		}
	}

	public void outputFileField_actionPerformed(ActionEvent e)
	{
		if (outputFileField.getText().trim().length() > 0)
		{
			exportButton.setEnabled(true);
		}

	}

	public static void main(String args[])
	{
		ExportDialog dlg = new ExportDialog();
		dlg.setVisible(true);
	}

	public void entireDbRadio_actionPerformed(ActionEvent e)
	{
		netlistCombo.setEnabled(false);
		platformNameField.setEnabled(false);
	}

	public void allPlatformsRadio_actionPerformed(ActionEvent e)
	{
		netlistCombo.setEnabled(false);
		platformNameField.setEnabled(false);
	}

	public void platformsInNetlistRadio_actionPerformed(ActionEvent e)
	{
		netlistCombo.setEnabled(true);
		platformNameField.setEnabled(false);
	}

	public void platformsByNameRadio_actionPerformed(ActionEvent e)
	{
		netlistCombo.setEnabled(false);
		platformNameField.setEnabled(true);
	}

	public void selectPlatformButton_actionPerformed(ActionEvent e)
	{
		PlatformSelectDialog dlg = new PlatformSelectDialog(this, null);
		dlg.setMultipleSelection(true);
		launchDialog(dlg);
		Platform ps[] = dlg.getSelectedPlatforms();
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<ps.length; i++)
			sb.append(ps[i].makeFileName() + " ");
		platformNameField.setText(sb.toString());
		platformsByNameRadio.setSelected(true);
		platformsByNameRadio_actionPerformed(null);
	}

	private void closeDlg()
	{
		setVisible(false);
		dispose();
	}

	private void result(String x)
	{
		resultsArea.append(x + "\n");
		resultsArea.setCaretPosition(resultsArea.getText().length());
	}
}
