package org.opendcs.webui.themes;

import java.util.stream.Stream;

import com.google.auto.service.AutoService;

@AutoService(ThemeSet.class)
public class DefaultTheme implements ThemeSet
{

    @Override
    public Stream<Theme> getThemes()
    {
        return Stream.of(new Theme("Default", "/webjars/bootstrap/css/bootstrap.min.css"));
    }
}
