package org.opendcs.fixtures;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Tell the test extension that test will require an
 * instance of this resource.
 *
 * Currently supported are:
 *
 * - @see org.opendcs.fixtures.spi.configuration.Configuration
 * - @see decodes.tsdb.TimeSeriesDb provided by the above configuration
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfiguredField
{
}
