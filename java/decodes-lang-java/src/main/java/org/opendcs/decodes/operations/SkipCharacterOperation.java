package org.opendcs.decodes.operations;

import org.opendcs.decodes.exec.DecodesExecutionContext;

public class SkipCharacterOperation extends AbstractDecodesOperation
{
    public final static String OPERATION_NAME = "SKIP_CHARACTERS";


    private final SKIP_DIRECTION direction;

    public SkipCharacterOperation(int repeat, SKIP_DIRECTION direction)
    {
        super(repeat, OPERATION_NAME);
        this.direction = direction;
    }
    
    @Override
    public String toString()
    {
        return String.format("%d -> %s:%s", repeat, operationName(), direction);
    }

    @Override
    public void execute(DecodesExecutionContext<?> context)
    {
        context.getMessageData();
        context.advancePosition(repeat*direction.multiplier);
    }
}
