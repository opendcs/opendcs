/*
 *  $Id$
 *  
 *  $Log$
 *  Revision 1.1  2015/10/26 12:46:06  mmaloney
 *  Additions for PythonAlgorithm
 *
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
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;

import javax.swing.DefaultRowSorter;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.Stream;

import decodes.gui.SortingListTable;
import decodes.gui.SortingListTableModel;
import decodes.tsdb.CompMetaData;
import decodes.tsdb.DbCompAlgorithm;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.compedit.algotab.ExecClassTableModel;
import decodes.tsdb.xml.CompXio;
import decodes.tsdb.xml.DbXmlException;

/**
 * Dialog to select an equipment model by name.
 */
@SuppressWarnings("serial")
public class ExecClassSelectDialog extends JDialog
{
	private JButton selectButton = new JButton("Select");
	private JButton cancelButton = new JButton("Cancel");
	private DbCompAlgorithm selection;
	private boolean _cancelled;
	private final ExecClassTableModel model = new ExecClassTableModel();
	private final JTable tab = new JTable(model);

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
		selectButton.addActionListener(e -> selectPressed());
		buttonPanel.add(selectButton);
		cancelButton.addActionListener(e -> cancelPressed());
		buttonPanel.add(cancelButton);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setPreferredSize(new Dimension(600, 400));
		scrollPane.getViewport().add(tab, null);
		tab.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tab.setRowSorter(new TableRowSorter<>(model));
		tab.addMouseListener(new MouseAdapter()
		{
			@Override
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
		throws NoSuchObjectException
	{
		model.load();
	}

	public void setSelection(String selection)
	{
		if (selection != null)
		{
			int modelRow = model.indexOf(selection);
			if (modelRow != -1)
			{				
				int tableRow = tab.convertRowIndexToView(modelRow);;
				tab.setRowSelectionInterval(tableRow, tableRow);
				this.selection = model.getAlgoAt(modelRow);
			}
		}
		else
		{
			this.selection = null;
		}
		
	}

	public DbCompAlgorithm getSelection()
	{
		return selection;
	}
	
	void selectPressed()
	{
		_cancelled = false;
		int idx = tab.getSelectedRow();
		if (idx < 0)
			_cancelled = true;
		else
		{
			int modelRow = tab.convertRowIndexToModel(idx);
			selection = model.getAlgoAt(modelRow);
		}
			
		
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
