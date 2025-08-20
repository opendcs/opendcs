/*
 *  $Id$
 *  
 *  $Log$
 *  Revision 1.1  2015/10/26 12:46:05  mmaloney
 *  Additions for PythonAlgorithm
 *
 */
package decodes.tsdb.compedit;

import decodes.util.DecodesSettings;
import ilex.util.LoadResourceBundle;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ResourceBundle;

/**
 * Dialog to select a Computation Function.
 * Used when writing Python Computations
 */
public class PyFuncSelectDialog extends JDialog
{
	private final JButton insertButton = new JButton("Insert");
	private final JButton cancelButton = new JButton("Cancel");
	private boolean _cancelled;
	private DefaultTableModel model = null;
	private JTable tab = null;
	private final PyFuncList funcList;
	private final JTextArea descArea = new JTextArea();

	private static ResourceBundle labels = null;

	/** No args constructor for JBuilder */
	public PyFuncSelectDialog(JFrame theFrame, PyFuncList funcList)
	{
		super(theFrame, "Select " + funcList.getListName() + " Function", true);
		_cancelled = false;
		this.funcList = funcList;
		try
		{
			DecodesSettings settings = DecodesSettings.instance();
			labels = LoadResourceBundle.getLabelDescriptions(
					"decodes/resources/compedit",
					settings.language);
			guiInit();
			pack();
			getRootPane().setDefaultButton(insertButton);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	void guiInit() {
		JPanel thePanel = new JPanel(new BorderLayout());
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 8));
		insertButton.addActionListener(e -> insertPressed());
		buttonPanel.add(insertButton);
		cancelButton.addActionListener(e -> cancelPressed());
		buttonPanel.add(cancelButton);

		String[] columnNames = getLanguageString("PyFuncSelectDialog.ColumnNames").split(",");
		model = new DefaultTableModel(columnNames,0){
			@Override
			public boolean isCellEditable(int row, int column) {
				return false; // Disable cell editing
			}
		};
		for (PyFunction item : funcList.getList()) {
			model.addRow(new Object[]{item.getName(), item.getSignature(), item});
		}
		tab = new JTable(model);
		tab.setRowSelectionAllowed(true);
		tab.setColumnSelectionAllowed(false);
		tab.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
		tab.setRowSorter(sorter);
		hideFunctionColumn(tab);
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

		// Add the description Text Area
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
				e -> updateDescription());
	}

	private void hideFunctionColumn(JTable table) {
		TableColumnModel columnModel = table.getColumnModel();
		TableColumn column = columnModel.getColumn(2);
		column.setMinWidth(0);
		column.setMaxWidth(0);
		column.setWidth(0);
	}

	private void updateDescription(){
		descArea.setText(getSelection().getDesc());
		descArea.setCaretPosition(0); // Scroll to the top
	}

	public PyFunction getSelection() {
		int row = tab.getSelectedRow();
		if (row >= 0)
		{
			int selectedModelRow = tab.getRowSorter().convertRowIndexToModel(row);
			return (PyFunction) model.getValueAt(selectedModelRow,2);
		}
		return new PyFunction("","","");
	}
	
	void insertPressed()
	{
		_cancelled = false;
		int idx = tab.getSelectedRow();
		if (idx < 0)
			_cancelled = true;

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

	private String getLanguageString(String name)
	{
		return labels.getString(name);
	}
}


