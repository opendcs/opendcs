package org.opendcs.decodes.operations;

public enum SKIP_DIRECTION
{
    FORWARD(1),
    BACKWARDS(-1);

    public final int multiplier;

    SKIP_DIRECTION(int i) 
    {
        multiplier = i;
    }
}