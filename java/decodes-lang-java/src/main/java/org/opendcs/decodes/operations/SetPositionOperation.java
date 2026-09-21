package org.opendcs.decodes.operations;

import org.opendcs.decodes.exec.DecodesExecutionContext;

public class SetPositionOperation extends AbstractDecodesOperation
{
    public final static String OPERATION_NAME = "SET_POSITION";

    public SetPositionOperation(int repeat)
    {
        super(repeat, OPERATION_NAME);
    }
    
    @Override
    public String toString()
    {
        return String.format("N/A -> %s:%d", operationName(), repeat);
    }

    @Override
    public void execute(DecodesExecutionContext<?> context)
    {
        // determine the start of this line (previous newline?) and advance
        // from there.
        System.out.println("Setting to " + repeat + " from start of line");
    }
}
