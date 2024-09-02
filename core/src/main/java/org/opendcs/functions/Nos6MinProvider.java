package org.opendcs.functions;

import org.opendcs.spi.decodes.DecodesFunctionProvider;

import decodes.decoder.DecodesFunction;
import decodes.decoder.Nos6Min;

public class Nos6MinProvider implements DecodesFunctionProvider
{

    @Override
    public String getName()
    {
        return Nos6Min.module;
    }

    @Override
    public DecodesFunction createInstance() 
    {
        return new Nos6Min(); 
    }

}
