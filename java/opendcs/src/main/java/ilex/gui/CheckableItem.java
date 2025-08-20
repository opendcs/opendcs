package ilex.gui;

/**
* Used by CheckBoxList to display objects that may be selected.
*/
public abstract class CheckableItem
{
    private boolean selected;

    protected CheckableItem()
    {
        selected = false;
    }

    /**
     * @return true if item is currently selected.
     */
    public boolean isSelected() { return selected; }

    /**
     * Selects or de-selects this item.
     * @param selected true if item is selected.
     */
    public void setSelected(boolean selected) { this.selected = selected; }

    /**
     * Subclass must provide method to return displayable string.
     * @return displayable string
     */
    public abstract String getDisplayString();
}

