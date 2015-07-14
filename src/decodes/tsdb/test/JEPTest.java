package decodes.tsdb.test;

import ilex.util.TextUtil;

import java.util.Observable;
import java.util.Observer;
import java.util.Stack;

import org.nfunk.jep.EvaluatorI;
import org.nfunk.jep.JEP;
import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.ParserVisitor;
import org.nfunk.jep.SymbolTable;
import org.nfunk.jep.Variable;
import org.nfunk.jep.function.CallbackEvaluationI;
import org.nfunk.jep.function.PostfixMathCommand;

public class JEPTest
	implements Observer
{
	JEP jep = new JEP();
	
	public void run()
	{
		
		jep.addStandardFunctions();
		jep.addStandardConstants();
		jep.setAllowAssignment(true);
		jep.setAllowUndeclared(true);
		jep.addFunction("lookupMeta", new LookupMeta());
		jep.addFunction("cond", new Condition());
		jep.getSymbolTable().addObserver(this);
		
		while(true)
		{
			System.out.print("Enter expression: ");
			String line = System.console().readLine();
			jep.parseExpression(line);
			Object value = jep.getValueAsObject();

			if (jep.hasError() || value == null)
				System.out.println("Error: " + jep.getErrorInfo());
			else
			{
				if (value != null)
					System.out.println("Result type=" + value.getClass().getName());
				System.out.println("Result=" + value);
			}
		}

	}

	public static void main(String[] args)
	{
		new JEPTest().run();

	}

	@Override
	public void update(Observable o, Object arg)
	{
        if (o instanceof Variable)
            System.out.println("Var changed: o=" + o + ", arg=" + arg);
        else if(o instanceof SymbolTable.StObservable)
        {
        	SymbolTable.StObservable obs = (SymbolTable.StObservable)o;
        	System.out.println("New var: "+arg);
        	System.out.println("Type of arg is " + arg.getClass().getName());
        	Variable v = (Variable)arg;
        	v.setValue(new Double(0.0));
//        	jep.getSymbolTable().setVarValue(v.getName(), new Double(0.0));

            // This line is vital to ensure that 
            // any new variable created will be observed. 
           ((Variable) arg).addObserver(this);
        }
	}
}

class LookupMeta extends PostfixMathCommand
{
	/**
	 * Metadata params are Location and Param Name
	 */
	public LookupMeta()
	{
		numberOfParameters = 2;
	}
	
	public void run(Stack inStack)
		throws ParseException
	{
		System.out.println("lookupMeta, stack.size=" + inStack.size());
		checkStack(inStack);
		String paramName = inStack.pop().toString();
		String locName = inStack.pop().toString();
		
		System.out.println("lookupMetaData(loc=" + locName 
			+ ", parm=" + paramName + ", returning 123.45");
		inStack.push(new Double(123.45));	
	}
}

class ConditionFailed extends ParseException
{
	public ConditionFailed(String msg)
	{
		super(msg);
	}
}

class Condition
	extends PostfixMathCommand
	implements CallbackEvaluationI
{
	public Condition()
	{
		super();
		this.numberOfParameters = 2;
	}
	
//	public Node process(Node node, Object data, ParserVisitor pv)
//		throws ParseException
//	{
//System.out.println("Condition.process called");
//		return null;
//	}
	public boolean checkNumberOfParameters(int n)
	{
		return n == 2;
	}
	
	@Override
	public Object evaluate(Node node, EvaluatorI evaluator) throws ParseException
	{
System.out.println("Condition.evaluate called");
		if (!checkNumberOfParameters(node.jjtGetNumChildren()))
			throw new ParseException("cond function requires 2 arguments!");
		
		// Evaluate the condition
		Object condResult = evaluator.eval(node.jjtGetChild(0));
		boolean tf = isTrue(condResult);
		System.out.println("Condition.evaluate result is " + tf);
		if (!tf)
		{
System.out.println("false -- throwing ParseException");
			throw new ParseException("Condition is false.");
		}
//			throw new ConditionFailed("Condition is false.");
		else
		{
System.out.println("Evaluating second arg");
			return evaluator.eval(node.jjtGetChild(1));
		}
	}

	private boolean isTrue(Object result)
		throws ParseException
	{
		if (result instanceof Boolean)
			return (Boolean)result;
		else if (result instanceof Number)
		{
			Double d = ((Number)result).doubleValue();
System.out.println("result of condition is numeric: " + d);
			return d < -0.0000001 || d > 0.0000001;
		}
		else if (result instanceof String)
			return TextUtil.str2boolean((String)result);

		else
			throw new ParseException("Condition must result in boolean or number!");
	}
}


