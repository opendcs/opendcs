/*
 *  $Id$
 *  
 *  $Log$
 */
package decodes.tsdb.compedit;

import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.StringPair;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import decodes.gui.SortingListTable;
import decodes.gui.SortingListTableModel;
import decodes.tsdb.DbAlgorithmExecutive;

/**
 * Dialog to select an equipment model by name.
 */
@SuppressWarnings("serial")
public class ExecClassSelectDialog extends JDialog
{
	private JButton selectButton = new JButton("Select");
	private JButton cancelButton = new JButton("Cancel");
	private String selection;
	private boolean _cancelled;
	private static ExecClassTableModel model = null;
	private SortingListTable tab = null;
	private static boolean isLoaded = false;

	/** No args constructor for JBuilder */
	public ExecClassSelectDialog(JFrame theFrame)
	{
		super(theFrame, "Select Executable Class", true);
		_cancelled = false;
		try
		{
			guiInit();
			pack();
			getRootPane().setDefaultButton(selectButton);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	void guiInit() throws Exception
	{
		JPanel thePanel = new JPanel(new BorderLayout());
		thePanel.setBorder(new TitledBorder("Select Executable Class"));
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 8));
		selectButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				selectPressed();
			}
		});
		buttonPanel.add(selectButton);
		cancelButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				cancelPressed();
			}
		});
		buttonPanel.add(cancelButton);

		// Set up the table
		if (model == null)
			model = new ExecClassTableModel();
		tab = new SortingListTable(model, new int[] {50,50});
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setPreferredSize(new Dimension(600, 400));
		scrollPane.getViewport().add(tab, null);
		tab.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() == 2)
				{
					selectPressed();
				}
			}
		} );

		
		thePanel.add(scrollPane, BorderLayout.CENTER);
		thePanel.add(buttonPanel, BorderLayout.SOUTH);
		getContentPane().add(thePanel);
	}
	
	public void load()
	{
		if (isLoaded)
			return;
		isLoaded = true;
		// Load the model in a background thread for speed.
		new Thread(
			new Runnable()
			{
				@Override
				public void run()
				{
					model.load();
				}
				
			}).start();
	}

	public void setSelection(String selection)
	{
		if (selection != null)
		{
			int idx = model.indexOf(selection);
			if (idx != -1)
				tab.setRowSelectionInterval(idx, idx);
		}
		this.selection = selection;
	}

	public String getSelection() { return selection; }
	
	void selectPressed()
	{
		_cancelled = false;
		int idx = tab.getSelectedRow();
		if (idx < 0)
			_cancelled = true;
		else
			selection = model.classlist.get(idx).first;
		
		closeDlg();
	}

	/** Closes the dialog. */
	void closeDlg()
	{
		setVisible(false);
		dispose();
	}

	void cancelPressed()
	{
		_cancelled = true;
		closeDlg();
	}

	/** @return true if cancel was pressed. */
	public boolean wasCancelled()
	{
		return _cancelled;
	}
}

@SuppressWarnings("serial")
class ExecClassTableModel extends AbstractTableModel
	implements SortingListTableModel
{
	private String colNames[] = {"Class Name", "Description"};
	
	ArrayList<StringPair> classlist = new ArrayList<StringPair>();
	
	@Override
	public int getColumnCount()
	{
		int r = colNames.length;
		return r;
	}
	public int indexOf(String selection)
	{
		for(int i=0; i<classlist.size(); i++)
			if (selection.equals(classlist.get(i).first))
				return i;
		return -1;
	}
	@Override
	public String getColumnName(int c)
	{
		return colNames[c];
	}

	@Override
	public int getRowCount()
	{
		return classlist.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		if (rowIndex < 0 || rowIndex >= classlist.size())
			return null;
		StringPair sp = classlist.get(rowIndex);
		return columnIndex == 0 ? sp.first : sp.second;
	}

	@Override
	public void sortByColumn(final int column)
	{
		Collections.sort(classlist,
			new Comparator<StringPair>()
			{
				@Override
				public int compare(StringPair o1, StringPair o2)
				{
					return column == 0 ? o1.first.compareTo(o2.first)
						: o1.second.compareTo(o2.second);
				}
			});
	}

	@Override
	public Object getRowObject(int row)
	{
		return classlist.get(row);
	}
	
	@SuppressWarnings("rawtypes")
	void load()
	{
		File listfile = new File(EnvExpander.expand("$DCSTOOL_USERDIR/algorithms.txt"));
		if (!listfile.canRead())
			return;
		LineNumberReader lnr = null;
		try
		{
			lnr = new LineNumberReader(new FileReader(listfile));
			String line;
		  nextLine:
			while((line = lnr.readLine()) != null)
			{
				String className = line.trim();
				if (className.length() == 0)
					continue;
				for(StringPair sp : classlist)
					if (sp.first.equals(className))
						continue nextLine;
				classlist.add(new StringPair(className, ""));
			}
		}
		catch (IOException ex)
		{
			Logger.instance().warning("Error reading '" + listfile.getPath() + "': " + ex);
		}
		finally
		{
			if (lnr != null)
				try { lnr.close(); } catch(Exception x){}
		}
		
		// Now instantiate each class to try to get a description.
		ClassLoader cl = Thread.currentThread().getContextClassLoader();

		for(StringPair sp : classlist)
		{
			try
			{
				Class execClass = cl.loadClass(sp.first);
				DbAlgorithmExecutive dbe = (DbAlgorithmExecutive) execClass.newInstance();
				sp.second = dbe.getBriefDescription();
System.out.println("algo '" + sp.first + "' desc=" + sp.second);
			}
			catch(Exception ex)
			{
				Logger.instance().warning("Class from " + listfile.getPath()
					+ ": '" + sp.first + "': " + ex);
			}
		}
	}
}


