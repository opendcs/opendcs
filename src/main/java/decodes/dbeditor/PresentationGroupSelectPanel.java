/**
 * Copyright 2024 The OpenDCS Consortium and contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package decodes.dbeditor;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.ResourceBundle;

import decodes.dbeditor.presentation.PresentationGroupTableModel;
import decodes.db.PresentationGroup;
import decodes.db.Database;

@SuppressWarnings("serial")
public class PresentationGroupSelectPanel extends JPanel
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

	BorderLayout borderLayout1 = new BorderLayout();
	JScrollPane jScrollPane1 = new JScrollPane();
	PresentationGroupTableModel model;
	JTable pgTable;

	public PresentationGroupSelectPanel(final PresentationGroupListPanel pglp)
	{
		model = new PresentationGroupTableModel(this, Database.getDb());
		pgTable = new JTable(model);
		pgTable.setAutoCreateRowSorter(true);
		pgTable.getSelectionModel().setSelectionMode(
		ListSelectionModel.SINGLE_SELECTION);
		pgTable.addMouseListener(
			new MouseAdapter()
			{
				public void mouseClicked(MouseEvent e)
				{
					if (e.getClickCount() == 2)
					{
						pglp.openPressed();
					}
				}
			});
		try {
		    jbInit();
		}
		catch(Exception ex) {
		    ex.printStackTrace();
		}
	}

	private void jbInit() throws Exception
	{
		this.setLayout(borderLayout1);
		jScrollPane1.setPreferredSize(new Dimension(453, 300));
        this.add(jScrollPane1, BorderLayout.CENTER);
		jScrollPane1.getViewport().add(pgTable, null);
	}

	/**
	 * Returns the currently-selected object, or null if none is selected.
	 */
	public PresentationGroup getSelection()
	{
		int r = pgTable.getSelectedRow();
		if (r == -1)
		{
			return null;
		}
		int modelRow = pgTable.convertRowIndexToModel(r);
		return model.getObjectAt(modelRow);
	}

	public void refill()
	{
		model.refill();
	}

	public void addPG(PresentationGroup ob)
	{
		model.add(ob);
		invalidate();
		repaint();
	}

	public void deleteSelection()
	{
		int r = pgTable.getSelectedRow();
		if (r == -1)
		{
			return;
		}
		int modelRow = pgTable.convertRowIndexToModel(r);
		model.deleteAt(modelRow);
	}

	public void replace(PresentationGroup oldPg, PresentationGroup newPg)
	{
		model.replace(oldPg, newPg);
	}
}
