package decodes.tsdb.algo.jep;

import java.util.Stack;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

/**
 * This sets the gotoLabel in the context and then returns 0.
 */
public class GotoFunction
	extends PostfixMathCommand
{
	private JepContext ctx;
	
	public GotoFunction(JepContext ctx)
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
		ctx.setGotoLabel(label);
		inStack.push(new Double(0.0));
	}
}
