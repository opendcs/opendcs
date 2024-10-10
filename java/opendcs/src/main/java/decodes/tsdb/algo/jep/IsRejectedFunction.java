package decodes.tsdb.algo.jep;

import java.util.Stack;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

import decodes.cwms.CwmsFlags;

/**
 * Returns true if the passed integer flags value is marked as REJECTED.
 */
public class IsRejectedFunction
	extends PostfixMathCommand
{
	public static final String funcName = "isRejected";
	private JepContext ctx;
	
	public IsRejectedFunction(JepContext ctx)
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
		
		Object o = inStack.pop();
		if (!(o instanceof Number))
		{
			throw new ParseException(funcName + " must be passed 'parmname.flags'. Value passed was: "
				+ o.toString() + " with type " + o.getClass().getName());
		}
		int flags = ((Number)o).intValue();
		inStack.push(
			Double.valueOf(
				(flags & CwmsFlags.VALIDITY_MASK) == CwmsFlags.VALIDITY_REJECTED ? 1.0 : 0.0));
	}
}
