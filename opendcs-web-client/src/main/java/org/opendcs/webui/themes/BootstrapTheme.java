package org.opendcs.webui.themes;

import java.util.stream.Stream;

import com.google.auto.service.AutoService;

@AutoService(ThemeSet.class)
public class BootstrapTheme implements ThemeSet
{

    @Override
    public Stream<Theme> getThemes()
    {
        return Stream.of(new Theme("Bootstrap (Default)", "/webjars/bootstrap/css/bootstrap.min.css"));
    }
}
