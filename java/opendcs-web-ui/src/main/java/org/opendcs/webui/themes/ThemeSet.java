package org.opendcs.webui.themes;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface ThemeSet
{
    Stream<Theme> getThemes();
    

    static List<Theme> getAllThemes()
    {
        ServiceLoader<ThemeSet> themeSets = ServiceLoader.load(ThemeSet.class);
        final List<Theme> ret = new ArrayList<>();
        themeSets.forEach(ts -> ts.getThemes()
                 .collect(Collectors.toCollection(() -> ret)));
        return ret;
    }
}
