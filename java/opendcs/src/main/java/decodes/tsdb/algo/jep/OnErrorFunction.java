package decodes.tsdb.algo.jep;

import ilex.util.Logger;

import java.util.Stack;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

/**
 * This sets the onErrorLabel in the context and then returns 0.
 * Subsequently, if any expression results in an error, execution jumps to the
 * specified label.
 */
public class OnErrorFunction
	extends PostfixMathCommand
{
	public static final String funcName = "onError";
	private JepContext ctx;
	
	public OnErrorFunction(JepContext ctx)
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
		String label = inStack.pop().toString();
		ctx.setOnErrorLabel(label);
		inStack.push(Double.valueOf(0.0));
	}
}
