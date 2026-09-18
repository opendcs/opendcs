package org.opendcs.decodes.operations;

public class SkipLineOperation extends AbstractDecodesOperation
{
    public static final String OPERATION_NAME = "SKIP_LINE";

    private final SKIP_DIRECTION direction;

    public SkipLineOperation(int repeat, SKIP_DIRECTION direction)
    {
        super(repeat, OPERATION_NAME);
        this.direction = direction;
    }

    @Override
    public String toString()
    {
        return String.format("%d -> %s:%s", repeat, operationName(), direction);
    }
}
