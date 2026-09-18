package org.opendcs.decodes.operations;

public abstract class AbstractDecodesOperation implements DecodesOperation
{
    final int repeat;
    final String name;

    protected AbstractDecodesOperation(int repeat, String name)
    {
        this.repeat = repeat;
        this.name = name;
    }


    @Override
    public String operationName()
    {
        return this.name;
    }

    @Override
    public int repeat()
    {
        return this.repeat;
    }

    @Override
    public String toString()
    {
        return String.format("%d -> %s", repeat, name);
    }
}
