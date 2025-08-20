package org.opendcs.decodes.functions;

import org.opendcs.spi.decodes.DecodesFunctionProvider;

import decodes.decoder.CsvFunction;
import decodes.decoder.DecodesFunction;

public class CsvFunctionProvider implements DecodesFunctionProvider
{

    @Override
    public String getName()
    {
        return CsvFunction.module;
    }

    @Override
    public DecodesFunction createInstance() 
    {
        return new CsvFunction(); 
    }

}
