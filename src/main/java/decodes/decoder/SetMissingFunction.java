package decodes.decoder;

import ilex.util.Logger;

import java.util.StringTokenizer;

import decodes.db.DecodesScript;

public class SetMissingFunction extends DecodesFunction
{
	public static final String module = "setMissing";
	
	public SetMissingFunction()
	{
	}

	@Override
	public DecodesFunction makeCopy()
	{
		return new SetMissingFunction();
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

	@Override
	public void setArguments(String argString, DecodesScript script) throws ScriptFormatException
	{
		StringTokenizer st = new StringTokenizer(argString, ",");
		while(st.hasMoreTokens())
		{
			String t = st.nextToken();
			t = t.trim();
			script.addMissing(t);
			Logger.instance().debug1(module + ": " + t);
		}
	}

}
