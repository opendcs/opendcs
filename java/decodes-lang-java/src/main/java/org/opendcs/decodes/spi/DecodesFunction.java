package org.opendcs.decodes.spi;

import java.util.List;

import org.opendcs.decodes.exec.DecodesExecutionContext;
import org.opendcs.decodes.operations.FunctionDecodesOperation;

public abstract class DecodesFunction extends FunctionDecodesOperation
{
    protected DecodesFunction(String name, List<String> arguments)
    {
        // repeat is handled by the decodes engine, not
        // specific function implementation
        super(0, name, arguments);
        
    }

    public abstract void execute(DecodesExecutionContext<?> context);
}