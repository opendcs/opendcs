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
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
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
public class PyFuncSelectDialog extends JDialog
{
	private JButton insertButton = new JButton("Insert");
	private JButton cancelButton = new JButton("Cancel");
	private PyFunction selection;
	private boolean _cancelled;
	private FuncListTableModel model = null;
	private SortingListTable tab = null;
	private PyFuncList funcList = null;
	private JTextArea descArea = new JTextArea();

	/** No args constructor for JBuilder */
	public PyFuncSelectDialog(JFrame theFrame, PyFuncList funcList)
	{
		super(theFrame, "Select " + funcList.getListName() + " Function", true);
		_cancelled = false;
		this.funcList = funcList;
		try
		{
			guiInit();
			pack();
			getRootPane().setDefaultButton(insertButton);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	void guiInit() throws Exception
	{
		JPanel thePanel = new JPanel(new BorderLayout());
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 8));
		insertButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				insertPressed();
			}
		});
		buttonPanel.add(insertButton);
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
			model = new FuncListTableModel(funcList);
		tab = new SortingListTable(model, new int[] {30,70});
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setPreferredSize(new Dimension(600, 250));
		scrollPane.getViewport().add(tab, null);
		tab.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() == 2)
				{
					insertPressed();
				}
			}
		} );

		// Add the descirption Text Area
		JScrollPane descPane = new JScrollPane();
		descArea.setLineWrap(true);
		descArea.setWrapStyleWord(true);
		descPane.getViewport().add(descArea, null);
		descPane.setPreferredSize(new Dimension(600, 100));
		JPanel descPanel = new JPanel(new BorderLayout());
		descPanel.add(descPane, BorderLayout.CENTER);
		descPanel.setBorder(new TitledBorder("Description"));
		
		JSplitPane middlePane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		middlePane.add(scrollPane, JSplitPane.TOP);
		middlePane.add(descPanel, JSplitPane.BOTTOM);
		middlePane.setResizeWeight(.75);
		
		thePanel.add(middlePane, BorderLayout.CENTER);
		thePanel.add(buttonPanel, BorderLayout.SOUTH);
		getContentPane().add(thePanel);
		
		tab.getSelectionModel().addListSelectionListener(
			new ListSelectionListener()
			{
				@Override
				public void valueChanged(ListSelectionEvent e)
				{
					int row = tab.getSelectedRow();
					if (row >= 0)
					{
						PyFunction pyf = (PyFunction)model.getRowObject(row);
						descArea.setText(pyf.getDesc());
System.out.println("Row " + row + " selected. set desc to: " + pyf.getDesc());
					}
				}
			});
	}
	
	public PyFunction getSelection() { return selection; }
	
	void insertPressed()
	{
		_cancelled = false;
		int idx = tab.getSelectedRow();
		if (idx < 0)
			_cancelled = true;
		else
			selection = (PyFunction)model.getRowObject(idx);

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
class FuncListTableModel extends AbstractTableModel
	implements SortingListTableModel
{
	private String colNames[] = {"Name", "Signature"};
	
	ArrayList<PyFunction> functions = new ArrayList<PyFunction>();

	public FuncListTableModel(PyFuncList funcList)
	{
		functions.addAll(funcList.getList());
	}
	
	@Override
	public int getColumnCount()
	{
		int r = colNames.length;
		return r;
	}
	
	@Override
	public String getColumnName(int c)
	{
		return colNames[c];
	}

	@Override
	public int getRowCount()
	{
		return functions.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		if (rowIndex < 0 || rowIndex >= functions.size())
			return null;
		PyFunction pyf = functions.get(rowIndex);
		return columnIndex == 0 ? pyf.getName() : pyf.getSignature();
	}

	@Override
	public void sortByColumn(final int column)
	{
		Collections.sort(functions,
			new Comparator<PyFunction>()
			{
				@Override
				public int compare(PyFunction o1, PyFunction o2)
				{
					return column == 0 ? o1.getName().compareTo(o2.getName())
						: o1.getSignature().compareTo(o2.getSignature());
				}
			});
	}

	@Override
	public Object getRowObject(int row)
	{
		return functions.get(row);
	}
}


