package org.opendcs.functions;

import org.opendcs.spi.decodes.DecodesFunctionProvider;

import decodes.decoder.DecodesFunction;
import decodes.decoder.LatLonFunction;

public class LatLonFunctionProvider implements DecodesFunctionProvider
{

    @Override
    public String getName()
    {
        return LatLonFunction.module;
    }

    @Override
    public DecodesFunction createInstance() 
    {
        return new LatLonFunction(); 
    }

}
