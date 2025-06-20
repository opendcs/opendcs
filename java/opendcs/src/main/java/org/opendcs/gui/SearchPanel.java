package org.opendcs.gui;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import decodes.dbeditor.DbEditorFrame;

/**
 * SearchPanel is used to filter a TableModel
 * Search engine like filtering , with options for whole word, and case sensitive.
 * comboBox to select what column (or all) is used for filtering.
 */
@SuppressWarnings("serial")
public class SearchPanel extends JPanel
{
    private static String allColumns ;
    private static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	private final JTextField filterField = new JTextField();
	private final JLabel filterStatusLabel = new JLabel("0/0");

	private final JCheckBox matchCaseCheckBox = new JCheckBox(genericLabels.getString("matchCase"));
	private final JCheckBox wholeWordCheckBox = new JCheckBox(genericLabels.getString("matchWord"));

	private final JComboBox<String> columnSelector = new JComboBox<>();

	private final TableRowSorter<? extends TableModel> sorter;
	private final TableModel model;

	public SearchPanel(TableRowSorter<? extends TableModel> sorter, TableModel model)
	{
		this.sorter = sorter;
		this.model = model;
		buildUI();
		setupFilter();
		updateStatus();
	}

	private void buildUI()
	{
        allColumns = genericLabels.getString("allColumns");
		this.setLayout(new BorderLayout());

		JPanel comboRow = new JPanel(new BorderLayout());
		comboRow.add(columnSelector, BorderLayout.WEST);

		columnSelector.addItem(allColumns);
		for (int i = 0; i < model.getColumnCount(); i++) {
			columnSelector.addItem(model.getColumnName(i));
		}

		JLabel filterLabel = new JLabel(genericLabels.getString("filter") + ":");

		JPanel filterRow = new JPanel(new BorderLayout());
		filterRow.add(filterLabel, BorderLayout.WEST);

		JPanel filterInputPanel = new JPanel(new BorderLayout());
		filterInputPanel.add(filterField, BorderLayout.CENTER);

		JPanel optionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		optionPanel.add(wholeWordCheckBox);
		optionPanel.add(matchCaseCheckBox);
		filterInputPanel.add(optionPanel, BorderLayout.EAST);

		JPanel topRow = new JPanel(new BorderLayout());
		topRow.add(filterRow, BorderLayout.WEST);
		topRow.add(filterInputPanel, BorderLayout.CENTER);

		JPanel upperPanel = new JPanel(new BorderLayout());
		upperPanel.add(comboRow, BorderLayout.NORTH);
		upperPanel.add(topRow, BorderLayout.SOUTH);

		JPanel fullPanel = new JPanel(new BorderLayout());
		fullPanel.add(upperPanel, BorderLayout.NORTH);
		fullPanel.add(filterStatusLabel, BorderLayout.SOUTH);

		this.add(fullPanel, BorderLayout.NORTH);
	}

	private void setupFilter()
	{
		DocumentListener listener = new DocumentListener() {
			public void insertUpdate(DocumentEvent e) { newFilter(); }
			public void removeUpdate(DocumentEvent e) { newFilter(); }
			public void changedUpdate(DocumentEvent e) { newFilter(); }
		};
		filterField.getDocument().addDocumentListener(listener);

		matchCaseCheckBox.addActionListener(e -> newFilter());
		wholeWordCheckBox.addActionListener(e -> newFilter());
		columnSelector.addActionListener(e -> newFilter());
	}

	private void newFilter()
	{
		String text = filterField.getText();
		if (text == null || text.trim().isEmpty()) 
		{
			sorter.setRowFilter(null);
		} 
		else 
		{
			String raw = Pattern.quote(text);
			if (wholeWordCheckBox.isSelected()) {
				raw = "\\b" + raw + "\\b";
			}
			int flags = matchCaseCheckBox.isSelected() ? 0 : Pattern.CASE_INSENSITIVE;
			final Pattern compiledPattern = Pattern.compile(raw, flags);

			String selectedColumn = (String) columnSelector.getSelectedItem();

			RowFilter<TableModel, Integer> filter = new RowFilter<TableModel, Integer>() {
				public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
					if (allColumns.equals(selectedColumn)) {
						for (int i = 0; i < entry.getValueCount(); i++) {
							String value = entry.getStringValue(i);
							if (value != null && compiledPattern.matcher(value).find()) {
								return true;
							}
						}
					} else {
						int columnIndex = getColumnIndexByName(selectedColumn);
						if (columnIndex >= 0) {
							String value = entry.getStringValue(columnIndex);
							if (value != null && compiledPattern.matcher(value).find()) {
								return true;
							}
						}
					}
					return false;
				}
			};

			sorter.setRowFilter(filter);
		}

		updateStatus();
	}

	private int getColumnIndexByName(String columnName) {
		for (int i = 0; i < model.getColumnCount(); i++) {
			if (model.getColumnName(i).equals(columnName)) {
				return i;
			}
		}
		return -1;
	}

	private void updateStatus()
	{
		int total = model.getRowCount();
		int shown = sorter.getViewRowCount();
		filterStatusLabel.setText(shown + "/" + total);
	}

	public JTextField getFilterField() {
		return filterField;
	}
}
