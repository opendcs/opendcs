package decodes.decoder;

import ilex.util.Logger;

import java.util.StringTokenizer;

import decodes.db.DecodesScript;

/**
 * Decodes function used to replace a symbol with another value.
 * example:  replaceValueWith(T,0.0)   # replaces 'T' with 0.0  
 */
public class ReplaceValueWithFunction extends DecodesFunction
{
	public static final String module = "replaceValueWith";
	private String find;
	private String replace;
	private DecodesScript script;

	
	public ReplaceValueWithFunction()
	{
	}

	@Override
	public DecodesFunction makeCopy()
	{
		return new ReplaceValueWithFunction();
	}

	@Override
	public String getFuncName()
	{
		return module;
	}

	@Override
	public void execute(DataOperations dd, DecodedMessage msg) throws DecoderException
	{
		script.addReplace(find,replace);
	}

	/**
	 * get arguments from: replaceValue(find,replace)
	 */
	@Override
	public void setArguments(String argString, DecodesScript script) throws ScriptFormatException
	{
		Logger.instance().debug1(argString);
		String[] args = argString.split(",");
		if( args.length == 2)
		{
			find = args[0].trim();
			replace = args[1].trim();
			this.script = script;
		}
		else
		{
			throw new ScriptFormatException(module + " requires two arguments");
		}
 
	}

}
