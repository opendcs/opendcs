package org.opendcs.webui.themes;

public final class Theme
{
    public final String name;
    public final String link;
 
    public Theme(String name, String link)
    {
        this.name = name;
        this.link = link;
    }

    public String getName()
    {
        return name;
    }

    public String getLink()
    {
        return link;
    }
}
