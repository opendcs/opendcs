package lrgs.rtstat;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ResourceBundle;

import javax.swing.*;

import ilex.util.EnvExpander;
import ilex.util.LoadResourceBundle;
import ilex.util.Logger;
import ilex.util.AuthException;
import lrgs.gui.DecodesInterface;
import lrgs.nledit.NetlistEditFrame;
import decodes.gui.GuiDialog;
import decodes.util.DecodesException;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.NetworkListSpec;

public class NetlistMaintenanceDialog
	extends GuiDialog
{
	private static ResourceBundle labels = 
		RtStat.getLabels();
	private static ResourceBundle genericLabels = 
		RtStat.getGenericLabels();
	private JPanel mainPanel = new JPanel();
	private BorderLayout mainBorderLayout = new BorderLayout();
	private JPanel southButtonPanel = new JPanel();
	private FlowLayout flowLayout1 = new FlowLayout();
	private JButton okButton = new JButton();
	private JPanel centerPanel = new JPanel();
	private GridBagLayout gridBagLayout1 = new GridBagLayout();
	private JPanel serverListPanel = new JPanel();
	private JPanel serverActionPanel = new JPanel();
	private JButton refreshButton = new JButton();
	private GridBagLayout gridBagLayout2 = new GridBagLayout();
	private JButton retrieveButton = new JButton();
	private JButton installButton = new JButton();
	private BorderLayout serverListLayout = new BorderLayout();
	private JLabel serverListPanelHeader = new JLabel();
	private JPanel serverListSouthPanel = new JPanel();
	private JButton deleteFromServerButton = new JButton();
	private FlowLayout serverListSouthLayout = new FlowLayout();
	private JLabel emptyNorthLabel = new JLabel();
	private JScrollPane serverListScrollPane = new JScrollPane();
	private DefaultListModel serverListModel = new DefaultListModel();
	private JList serverList = new JList(serverListModel);
	private JPanel localListPanel = new JPanel();
	private JPanel localListButtonPanel = new JPanel();
	private JButton newLocalButton = new JButton();
	private GridBagLayout gridBagLayout3 = new GridBagLayout();
	private JButton editLocalButton = new JButton();
	private JLabel jLabel1 = new JLabel();
	private BorderLayout localListLayout = new BorderLayout();
	private JLabel localListHeader = new JLabel();
	private JPanel localListSouthPanel = new JPanel();
	private JButton deleteLocalCopyButton = new JButton();
	private FlowLayout localListSouthLayout = new FlowLayout();
	private JScrollPane localListScrollPane = new JScrollPane();
	private DefaultListModel localListModel = new DefaultListModel();
	private JList localList = new JList(localListModel);

	private DdsClientIf clientIf = null;
	private File localNlDir = null;
	private boolean dbOpenTried = false;
	private ArrayList<NetworkListSpec> netlistSpecs = null;

	/**
	 * Constructor.
	 * @param owner the owner
	 */
	public NetlistMaintenanceDialog(Frame owner)
	{
		this(owner, labels.getString("NetlistMaintDialog.title"), false);
	}

	private NetlistMaintenanceDialog(Frame owner, String title, boolean modal)
	{
		super(owner, title, modal);
		try
		{
			setDefaultCloseOperation(DISPOSE_ON_CLOSE);
			jbInit();
			mainPanel.setPreferredSize(new Dimension(650, 400));
			pack();
		}
		catch (Exception exception)
		{
			exception.printStackTrace();
		}
	}

	/**
	 * Called from RtStatFrame prior to launching the dialog, this method
	 * populates the lists.
	 */
	public void startDialog(DdsClientIf clientIf, String nll[])
	{
		this.clientIf = clientIf;
		serverListModel.clear();
		if (nll != null)
		{
			Arrays.sort(nll);
			for(int i=0; i<nll.length; i++)
				serverListModel.add(i, nll[i]);
		}
		this.setTitle(labels.getString("NetlistMaintDialog.title")
			+ ": " + clientIf.getServerHost());

		refreshLocal();
	}

	private void jbInit()
		throws Exception
	{
		mainPanel.setLayout(mainBorderLayout);
		this.setTitle(labels.getString("NetlistMaintDialog.title"));
		southButtonPanel.setLayout(flowLayout1);
		flowLayout1.setHgap(15);
		flowLayout1.setVgap(10);
		okButton.setPreferredSize(new Dimension(100, 23));
		okButton.setText(genericLabels.getString("OK"));
		okButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				okButton_actionPerformed(e);
			}
		});
		centerPanel.setLayout(gridBagLayout1);
		refreshButton.setPreferredSize(new Dimension(140, 23));
		refreshButton.setText(labels.getString(
				"NetlistMaintDialog.refresh"));
		refreshButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				refreshButton_actionPerformed();
			}
		});
		serverActionPanel.setLayout(gridBagLayout2);
		retrieveButton.setPreferredSize(new Dimension(140, 23));
		retrieveButton.setText(labels.getString(
				"NetlistMaintDialog.retrieve"));
		retrieveButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				retrieveButton_actionPerformed(e);
			}
		});
		installButton.setPreferredSize(new Dimension(140, 23));
		installButton.setText(labels.getString(
				"NetlistMaintDialog.install"));
		installButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				installButton_actionPerformed(e);
			}
		});
		serverListPanel.setLayout(serverListLayout);
		serverListPanelHeader.setFont(new java.awt.Font("Dialog", Font.BOLD, 11));
		serverListPanelHeader.setHorizontalAlignment(SwingConstants.CENTER);
		serverListPanelHeader.setText(labels.getString(
			"NetlistMaintDialog.networkListServer"));
		deleteFromServerButton.setPreferredSize(new Dimension(180, 23));
		deleteFromServerButton.setText(labels.getString(
				"NetlistMaintDialog.deleteFromServer"));
		deleteFromServerButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				deleteFromServerButton_actionPerformed();
			}
		});
		serverListSouthPanel.setLayout(serverListSouthLayout);
		serverListSouthLayout.setVgap(10);
		emptyNorthLabel.setText("   ");
		serverListScrollPane.setBorder(BorderFactory.createLoweredBevelBorder());
		newLocalButton.setPreferredSize(new Dimension(100, 23));
		newLocalButton.setText(labels.getString(
				"NetlistMaintDialog.newLocal"));
		newLocalButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				newLocalButton_actionPerformed(e);
			}
		});
		localListButtonPanel.setLayout(gridBagLayout3);
		editLocalButton.setPreferredSize(new Dimension(100, 23));
		editLocalButton.setText(labels.getString(
				"NetlistMaintDialog.editLocal"));
		editLocalButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				editLocalButton_actionPerformed(e);
			}
		});
		jLabel1.setText("jLabel1");
		localListPanel.setLayout(localListLayout);
		localListHeader.setFont(new java.awt.Font("Dialog", Font.BOLD, 11));
		localListHeader.setHorizontalAlignment(SwingConstants.CENTER);
		localListHeader.setText(labels.getString(
				"NetlistMaintDialog.localNetworkLists"));
		deleteLocalCopyButton.setPreferredSize(new Dimension(180, 23));
		deleteLocalCopyButton.setText(labels.getString(
				"NetlistMaintDialog.delLocalCopy"));
		deleteLocalCopyButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				deleteLocalCopyButton_actionPerformed(e);
			}
		});
		localListSouthPanel.setLayout(localListSouthLayout);
		localListSouthLayout.setVgap(10);
		localListScrollPane.setBorder(BorderFactory.createLoweredBevelBorder());
		getContentPane().add(mainPanel);
		mainPanel.add(southButtonPanel, java.awt.BorderLayout.SOUTH);
		southButtonPanel.add(okButton);
		mainPanel.add(centerPanel, java.awt.BorderLayout.CENTER);
		serverListPanel.add(serverListSouthPanel, java.awt.BorderLayout.SOUTH);
		serverListSouthPanel.add(deleteFromServerButton);
		mainPanel.add(emptyNorthLabel, java.awt.BorderLayout.NORTH);
		serverListPanel.add(serverListPanelHeader, java.awt.BorderLayout.NORTH);
		serverListPanel.add(serverListScrollPane, java.awt.BorderLayout.CENTER);
		serverListScrollPane.getViewport().add(serverList);
		centerPanel.add(serverListPanel,
						new GridBagConstraints(0, 1, 1, 1, 0.5, 1.0
											   , GridBagConstraints.CENTER,
											   GridBagConstraints.BOTH,
											   new Insets(0, 0, 0, 0), 0, 0));
		centerPanel.add(localListPanel,
						new GridBagConstraints(2, 1, 1, 1, 0.5, 1.0
											   , GridBagConstraints.CENTER,
											   GridBagConstraints.BOTH,
											   new Insets(0, 0, 0, 0), 0, 0));
		localListPanel.add(localListHeader, java.awt.BorderLayout.NORTH);
		localListPanel.add(localListSouthPanel, java.awt.BorderLayout.SOUTH);
		localListSouthPanel.add(deleteLocalCopyButton);
		localListPanel.add(localListScrollPane, java.awt.BorderLayout.CENTER);
		localListScrollPane.getViewport().add(localList);
		centerPanel.add(localListButtonPanel,
						new GridBagConstraints(3, 1, 1, 1, 0.0, 1.0
											   , GridBagConstraints.CENTER,
											   GridBagConstraints.BOTH,
											   new Insets(0, 0, 0, 0), 0, 0));
		centerPanel.add(serverActionPanel,
						new GridBagConstraints(1, 1, 1, 1, 0.0, 1.0
											   , GridBagConstraints.CENTER,
											   GridBagConstraints.BOTH,
											   new Insets(0, 0, 0, 0), 0, 0));
		serverActionPanel.add(refreshButton,
							  new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
			, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
			new Insets(40, 10, 10, 10), 0, 0));
		serverActionPanel.add(retrieveButton,
							  new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
			, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
			new Insets(10, 10, 10, 10), 0, 0));
		serverActionPanel.add(installButton,
							  new GridBagConstraints(0, 2, 1, 1, 0.0, 1.0
			, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
			new Insets(10, 10, 10, 10), 0, 0));
		localListButtonPanel.add(newLocalButton,
								 new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
			, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
			new Insets(40, 10, 10, 10), 0, 0));
		localListButtonPanel.add(editLocalButton,
								 new GridBagConstraints(0, 1, 1, 1, 0.0, 1.0
			, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
			new Insets(10, 10, 10, 10), 0, 0));
	}

	private void okButton_actionPerformed(ActionEvent e)
	{
		setVisible(false);
	}

	private void refreshButton_actionPerformed()
	{
		try
		{
			String nll[] = clientIf.getNetlistList();
			serverListModel.clear();
			if (nll != null)
			{
				Arrays.sort(nll);
				for(int i=0; i<nll.length; i++)
					serverListModel.add(i, nll[i]);
			}
		}
		catch(AuthException ex)
		{
			showError(labels.getString(
					"NetlistMaintDialog.cantGetNetlistFromServ") + ex);
		}
	}

	private void retrieveButton_actionPerformed(ActionEvent e)
	{
		String listname = "";
		try
		{
			int n = serverListModel.size();
			for(int i=0; i<n; i++)
				if (serverList.isSelectedIndex(i))
				{
					listname = (String)serverListModel.get(i);
					byte data[] = clientIf.getNetlist(listname);
					File nl = new File(localNlDir, listname);
					try
					{
						FileOutputStream fos = new FileOutputStream(nl);
						fos.write(data);
						fos.close();
					}
					catch(IOException ex)
					{
						showError(LoadResourceBundle.sprintf(
								labels.getString(
								"NetlistMaintDialog.errorWritingLf"),
								nl.getPath()) + ex);
					}
				}
		}
		catch(AuthException ex)
		{
			showError(LoadResourceBundle.sprintf(
					labels.getString(
					"NetlistMaintDialog.errorRetrievingL"),
					listname) + ex);
		}
		refreshLocal();
	}

	private void installButton_actionPerformed(ActionEvent e)
	{
		String listname = "";
		try
		{
			int n = localListModel.size();
			for(int i=0; i<n; i++)
				if (localList.isSelectedIndex(i))
					installList((String)localListModel.get(i));
		}
		catch(AuthException ex)
		{
			showError(LoadResourceBundle.sprintf(
					labels.getString(
					"NetlistMaintDialog.installingListErr"),
					listname) + ex);
		}
		refreshButton_actionPerformed();
	}
	
	private void installList(String listname)
		throws AuthException
	{
		int paren = listname.lastIndexOf('(');
		boolean isDecodes = paren > 0
			&& "(DECODES)".equalsIgnoreCase(listname.substring(paren));
		if (paren>0)
			listname = listname.substring(0, paren-1);
		if (isDecodes)
		{
			for(NetworkListSpec nls : this.netlistSpecs)
				if (listname.equals(nls.getName()))
				{
					decodes.db.NetworkList nl = 
						new decodes.db.NetworkList(nls.getName(), 
							nls.getTmType());
					try
					{
						nl.read();
						nl.prepareForExec();
						String ds = nl.legacyNetworkList.toFileString();
						clientIf.installNetlist(listname, ds.getBytes());
					}
					catch(Exception ex)
					{
						Logger.instance().warning(
							"Cannot read DECODES netlist '"
							+ listname + "': " + ex);
					}
					break;
				}
		}
		else
		{
			File nl = new File(localNlDir, listname);
			try
			{
				FileInputStream fis = new FileInputStream(nl);
				byte data[] = new byte[(int)nl.length()];
				fis.read(data);
				fis.close();
				clientIf.installNetlist(listname, data);
			}
			catch(IOException ex)
			{
				showError(LoadResourceBundle.sprintf(
						labels.getString(
						"NetlistMaintDialog.readingFileErr"),
						nl.getPath()) + ex);
			}
		}
	}

	private void deleteFromServerButton_actionPerformed()
	{
		try
		{
			int n = serverListModel.size();
			for(int i=0; i<n; i++)
				if (serverList.isSelectedIndex(i))
				{
					String listname = (String)serverListModel.get(i);
					clientIf.deleteNetlist(listname);
				}
		}
		catch(AuthException ex)
		{
			showError(labels.getString(
					"NetlistMaintDialog.delListErr") + ex);
		}
		refreshButton_actionPerformed();
	}

	private void newLocalButton_actionPerformed(ActionEvent e)
	{
		NetlistEditFrame.netlistDir = localNlDir.getPath();
		NetlistEditFrame editor = new NetlistEditFrame();
		editor.setStandAlone(false);
		centerFrame(editor);
		editor.setVisible(true);
	}

	private void editLocalButton_actionPerformed(ActionEvent e)
	{
		String listname = (String)localList.getSelectedValue();
		if (listname == null)
			return;
		int paren = listname.lastIndexOf('(');
		boolean isDecodes = paren > 0
			&& "(DECODES)".equalsIgnoreCase(listname.substring(paren));
		if (paren>0)
			listname = listname.substring(0, paren-1);
		if (isDecodes)
		{
			showError("Cannot edit a DECODES list from this dialog."
				+ " Use the DECODES Database Editor.");
			return;
		}
		NetlistEditFrame.netlistDir = localNlDir.getPath();
		NetlistEditFrame editor = new NetlistEditFrame();
		editor.setStandAlone(false);
		String path = localNlDir.getPath() + File.separator + listname;
		editor.openFile(path);
		centerFrame(editor);
		editor.setVisible(true);
	}

	private void deleteLocalCopyButton_actionPerformed(ActionEvent e)
	{
		int n = localListModel.size();
		for(int i=0; i<n; i++)
			if (localList.isSelectedIndex(i))
			{
				String listname = (String)localListModel.get(i);
				int paren = listname.lastIndexOf('(');
				boolean isDecodes = paren > 0
					&& "(DECODES)".equalsIgnoreCase(listname.substring(paren));
				if (paren>0)
					listname = listname.substring(0, paren-1);
				if (isDecodes)
				{
					showError("Cannot delete a DECODES list from this menu."
						+ " Use the DECODES Database Editor.");
				}
				else
				{
					File fl = new File(localNlDir, listname);
					if (!fl.delete())
						System.out.println("Cannot delete '" + listname
							+ "' in directory '" + localNlDir + "'");
				}
			}
		refreshLocal();
	}

	private void refreshLocal()
	{
		localNlDir = new File(EnvExpander.expand("$LRGSHOME/netlist"));
		if (!localNlDir.isDirectory())
		{
			localNlDir = new File(EnvExpander.expand("$HOME/netlist"));
			if (!localNlDir.isDirectory())
			{
				localNlDir = new File(EnvExpander.expand(
					"$DECODES_INSTALL_DIR/netlist"));
				if (!localNlDir.isDirectory())
					localNlDir = new File(System.getProperty("user.dir"));
			}
		}
		Logger.instance().info("Reading network lists from '"
			+ localNlDir.getPath() + "'");

		localListModel.clear();
		File files[] = localNlDir.listFiles();
		Arrays.sort(files);
		for(int i=0; i<files.length; i++)
			if (files[i].isFile() && files[i].canRead())
				localListModel.addElement(files[i].getName() + " (File)");
		
		Database db = Database.getDb();
		if (db == null && !dbOpenTried)
		{
			dbOpenTried = true;
			try
			{
				DecodesInterface.initDecodes(null);
				db = Database.getDb();
			}
			catch(Exception ex)
			{
				Logger.instance().info("Cannot open DECODES database: "+ex);
				db = null;
				netlistSpecs = null;
			}
		}
		if (db != null)
		{
			try
			{
				netlistSpecs = db.getDbIo().getNetlistSpecs();
				for(NetworkListSpec nls : netlistSpecs)
					localListModel.addElement(nls.getName() + " (DECODES)");
			}
			catch(Exception ex)
			{
				Logger.instance().info("Cannot list DECODES network lists: "
					+ex);
				db = null;
				netlistSpecs = null;
			}
		}
	}

	private void centerFrame(JFrame frame)
	{
        //Center the window
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = frame.getSize();
        if (frameSize.height > screenSize.height) {
            frameSize.height = screenSize.height;
        }
        if (frameSize.width > screenSize.width) {
            frameSize.width = screenSize.width;
        }
        frame.setLocation(
			(screenSize.width - frameSize.width) / 2,
			(screenSize.height - frameSize.height) / 2);
	}
}
