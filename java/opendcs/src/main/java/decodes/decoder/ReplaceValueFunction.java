package decodes.decoder;

import ilex.util.Logger;

import java.util.StringTokenizer;

import org.python.antlr.PythonParser.atom_return;

import decodes.db.DecodesScript;

/**
 * Decodes function used to replace a symbol with another value.
 * example:  replaceValue(T,0.0)   # replaces 'T' with 0.0  
 */
public class ReplaceValueFunction extends DecodesFunction
{
	public static final String module = "replaceWith";
	
	public ReplaceValueFunction()
	{
	}

	@Override
	public DecodesFunction makeCopy()
	{
		return new ReplaceValueFunction();
	}

	@Override
	public String getFuncName()
	{
		return module;
	}

	@Override
	public void execute(DataOperations dd, DecodedMessage msg) throws DecoderException
	{
		// Execute doesn't do anything. Work is done in setArguments
	}

	/**
	 * get arguments from: replaceValue(find,replace)
	 */
	@Override
	public void setArguments(String argString, DecodesScript script) throws ScriptFormatException
	{
		Logger.instance().info(argString);
		StringTokenizer st = new StringTokenizer(argString, ",");
		while(st.hasMoreTokens())
		{
			String find = st.nextToken();
			find = find.trim();
			if( st.hasMoreTokens())
			{
		        String replace = st.nextToken();
			    replace = replace.trim();
				script.addReplace(find,replace);
			}

		}
	}

}
