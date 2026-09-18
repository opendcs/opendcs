package org.opendcs.decodes.operations;

import java.util.List;

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
}
