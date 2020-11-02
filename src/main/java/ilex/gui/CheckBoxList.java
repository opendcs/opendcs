package ilex.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

/**
This displays a list containing checkboxes, one per line.
You must provide a ListModel containing CheckableItem objects.
*/
public class CheckBoxList
    extends JList
{
    private MyListModel model;

    /** Constructor. */
    public CheckBoxList()
    {
        super(new MyListModel());
        model = (MyListModel)getModel();

        setCellRenderer(new CheckListRenderer(this));
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setBorder(new EmptyBorder(0,4,0,0));
        addMouseListener(
            new MouseAdapter()
            {
                public void mouseClicked(MouseEvent e)
                {
                    int index = locationToIndex(e.getPoint());
                    CheckableItem item =
                        (CheckableItem)getModel().getElementAt(index);
                    item.setSelected(! item.isSelected());
                    Rectangle rect = getCellBounds(index, index);
                    repaint(rect);
                }
            });
    }

	/**
	 * @return the list model containing the data.
	 */
    public DefaultListModel getDefaultModel() { return model; }

	/**
	 * Checks or unchecks all elements in the list.
	 * @param checked the value that all checkboxes will be set to.
	 */
	public void selectAll(boolean checked)
	{
		int n = model.size();
		for(int i=0; i<n; i++)
		{
			CheckableItem cdo = (CheckableItem)model.get(i);
			cdo.setSelected(checked);
		}
		model.fireAllChanged();
	}

    // inner class to render the cells
    class CheckListRenderer extends JCheckBox implements ListCellRenderer
    {
        private JList list;

        public CheckListRenderer(JList list)
        {
            this.list = list;
            setBackground(UIManager.getColor("List.textBackground"));
            setForeground(UIManager.getColor("List.textForeground"));
        }

        public Component getListCellRendererComponent(JList list,
            Object value, int index, boolean isSelected, boolean hasFocus)
        {
            CheckableItem item = (CheckableItem)value;
            setEnabled(list.isEnabled());
            setSelected(item.isSelected());
            setFont(list.getFont());
            setText(item.getDisplayString());
            return this;
        }
	}
}

class MyListModel extends DefaultListModel
{
	public void fireAllChanged()
	{
		fireContentsChanged(this, 0, size());
	}
}

