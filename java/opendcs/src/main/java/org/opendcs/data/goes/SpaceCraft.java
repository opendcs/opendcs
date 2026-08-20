package org.opendcs.data.goes;

public enum SpaceCraft
{
    EAST("E"),
    WEST("W"),
    ALL("A");

    private final String craft;

    SpaceCraft(String craft)
    {
        this.craft = craft;
    }
    
    @Override
    public String toString()
    {
        return craft;
    }
    
    public char toChar()
    {
        return craft.charAt(0);
    }
}
