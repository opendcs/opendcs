package ilex.gui;

import javax.swing.JTextField;

public class ChangeTrackTextField
	extends JTextField
{
	private String origText;

	public ChangeTrackTextField()
	{
		super();
		origText = "";
	}

	public ChangeTrackTextField(String v)
	{
		super(v);
		setOrigText(v);
	}

	public void setOrigText(String v)
	{
		setText(v);
		origText = v;
	}

	public boolean isChanged()
	{
		return origText.equals(getText().trim());
	}
}
