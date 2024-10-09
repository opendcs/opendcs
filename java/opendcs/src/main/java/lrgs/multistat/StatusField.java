package lrgs.multistat;

import javax.swing.JTextField;
import java.awt.Font;
import java.awt.Color;

public class StatusField extends JTextField
{
	private static Font font = new java.awt.Font("Dialog", 1, 15);

    public StatusField()
    {
		setFont(font);
		setBackground(Color.black);
		setEditable(false);
		setOk();
    }

	public void setWarning()
	{
		setForeground(Color.yellow);
	}
	public void setOk()
	{
		setForeground(Color.green);
	}
	public void setError()
	{
		setForeground(Color.red);
	}
}
