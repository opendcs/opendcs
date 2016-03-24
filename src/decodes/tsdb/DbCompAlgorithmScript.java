package decodes.tsdb;

import hec.util.TextUtil;
import ilex.util.Logger;

/**
 * For Python Algorithms, this class will be associated with the DbCompAlgorithm
 * and will hold the information in the CP_ALGO_SCRIPT table.
 */
public class DbCompAlgorithmScript
{
	private DbCompAlgorithm parent = null;
	private String text = "";
	private ScriptType scriptType = ScriptType.Undefined;

	public DbCompAlgorithmScript(DbCompAlgorithm parent, ScriptType scriptType)
	{
		super();
		this.parent = parent;
		this.scriptType = scriptType;
	}
	
	public String getText() { return text; }
	
	public void addToText(String block)
	{
		if (text == null || text.length() == 0)
			text = block;
		else
			text = text + block;
Logger.instance().debug1("DbCompAlgorithmScript.addToScript, after adding, text='" + text + "'");
	}

	public ScriptType getScriptType()
	{
		return scriptType;
	}

	public DbCompAlgorithm getParent()
	{
		return parent;
	}

	public DbCompAlgorithmScript copy(DbCompAlgorithm newAlgo)
	{
		DbCompAlgorithmScript ret = new DbCompAlgorithmScript(newAlgo, this.scriptType);
		ret.text = this.text;
		return ret;
	}
	
	@Override
	public boolean equals(Object rhs)
	{
		if (!(rhs instanceof DbCompAlgorithmScript))
			return false;
		DbCompAlgorithmScript rhss = (DbCompAlgorithmScript)rhs;
		return scriptType == rhss.scriptType && TextUtil.equals(text, rhss.text);
	}
}
