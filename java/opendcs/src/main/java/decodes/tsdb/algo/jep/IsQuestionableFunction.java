package decodes.tsdb.algo.jep;

import java.util.Stack;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

import decodes.cwms.CwmsFlags;

/**
 * Returns true if the passed integer flags value is marked as QUESTIONABLE.
 */
public class IsQuestionableFunction
	extends PostfixMathCommand
{
	public static final String funcName = "isQuestionable";
	private JepContext ctx;
	
	public IsQuestionableFunction(JepContext ctx)
	{
		super();
		this.ctx = ctx;
		this.numberOfParameters = 1;
	}
	
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void run(Stack inStack)
		throws ParseException
	{
		checkStack(inStack);
		
		Object o = inStack.pop().toString();
		if (!(o instanceof Number))
		{
			throw new ParseException(funcName + " must be passed parmname.flags.");
		}
		int flags = ((Number)o).intValue();
		inStack.push(
			Double.valueOf(
				(flags & CwmsFlags.VALIDITY_MASK) == CwmsFlags.VALIDITY_QUESTIONABLE ? 1.0 : 0.0));
	}
}
