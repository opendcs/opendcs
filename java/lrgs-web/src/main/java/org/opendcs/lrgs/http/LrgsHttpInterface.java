package org.opendcs.lrgs.http;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

/**
 * Compatiblity layer for transitioning older configurations
 * LrgsHttpInterface 
 */
public class LrgsHttpInterface extends LrgsHttpInput
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();

    public LrgsHttpInterface()
    {
        log.warn("LrgsHttpInterface has been replaced by the properly named LrgsHttpInput. Please change your configuration.");
    }
}
