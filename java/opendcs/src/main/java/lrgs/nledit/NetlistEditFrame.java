/*
*  $Id$
*/
package lrgs.nledit;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;

import decodes.util.ResourceFactory;

import ilex.util.AsciiUtil;

public class NetlistEditFrame extends JFrame {
	private static ResourceBundle labels = 
						NetlistEditor.getLabels();
	private static ResourceBundle genericLabels = 
						NetlistEditor.getGenericLabels();
    JPanel contentPane;
    JMenuBar jMenuBar1 = new JMenuBar();
    JMenu jMenuFile = new JMenu();
    JMenuItem jMenuFileExit = new JMenuItem();
    JMenu jMenuHelp = new JMenu();
    JMenuItem jMenuHelpAbout = new JMenuItem();
    JToolBar jToolBar = new JToolBar();
    JButton saveButton = new JButton();
    ImageIcon imageOpen;
    ImageIcon imageSave;
    ImageIcon imageInsertBefore;
    ImageIcon imageInsertAfter;
    JLabel statusBar = new JLabel();
    BorderLayout borderLayout1 = new BorderLayout();
    JMenuItem jMenuFileOpen = new JMenuItem();
    JMenuItem jMenuFileSave = new JMenuItem();
    JMenuItem jMenuFileNew = new JMenuItem();
    JMenuItem jMenuFileSaveAs = new JMenuItem();
    JMenu jMenu1 = new JMenu();
    JMenuItem jMenuEditCut = new JMenuItem();
    JMenuItem jMenuEditCopy = new JMenuItem();
    JMenuItem jMenuEditPaste = new JMenuItem();
    JMenuItem jMenuEditInsertBefore = new JMenuItem();
    JMenuItem jMenuEditInsertAfter = new JMenuItem();
    JFileChooser myFileChooser = new JFileChooser();
	String curFileName = null;
    ImageIcon imageCopy;
    ImageIcon imageCut;
    ImageIcon imagePaste;
    ImageIcon imageNew;
    JButton newButton = new JButton();
    JButton openButton = new JButton();
    JButton cutButton = new JButton();
    JButton copyButton = new JButton();
    JButton pasteButton = new JButton();
    JPanel jPanel1 = new JPanel();
    BorderLayout borderLayout2 = new BorderLayout();
    JScrollPane tableScrollPane = new JScrollPane();
	NetworkListTable networkListTable = new NetworkListTable();
    JMenuItem jMenuFileMerge = new JMenuItem();
    JButton insertBeforeButton = new JButton();
    JButton insertAfterButton = new JButton();
	PasteBuffer pasteBuffer = new PasteBuffer();
	boolean isStandAlone = false;

	// If non-null, editor will default to this dir. Other wise will use
	// user's home dir.
	public static String netlistDir = null;

    /**Construct the frame*/
    public NetlistEditFrame() {
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        try {
            jbInit();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
//		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

    }
    /**Component initialization*/
    private void jbInit() throws Exception  
	{
		if (netlistDir != null)
			myFileChooser.setCurrentDirectory(new File(netlistDir));
		else
			myFileChooser.setCurrentDirectory(
				new File(System.getProperty("user.dir")));
        imageOpen = new ImageIcon(lrgs.nledit.NetlistEditFrame.class.getResource("open.gif"));
        imageSave = new ImageIcon(lrgs.nledit.NetlistEditFrame.class.getResource("save.gif"));
        imageCopy = new ImageIcon(lrgs.nledit.NetlistEditFrame.class.getResource("copy.gif"));
        imageCut = new ImageIcon(lrgs.nledit.NetlistEditFrame.class.getResource("cut.gif"));
        imagePaste = new ImageIcon(lrgs.nledit.NetlistEditFrame.class.getResource("paste.gif"));
        imageNew = new ImageIcon(lrgs.nledit.NetlistEditFrame.class.getResource("new.gif"));
        imageInsertBefore = new ImageIcon(lrgs.nledit.NetlistEditFrame.class.getResource("insertBefore.gif"));
        imageInsertAfter = new ImageIcon(lrgs.nledit.NetlistEditFrame.class.getResource("insertAfter.gif"));
        //setIconImage(Toolkit.getDefaultToolkit().createImage(NetlistEditFrame.class.getResource("[Your Icon]")));
        contentPane = (JPanel) this.getContentPane();
        contentPane.setLayout(borderLayout1);
        this.setSize(new Dimension(543, 331));
        this.setTitle(labels.getString("NetlistEditFrame.frameTitle"));
        statusBar.setText(" ");
        jMenuFile.setText(genericLabels.getString("file"));
        jMenuFileExit.setText(genericLabels.getString("exit"));
        jMenuFileExit.addActionListener(new ActionListener()  {
            public void actionPerformed(ActionEvent e) {
                jMenuFileExit_actionPerformed(e);
            }
        });
        jMenuHelp.setText(genericLabels.getString("help"));
        jMenuHelpAbout.setText(genericLabels.getString("about"));
        jMenuHelpAbout.addActionListener(new ActionListener()  {
            public void actionPerformed(ActionEvent e) {
                jMenuHelpAbout_actionPerformed(e);
            }
        });
        saveButton.setIcon(imageSave);
        saveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuFileSave_actionPerformed(e);
            }
        });
        saveButton.setMaximumSize(new Dimension(25, 25));
        saveButton.setMinimumSize(new Dimension(25, 25));
        saveButton.setToolTipText(
        		labels.getString("NetlistEditFrame.saveFile"));
        jMenuFileOpen.setText(genericLabels.getString("open"));
        jMenuFileOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuFileOpen_actionPerformed(e);
            }
        });
        jMenuFileNew.setText(genericLabels.getString("new"));
        jMenuFileNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuFileNew_actionPerformed(e);
            }
        });
        jMenuFileSave.setText(
        		labels.getString("NetlistEditFrame.save"));
        jMenuFileSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuFileSave_actionPerformed(e);
            }
        });
        jMenuFileSaveAs.setText(
        		labels.getString("NetlistEditFrame.saveAs"));
        jMenuFileSaveAs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuFileSaveAs_actionPerformed(e);
            }
        });
        jMenu1.setText(genericLabels.getString("edit"));
        jMenuEditCut.setText(
        		labels.getString("NetlistEditFrame.cut"));
        jMenuEditCut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuEditCut_actionPerformed(e);
            }
        });
        jMenuEditCopy.setText(genericLabels.getString("copy"));
        jMenuEditCopy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuEditCopy_actionPerformed(e);
            }
        });
        jMenuEditPaste.setText(
        		labels.getString("NetlistEditFrame.paste"));
        jMenuEditPaste.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuEditPaste_actionPerformed(e);
            }
        });
        jMenuEditInsertBefore.setText(
        		labels.getString("NetlistEditFrame.insertBefore"));
        jMenuEditInsertBefore.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuEditInsertBefore_actionPerformed(e);
            }
        });
        jMenuEditInsertAfter.setText(
        		labels.getString("NetlistEditFrame.insertAfter"));
        jMenuEditInsertAfter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuEditInsertAfter_actionPerformed(e);
            }
        });
        newButton.setMaximumSize(new Dimension(25, 25));
        newButton.setMinimumSize(new Dimension(25, 25));
        newButton.setToolTipText(
        		labels.getString("NetlistEditFrame.NewNetListTT"));
        newButton.setIcon(imageNew);
        newButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuFileNew_actionPerformed(e);
            }
        });
        imageOpen.setDescription("vfs://host:0/file:///C%|/ilex/work/lrgs/classes/lrgs/nledit/open.gif");
        openButton.setMaximumSize(new Dimension(25, 25));
        openButton.setMinimumSize(new Dimension(25, 25));
        openButton.setToolTipText("Open Network List");
        openButton.setIcon(imageOpen);
        openButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuFileOpen_actionPerformed(e);
            }
        });
        imageSave.setDescription("vfs://host:0/file:///C%|/ilex/work/lrgs/classes/lrgs/nledit/save.gif");
        cutButton.setMaximumSize(new Dimension(25, 25));
        cutButton.setMinimumSize(new Dimension(25, 25));
        cutButton.setToolTipText(
        		labels.getString("NetlistEditFrame.cutTT"));
        cutButton.setIcon(imageCut);
        cutButton.addActionListener(new java.awt.event.ActionListener() {
           public void actionPerformed(ActionEvent e) {
                jMenuEditCut_actionPerformed(e);
            }
        });
        copyButton.setMaximumSize(new Dimension(25, 25));
        copyButton.setMinimumSize(new Dimension(25, 25));
        copyButton.setToolTipText(
        		labels.getString("NetlistEditFrame.copyTT"));
        copyButton.setIcon(imageCopy);
        copyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuEditCopy_actionPerformed(e);
            }
        });
        pasteButton.setMaximumSize(new Dimension(25, 25));
        pasteButton.setMinimumSize(new Dimension(25, 25));
        pasteButton.setToolTipText(
        		labels.getString("NetlistEditFrame.pasteTT"));
        pasteButton.setIcon(imagePaste);
        pasteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuEditPaste_actionPerformed(e);
            }
        });
        jPanel1.setLayout(borderLayout2);
//        addDcpButton.setPreferredSize(new Dimension(70, 23));
//        moveUpButton.setPreferredSize(new Dimension(70, 23));
//        moveDownButton.setPreferredSize(new Dimension(70, 23));
        jToolBar.setFloatable(false);
        jMenuFileMerge.setText(labels.getString("NetlistEditFrame.merge"));
        jMenuFileMerge.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuFileMerge_actionPerformed(e);
            }
        });
//        sortIdButton.setPreferredSize(new Dimension(70, 23));
//        sortNameButton.setPreferredSize(new Dimension(70, 23));
        insertBeforeButton.setMaximumSize(new Dimension(25, 25));
        insertBeforeButton.setMinimumSize(new Dimension(25, 25));
        insertBeforeButton.setToolTipText(
        		labels.getString("NetlistEditFrame.insertBeforeTT"));
        insertBeforeButton.setIcon(imageInsertBefore);
        insertBeforeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuEditInsertBefore_actionPerformed(e);
            }
        });
        insertAfterButton.setMaximumSize(new Dimension(25, 25));
        insertAfterButton.setMinimumSize(new Dimension(25, 25));
        insertAfterButton.setToolTipText(
        		labels.getString("NetlistEditFrame.insertAfterTT"));
        insertAfterButton.setIcon(imageInsertAfter);
        insertAfterButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuEditInsertAfter_actionPerformed(e);
            }
        });
        jMenuFile.add(jMenuFileNew);
        jMenuFile.add(jMenuFileOpen);
        jMenuFile.add(jMenuFileMerge);
        jMenuFile.add(jMenuFileSave);
        jMenuFile.add(jMenuFileSaveAs);
        jMenuFile.addSeparator();
        jMenuFile.add(jMenuFileExit);
        jMenuHelp.add(jMenuHelpAbout);
        jMenuBar1.add(jMenuFile);
        jMenuBar1.add(jMenu1);
        jMenuBar1.add(jMenuHelp);
        this.setJMenuBar(jMenuBar1);
        contentPane.add(jToolBar, BorderLayout.NORTH);
        jToolBar.add(newButton, null);
        jToolBar.add(openButton, null);
        jToolBar.add(saveButton);
		jToolBar.addSeparator(new Dimension(10, 31));
        jToolBar.add(cutButton, null);
        jToolBar.add(copyButton, null);
        jToolBar.add(pasteButton, null);
		jToolBar.addSeparator(new Dimension(10, 31));
        jToolBar.add(insertBeforeButton, null);
        jToolBar.add(insertAfterButton, null);
        contentPane.add(statusBar, BorderLayout.SOUTH);
        contentPane.add(jPanel1, BorderLayout.CENTER);
        jPanel1.add(tableScrollPane, BorderLayout.CENTER);

//		networkListTable.createDefaultColumnsFromModel();
//		networkListTable.setColumnSelectionAllowed(true);
//		networkListTable.setRowSelectionAllowed(true);

//		ListSelectionModel lsm = networkListTable.getColumnModel().getSelectionModel();
//		lsm.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//		lsm.addListSelectionListener(new TestListener());
//		JTableHeader jth = networkListTable.getTableHeader();
//		jth.setReorderingAllowed(false);

        tableScrollPane.getViewport().add(networkListTable, null);

        jMenu1.add(jMenuEditCut);
        jMenu1.add(jMenuEditCopy);
        jMenu1.add(jMenuEditPaste);
        jMenu1.addSeparator();
        jMenu1.add(jMenuEditInsertBefore);
        jMenu1.add(jMenuEditInsertAfter);
    }

    /**File | Exit action performed*/
    public void jMenuFileExit_actionPerformed(ActionEvent e)
	{
		if (!okToAbandon())
			return;

		if (isStandAlone)
	        System.exit(0);
		else
			dispose();
    }

	public void setStandAlone(boolean tf) { isStandAlone = tf; }

    /**Help | About action performed*/
    public void jMenuHelpAbout_actionPerformed(ActionEvent e)
	{
		JDialog dlg = ResourceFactory.instance().getAboutDialog(
			this, "NLEdit", "LRGS Netlist Editor");

        Dimension dlgSize = dlg.getPreferredSize();
        Dimension frmSize = getSize();
        Point loc = getLocation();
        dlg.setLocation((frmSize.width - dlgSize.width) / 2 + loc.x, (frmSize.height - dlgSize.height) / 2 + loc.y);
        dlg.setVisible(true);
    }
    /**Overridden so we can exit when window is closed*/
    protected void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            jMenuFileExit_actionPerformed(null);
        }
    }


    void jMenuFileNew_actionPerformed(ActionEvent e)
	{
		if (!okToAbandon())
			return;
		networkListTable.clear();
    }

    void jMenuFileOpen_actionPerformed(ActionEvent e)
	{
		if (!okToAbandon())
			return;
		if (JFileChooser.APPROVE_OPTION == myFileChooser.showOpenDialog(this))
		{
			File f = myFileChooser.getSelectedFile();
			openFile(f.getPath());
		}
    }

    void jMenuFileSave_actionPerformed(ActionEvent e)
	{
		if (curFileName == null)
			jMenuFileSaveAs_actionPerformed(e);
		else
			networkListTable.saveFile(curFileName);
    }

    void jMenuFileSaveAs_actionPerformed(ActionEvent e)
	{
		// Use the SAVE version of the dialog, test return for Approve/Cancel
		if (JFileChooser.APPROVE_OPTION == myFileChooser.showSaveDialog(this))
		{
		    // Set the current file name to the user's selection,
		    // then do a regular saveFile
		    curFileName = myFileChooser.getSelectedFile().getPath();
		    //repaints menu after item is selected
		    statusBar.setText(
		    		labels.getString("NetlistEditFrame.openedStatus")
		    		+ curFileName);
		    this.repaint();
		    networkListTable.saveFile(curFileName);
		}
		else
		{
		    this.repaint();
		}
    }

    void jMenuFileMerge_actionPerformed(ActionEvent e)
	{
		if (JFileChooser.APPROVE_OPTION == myFileChooser.showOpenDialog(this))
		{
			File f = myFileChooser.getSelectedFile();

		    // Display the name of the opened directory+file in the statusBar.
			networkListTable.mergeFile(f.getPath());
		}

    }

    void jMenuEditCut_actionPerformed(ActionEvent e)
	{
		pasteBuffer.cut(networkListTable);
    }

    void jMenuEditCopy_actionPerformed(ActionEvent e)
	{
		pasteBuffer.copy(networkListTable);
    }

    void jMenuEditPaste_actionPerformed(ActionEvent e)
	{
		pasteBuffer.paste(networkListTable);
    }


    void jMenuEditInsertBefore_actionPerformed(ActionEvent e)
	{
		networkListTable.insertBefore();
    }

    void jMenuEditInsertAfter_actionPerformed(ActionEvent e)
	{
		networkListTable.insertAfter();
    }

	public boolean okToAbandon()
	{
		if (!networkListTable.isModified())
			return true;

		int value = JOptionPane.showConfirmDialog(this, 
				genericLabels.getString("saveChanges"),
		    labels.getString("NetlistEditFrame.frameTitle"), 
		    JOptionPane.YES_NO_CANCEL_OPTION) ;

		switch (value)
		{
		case JOptionPane.YES_OPTION:
		    // yes, please save changes
			jMenuFileSave_actionPerformed(null);
		    return !networkListTable.isModified();
		case JOptionPane.NO_OPTION:
		    // no, abandon edits
		    // i.e. return true without saving
		    return true;
		case JOptionPane.CANCEL_OPTION:
		default:
		    // cancel
		    return false;
		}
	}

	void updateCaption()
	{
		String caption;

		if (curFileName == null)
		{
		    // synthesize the "Untitled" name if no name yet.
		    caption = 
		    	labels.getString("NetlistEditFrame.untitled");
		}
		else
		{
		    caption = curFileName;
		}

		// add a "*" in the caption if the file is dirty.
		if (networkListTable.isModified())
		{
		    caption = "* " + caption;
		}
		caption = labels.getString("NetlistEditFrame.frameTitle")
		+  " - " + caption;

		this.setTitle(caption);
	}


	public void openFile(String filename)
	{
		curFileName = filename;

	    // Display the name of the opened directory+file in the statusBar.
	    statusBar.setText(
	    		labels.getString("NetlistEditFrame.openedStatus")
	    		+curFileName);
		networkListTable.loadFile(curFileName);
	}

}


