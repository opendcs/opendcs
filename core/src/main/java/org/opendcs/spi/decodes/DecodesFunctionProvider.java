package org.opendcs.spi.decodes;

import decodes.decoder.DecodesFunction;

public interface DecodesFunctionProvider
{
    /**
     * Name of the decodes function that will be used in a DecodesScript.
     * The name is case sensitive. If you function is provided outside of the 
     * OpenDCS distribution, please prefix the name with some sort of organizational identifier.
     * @return
     */
    String getName();

    /**
     * Create an actual instance of your custom decodes function.
     * @return Valid and immediately usable instance of a DecodesFunction.
     */
    DecodesFunction createInstance();
}
