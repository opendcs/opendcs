/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
* 
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations 
* under the License.
*/
package decodes.dbeditor;

import javax.swing.JComboBox;

import decodes.db.Constants;

/**
Combo box for canned 'Recording Mode' selection (Fixed or Variable).
*/
public class RecordingModeCombo extends JComboBox
{
	public static final String modeFixed = "Fixed";
	public static final String modeVariable = "Variable";

	static String types[] = { modeFixed, modeVariable };

	/** Constructor. */
    public RecordingModeCombo()
	{
		super(types);
    }

	/**
	  Sets the current selection.
	  @param recordingMode the current selection.
	*/
	public void setSelection(char recordingMode)
	{
		if (Character.toLowerCase(recordingMode)
		 == Character.toLowerCase(Constants.recordingModeVariable))
			this.setSelectedItem(modeVariable);
		else
			this.setSelectedItem(modeFixed);
	}

	/**
	  @return the current selection.
	*/
	public char getSelection()
	{
		if (this.getSelectedItem() == modeVariable)
		{
			return Constants.recordingModeVariable;
		}
		else
		{
			return Constants.recordingModeFixed;
		}
	}
}
