package org.opendcs.decodes.functions;

import org.opendcs.spi.decodes.DecodesFunctionProvider;

import decodes.decoder.DecodesFunction;
import decodes.decoder.NosHourly;

public class NosHourlyProvider implements DecodesFunctionProvider
{

    @Override
    public String getName()
    {
        return NosHourly.module;
    }

    @Override
    public DecodesFunction createInstance() 
    {
        return new NosHourly(); 
    }

}
