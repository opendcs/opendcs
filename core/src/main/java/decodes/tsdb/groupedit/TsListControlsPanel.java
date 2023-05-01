package decodes.tsdb.groupedit;

import ilex.util.LoadResourceBundle;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JPanel;

import decodes.util.DecodesSettings;

/**
All Ts Editor List Panel objects display a TsListControlsPanel at the bottom.
*/
public class TsListControlsPanel extends JPanel
{
    // Titles, Labels defined here for internationalization
    String openLabel;
    String newLabel;
    String deleteLabel;
    String refreshLabel;

    JButton openButton = new JButton();
    JButton newButton = new JButton();
    JButton deleteButton = new JButton();
    JButton refreshButton = new JButton();
    TsListControllers myController;
    GridBagLayout gridBagLayout1 = new GridBagLayout();

    /**
      Constructor.
      @ctl the parent panel.
    */
	public TsListControlsPanel(TsListControllers ctl)
	{
		myController = ctl;
		ResourceBundle genericResources = LoadResourceBundle
			.getLabelDescriptions("decodes/resources/generic",
				DecodesSettings.instance().language);

		// For internationalization, get the title description
		// from properties file
		openLabel = genericResources.getString("open");
		newLabel = genericResources.getString("new");
		deleteLabel = genericResources.getString("delete");
		refreshLabel = genericResources.getString("refresh");

		try
		{
			jbInit();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

    /** GUI component initialization. */
    private void jbInit() throws Exception {
        openButton.setText(openLabel);
        //openButton.setIcon(new ImageIcon("tangerine2-icon.gif"));
        openButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openButton_actionPerformed(e);
            }
        });
        this.setLayout(gridBagLayout1);
        newButton.setText(newLabel);
        //newButton.setIcon(new ImageIcon("images/new.gif"));
        newButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                newButton_actionPerformed(e);
            }
        });
        deleteButton.setText(deleteLabel);
        //deleteButton.setIcon(new ImageIcon("images/delete.gif"));
        deleteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                deleteButton_actionPerformed(e);
            }
        });
        refreshButton.setText(refreshLabel);
        //refreshButton.setIcon(new ImageIcon("images/refresh.gif"));
        refreshButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	refreshButton_actionPerformed(e);
            }
        });
		this.setMinimumSize(new Dimension(571, 50));
//        this.setPreferredSize(new Dimension(571, 50));
		this.add(openButton,
			 new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0
						, GridBagConstraints.CENTER,
						GridBagConstraints.HORIZONTAL,
						new Insets(10, 4, 10, 4), 0, 0));
		this.add(newButton, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
		    , GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
		    new Insets(10, 4, 10, 4), 0, 0));
		this.add(deleteButton,
			 new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0
						, GridBagConstraints.CENTER,
						GridBagConstraints.HORIZONTAL,
						new Insets(10, 4, 10, 4), 0, 0));
		this.add(refreshButton,
			 new GridBagConstraints(4, 0, 1, 1, 1.5, 0.0
						, GridBagConstraints.EAST,
						GridBagConstraints.NONE,
						new Insets(10, 4, 10, 4), 18, 0));
	}

        /**
          Called when user presses the 'Open' Button.
          @param e ignored.
        */
    void openButton_actionPerformed(ActionEvent e)
    {
                myController.openPressed();
    }

        /**
          Called when user presses the 'New' Button.
          @param e ignored.
        */
    void newButton_actionPerformed(ActionEvent e)
        {
                myController.newPressed();
    }

    /**
          Called when user presses the 'Delete' Button.
          @param e ignored.
        */
    void deleteButton_actionPerformed(ActionEvent e)
        {
                myController.deletePressed();
    }

    /**
      Called when user presses the 'Refresh' Button.
      @param e ignored.
     */
    void refreshButton_actionPerformed(ActionEvent e)
    {
        myController.refresh();
    }


}
