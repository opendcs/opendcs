package decodes.tsdb.algo.jep;

import java.util.Stack;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

/**
 * This implements the lookupMeta(location, varname) function for the JEP parser when run inside
 * the ExpressionParserAlgorithm.
 * The function takes two arguments: location and variable name.
 */
public class LookupMetaFunction
	extends PostfixMathCommand
{
	public static final String funcName = "lookupMeta";

	public LookupMetaFunction()
	{
		super();
		this.numberOfParameters = 2;
	}
	
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void run(Stack inStack)
		throws ParseException
	{
		checkStack(inStack);
		String paramName = inStack.pop().toString();
		String locName = inStack.pop().toString();
		
		//TODO lookup the varname for the location and return the stored value.
		
//		System.out.println("lookupMetaData(loc=" + locName 
//			+ ", parm=" + paramName + ", returning 123.45");
//		inStack.push(new Double(123.45));
		
		throw new ParseException(funcName + " not yet implemented");
	}
}
