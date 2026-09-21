package org.opendcs.decodes.operations;

import org.opendcs.decodes.exec.DecodesExecutionContext;

public class SkipWhiteSpaceOperation extends AbstractDecodesOperation
{
    public static final String OPERATION_NAME = "SKIP_WHITESPACE";

    public SkipWhiteSpaceOperation()
    {
        super(1, OPERATION_NAME);
    }

    @Override
    public String toString()
    {
        return String.format("N/A -> %s", repeat, operationName());
    }

    @Override
    public void execute(DecodesExecutionContext<?> context)
    {
        System.out.println("fine the next line");
        
    }
}
