package decodes.rledit;

import java.awt.*;
import java.util.ResourceBundle;

import javax.swing.*;

import decodes.util.DecodesVersion;
import ilex.util.EnvExpander;

/**
The "about box" for the reference list editor.
*/
public class RefListFrame_AboutBox extends JDialog 
{
	private static ResourceBundle genericLabels = 
		RefListEditor.getGenericLabels();
	private static ResourceBundle labels = RefListEditor.getLabels();
	private JPanel panel1 = new JPanel();
	private JPanel panel2 = new JPanel();
	private JPanel insetsPanel1 = new JPanel();
	private JPanel insetsPanel2 = new JPanel();
	private JPanel insetsPanel3 = new JPanel();
	private JButton button1 = new JButton();
	private JLabel imageLabel = new JLabel();
	private JLabel label1 = new JLabel();
	private JLabel label2 = new JLabel();
	private JLabel label3 = new JLabel();
	private JLabel label4 = new JLabel();
	private ImageIcon image1 = new ImageIcon();
	private BorderLayout borderLayout1 = new BorderLayout();
	private BorderLayout borderLayout2 = new BorderLayout();
	private FlowLayout flowLayout1 = new FlowLayout();
	private GridLayout gridLayout1 = new GridLayout();
	private String product = DecodesVersion.getAbbr();
	private String version = "Version " + DecodesVersion.getVersion();
	private String copyright = "Modified " + DecodesVersion.getModifyDate();
	private String comments = labels.getString(
							"RefListFrameAboutBox.comments");

	/**
	 * Constructor.
	 * @param parent the parent frame
	 */
	public RefListFrame_AboutBox(Frame parent) 
	{
		super(parent);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		try {
			jbInit();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * No args constructor for JBuilder.
	 */
	RefListFrame_AboutBox() {
		this(null);
	}

	/** Initializes GUI components. */
	private void jbInit() throws Exception	{
		image1 = new ImageIcon(
			EnvExpander.expand("$DECODES_INSTALL_DIR/icons/gears.png"), "DECODES Icon");

		imageLabel.setIcon(image1);
		this.setTitle(genericLabels.getString("about"));
		panel1.setLayout(borderLayout1);
		panel2.setLayout(borderLayout2);
		insetsPanel1.setLayout(flowLayout1);
		insetsPanel2.setLayout(flowLayout1);
		insetsPanel2.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		gridLayout1.setRows(4);
		gridLayout1.setColumns(1);
		label1.setText(product);
		label2.setText(version);
		label3.setText(copyright);
		label4.setText(comments);
		insetsPanel3.setLayout(gridLayout1);
		insetsPanel3.setBorder(BorderFactory.createEmptyBorder(10, 60, 10, 10));
		button1.setText(genericLabels.getString("OK"));
		button1.addActionListener(e -> dispose());
		insetsPanel2.add(imageLabel, null);
		panel2.add(insetsPanel2, BorderLayout.WEST);
		this.getContentPane().add(panel1, null);
		insetsPanel3.add(label1, null);
		insetsPanel3.add(label2, null);
		insetsPanel3.add(label3, null);
		insetsPanel3.add(label4, null);
		panel2.add(insetsPanel3, BorderLayout.CENTER);
		insetsPanel1.add(button1, null);
		panel1.add(insetsPanel1, BorderLayout.SOUTH);
		panel1.add(panel2, BorderLayout.NORTH);
		setResizable(true);
	}

}
