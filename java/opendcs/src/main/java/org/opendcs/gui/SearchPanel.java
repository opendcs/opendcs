package org.opendcs.gui;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;

import java.util.regex.Pattern;
/**
 * SearchPanel has a searching JTextField used to filter a TableModel
 */
@SuppressWarnings("serial")
public class SearchPanel extends JPanel
{
	private final JTextField filterField = new JTextField();
	private final JLabel filterStatusLabel = new JLabel("0/0");

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
		this.setLayout(new BorderLayout());
		JLabel filterLabel = new JLabel("Filter: ");

		JPanel filterRow = new JPanel(new BorderLayout());
		filterRow.add(filterLabel, BorderLayout.WEST);

		JPanel filterInput = new JPanel(new BorderLayout());
		filterInput.add(filterField, BorderLayout.CENTER);
		filterInput.add(filterStatusLabel, BorderLayout.EAST);

		filterRow.add(filterInput, BorderLayout.CENTER);
		this.add(filterRow, BorderLayout.NORTH);
	}

	private void setupFilter()
	{
		filterField.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) { newFilter(); }
			public void removeUpdate(DocumentEvent e) { newFilter(); }
			public void changedUpdate(DocumentEvent e) { newFilter(); }
		});
	}

	private void newFilter()
	{
		String text = filterField.getText();
		if (text == null || text.trim().isEmpty()) {
			sorter.setRowFilter(null);
		} else {
			sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
		}
		updateStatus();
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
