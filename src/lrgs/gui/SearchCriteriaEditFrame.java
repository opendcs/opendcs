package lrgs.gui;

import ilex.gui.GuiApp;
import ilex.gui.MenuFrame;
import ilex.util.LoadResourceBundle;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;

import lrgs.common.SearchCriteria;
import decodes.gui.TopFrame;
import decodes.util.DecodesSettings;
import decodes.util.NwsXref;
import decodes.util.Pdt;

@SuppressWarnings("serial")
public class SearchCriteriaEditFrame 
	extends TopFrame
	implements SearchCriteriaEditorIF
{
	private SearchCriteriaEditPanel scePanel = null;
	private JFileChooser filechooser = new JFileChooser();
	private File scFile = null;
	private boolean autoSave = false;
	private SearchCritEditorParent parent = null;

	
	public SearchCriteriaEditFrame()
	{
		super();
		setTitle("Search Criteria Editor");
		init();
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pack();
		trackChanges("SearchCritEdit");
	}
	
	public SearchCriteriaEditFrame(File sc)
	{
		this();
		scFile = sc;
		if (scFile == null)
			return;
		try
		{
			scePanel.setSearchCrit(new SearchCriteria(scFile));
		}
		catch(Exception ex)
		{
			showError("Cannot read search criteria from '" + scFile.getPath()
				+ "': " + ex);
			return;
		}
	}

	private void init()
	{
		JMenu jMenuFile = new JMenu("File");
		JMenuItem jMenuFileOpen = new JMenuItem("Open");
		jMenuFile.add(jMenuFileOpen);
		jMenuFileOpen.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					openPressed();
				}
			});

		JMenuItem jMenuFileNew = new JMenuItem("New");
		jMenuFile.add(jMenuFileNew);
		jMenuFileNew.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					newPressed();
				}
			});
		JMenuItem jMenuFileSave = new JMenuItem("Save");
		jMenuFile.add(jMenuFileSave);
		jMenuFileSave.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					savePressed();
				}
			});
		JMenuItem jMenuFileSaveAs = new JMenuItem("Save As");
		jMenuFile.add(jMenuFileSaveAs);
		jMenuFileSaveAs.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					saveAsPressed();
				}
			});
		JMenuItem jMenuFileExit = new JMenuItem("Exit");
		jMenuFile.addSeparator();
		jMenuFile.add(jMenuFileExit);
		jMenuFileExit.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					exitPressed();
				}
			});
		
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(jMenuFile);
		setJMenuBar(menuBar);
		
		add(scePanel = new SearchCriteriaEditPanel());
		scePanel.setTopFrame(this);

		filechooser.setCurrentDirectory(
			new File(System.getProperty("user.dir")));
	}
	
	
	protected void exitPressed()
	{
		if (scePanel.hasChanged())
		{
			if (autoSave && scFile != null)
				savePressed();
			else if (!checkDiscard())
				return;
		}
		cleanupBeforeExit();

		if (exitOnClose) 
			System.exit(0);
		else
			dispose();
	}

	protected void saveAsPressed()
	{
		int option = filechooser.showSaveDialog(this);
		if (option == JFileChooser.APPROVE_OPTION)
		{
			scFile = filechooser.getSelectedFile();
			if (scFile == null)
				return;
			savePressed();
		}
	}

	protected void savePressed()
	{
		if (scFile == null)
		{
			saveAsPressed();
			return;
		}
		SearchCriteria sc = new SearchCriteria();
		scePanel.fillSearchCrit(sc);
		try
		{
			sc.saveFile(scFile);
		}
		catch (IOException ex)
		{
			showError("Cannot save search criteria to '" 
				+ scFile.getPath() + "': " + ex);
		}
	}

	protected void newPressed()
	{
		if (!checkDiscard())
			return;
		SearchCriteria sc = new SearchCriteria();
		sc.setLrgsSince("now - 1 hour");
		sc.setLrgsUntil("now");
		scePanel.setSearchCrit(sc);
	}

	protected void openPressed()
	{
		if (!checkDiscard())
			return;
		int option = filechooser.showOpenDialog(this);
		if (option == JFileChooser.APPROVE_OPTION)
		{
			scFile = filechooser.getSelectedFile();
			if (scFile == null)
				return;
			try
			{
				scePanel.setSearchCrit(new SearchCriteria(scFile));
			}
			catch(Exception ex)
			{
				showError("Cannot read search criteria from '" + scFile.getPath()
					+ "': " + ex);
				return;
			}
		}
	}
	
	
	/**
	 * Return true if it's OK to discard the current edit fields.
	 */
	private boolean checkDiscard()
	{
		if (!scePanel.hasChanged())
			return true;
		return showConfirm("Confirm Search Criteria", 
			"Search criteria have been modified. Discard changes?", 
			JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
	}


	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		DecodesInterface.maintainGoesPdt();
		SearchCriteriaEditFrame f = new SearchCriteriaEditFrame();
//		f.centerOnScreen();
		Rectangle r = f.getBounds();
		f.setExitOnClose(true);
		f.launch(r.x, r.y, r.width, r.height);
	}

	@Override
	public void setAutoSave(boolean tf)
	{
		autoSave = tf;
	}

	@Override
	public void setParent(SearchCritEditorParent parent)
	{
		this.parent = parent;
	}

	@Override
	public void movedTo(Point p)
	{
		GuiApp.setProperty("SearchCritEditor.x", ""+p.x);
		GuiApp.setProperty("SearchCritEditor.y", ""+p.y);
	}

	@Override
	public void startup(int x, int y)
	{
		Dimension prefSize = getPreferredSize();
		int width = GuiApp.getIntProperty("SearchCritEditor.width", prefSize.width);
		int height = GuiApp.getIntProperty("SearchCritEditor.height", prefSize.height);
		if (height < 420)
			height = 420;
		x = GuiApp.getIntProperty("SearchCritEditor.x", x);
		y = GuiApp.getIntProperty("SearchCritEditor.y", y);
		launch(x, y, width, height);
	}

	@Override
	public void cleanupBeforeExit()
	{
System.out.println("cleanupBeforeExit()");
		if (parent != null)
			parent.closingSearchCritEditor();
	}

	@Override
	public void fillSearchCrit(SearchCriteria searchcrit)
	{
		scePanel.fillSearchCrit(searchcrit);
	}

	@Override
	public boolean isChanged()
	{
		return scePanel.hasChanged();
	}

	public void launch( int x, int y, int w, int h )
	{
		setBounds(x,y,w,h);
		setVisible(true);

		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		final SearchCriteriaEditFrame myframe = this;
		addWindowListener(
			new WindowAdapter()
			{
				public void windowClosed(WindowEvent e)
				{
					myframe.cleanupBeforeExit();
					if (exitOnClose)
						System.exit(0);
				}
			});

//		addComponentListener(
//			new ComponentAdapter()
//			{
//				public void componentResized(ComponentEvent e)
//				{
//System.out.println("componentResized()");
//					Dimension d = e.getComponent().getSize();
//					myframe.setSize(d);
//					//myframe.resize(d);
//				}
//				public void componentMoved(ComponentEvent e)
//				{
//System.out.println("componentMoved()");
//					Point p = e.getComponent().getLocation();
//					myframe.movedTo(p);
//				}
//			});

	}

}
