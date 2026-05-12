package org.opendcs.model.cwms;

import java.util.List;

import decodes.db.Site;

/**
 * Holds property names that are used by the CWMS Sites (Locations) table but would
 * otherwise go into site_property within the OpenDCS Schema.
 */
public final class CwmsSite extends Site
{
    public static final String HORIZONTAL_DATUM = "horizontal_datum";
    public static final String VERTICAL_DATUM = "vertical_datum";
    public static final String BOUNDING_OFFICE = "bounding_office";

    public static final List<String> CWMS_SITE_PROPERTIES =
        List.of(HORIZONTAL_DATUM, VERTICAL_DATUM, BOUNDING_OFFICE);

    private CwmsSite()
    {
        /* class is for future expansion and for the moment, to hold some constants. */
    }

}
