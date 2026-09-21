package org.opendcs.decodes.operations;

import java.util.List;

import org.opendcs.decodes.exec.DecodesExecutionContext;

public class FunctionDecodesOperation extends AbstractDecodesOperation
{
    private final List<String> arguments;

    public FunctionDecodesOperation(int repeat, String name, List<String> arguments)
    {
        super(repeat, name);
        this.arguments = arguments;
    }

    @Override
    public String toString()
    {
        return String.format("%d -> %s(%s)", repeat, operationName(), String.join(",", arguments));
    }

    @Override
    public void execute(DecodesExecutionContext<?> context)
    {
        // lookup the actual impl and execute
        System.out.println("Executing " + this.toString());
    }
}
