package decodes.dbeditor;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.ResourceBundle;

import ilex.util.LoadResourceBundle;

/**
This panel appears at the bottom of the DB Editor panels and contains the
'Commit', 'Close', and 'Help' buttons.
*/
public class EntityOpsPanel extends JPanel
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();

    JButton commitButton = new JButton();
    JButton closeButton = new JButton();
    JButton helpButton = new JButton();
    GridBagLayout gridBagLayout1 = new GridBagLayout();
	EntityOpsController myController;

	/**
	  Constructor.
	  @param ctl the owning panel must implement these methods.
	*/
    public EntityOpsPanel(EntityOpsController ctl)
	{
		myController = ctl;

        try {
            jbInit();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

	/** GUI component initialization. */
    private void jbInit() throws Exception {
        commitButton.setText(genericLabels.getString("commit"));
        commitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                commitButton_actionPerformed(e);
            }
        });
        this.setLayout(gridBagLayout1);
        closeButton.setText(genericLabels.getString("close"));
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                closeButton_actionPerformed(e);
            }
        });
        helpButton.setText(genericLabels.getString("help"));
        helpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                helpButton_actionPerformed(e);
            }
        });
        this.add(helpButton, new GridBagConstraints(2, 0, 1, 1, 1.0, 1.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(12, 10, 12, 10), 0, 0));
        this.add(commitButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(12, 10, 12, 10), 0, 0));
        this.add(closeButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(12, 10, 12, 10), 0, 0));
    }

	/**
	  Called when user presses the 'Commit' Button.
	  @param e ignored.
	*/
    void commitButton_actionPerformed(ActionEvent e)
	{
		myController.commitEntity();
    }

	/**
	  Called when user presses the 'Close' Button.
	  @param e ignored.
	*/
    void closeButton_actionPerformed(ActionEvent e)
	{
		myController.closeEntity();
    }

	/**
	  Called when user presses the 'Help' Button.
	  @param e ignored.
	*/
    void helpButton_actionPerformed(ActionEvent e)
	{
		myController.help();
    }
}

