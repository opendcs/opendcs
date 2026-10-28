package org.opendcs.database.dai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class TimeSeriesIdentifierDaoTest
{
    @Test
    void extractsDisplayNameWithoutTrailingIdentifierWhitespace()
    {
        var dao = (TimeSeriesIdentifierDao)Proxy.newProxyInstance(
            TimeSeriesIdentifierDao.class.getClassLoader(),
            new Class<?>[] {TimeSeriesIdentifierDao.class},
            (proxy, method, args) -> InvocationHandler.invokeDefault(proxy, method, args));

        var result = dao.extractDisplayName(
            "Alder Springs.Precip.Total.1Hour.1Hour.comp (Hourly Precip for Alder Springs)");

        assertEquals("Alder Springs.Precip.Total.1Hour.1Hour.comp", result.first);
        assertEquals(Optional.of("Hourly Precip for Alder Springs"), result.second);
    }
}
