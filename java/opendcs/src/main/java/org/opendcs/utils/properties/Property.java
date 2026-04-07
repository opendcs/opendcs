package org.opendcs.utils.properties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.opendcs.annotations.PropertySpec;
import org.opendcs.utils.properties.conversion.NoPropertyConverterException;
import org.opendcs.utils.properties.conversion.PropertyConverter;

import ilex.util.EnvExpander;
import ilex.util.HasProperties;

/**
 * Establish a known Property value that is to be retrieved.
 * 
 * The get and find methods *always* lookup the value when called. So the created instances
 * can be stored to retrieve at different times.
 */
public final class Property<T>
{
    final T defaultValue;
    final Date expansionDate; // if not set use new Date at time of expansion
    final Properties expansionSource;
    final boolean mustExpand;
    final PropertySpec propertySpec;
    final String propertyName;
    final Class<T> targetType;
    final List<? extends HasProperties> sources;
    final PropertyConverter<T> converter;


    private Property(Builder<T> builder)
    {
        this.propertyName = builder.propertyName;
        this.targetType = builder.targetType;
        this.expansionDate = builder.expansionDate;
        this.defaultValue = builder.defaultValue;
        this.expansionSource = builder.expansionSource;
        this.mustExpand = builder.mustExpand;
        this.propertySpec = builder.propertySpec;
        this.sources = builder.sources;
        this.converter = builder.converter;
    }

    // For both of the below, it may be better to allow the expansion sources to be
    // provided here vs keeping it as the default, otherwise that data could become stale
    // Alternatively perhaps those usages would just keep the builder around instead.


    /**
     * Retrieve expected property.
     * @return The value expected
     * @throws NoSuchPropertyException
     */
    public T get() throws NoSuchPropertyException
    {
        return find().orElseThrow(() -> new NoSuchPropertyException("No property avaiabled named '" + propertyName + "' from any configured source"));
    }

    /**
     * Return property that may not exist.
     * @return
     */
    public Optional<T> find()
    {
        return findValue();
    }
    
    private Optional<T> findValue()
    {
        T ret = this.defaultValue;

        for (var source: sources)
        {
            String value = source.getProperty(propertyName);
            if (value != null)
            {
                ret = converter.fromString(expand(value));
                break; // break out of the loop, we found the value we want
            }   
        }

        return Optional.ofNullable(ret);
    }

    private String expand(String value)
    {
        String ret = value;
        if (mustExpand)
        {
            ret = EnvExpander.expand(value, this.expansionSource, this.expansionDate);
        }
        return ret;
    }

    @SuppressWarnings("java:S2972") // it doesn't really make sense to me to split this class out, while a bit long, it is simple.
    public static final class Builder<T>
    {
        T defaultValue;
        Date expansionDate = null; // if not set use new Date at time of expansion
        Properties expansionSource = null;
        boolean mustExpand = false;
        PropertySpec propertySpec = null;
        String propertyName;
        Class<T> targetType;
        final List<HasProperties> sources = new ArrayList<>();
        PropertyConverter<T> converter;
    

        private Builder(String propertyName, Class<T> targetType)
        {
            this.propertyName = propertyName;
            this.targetType = targetType;
        }

        
        /**
         * Include System.getProperties and System.getenv in the possible sources.
         * These will be first in the order of operations. If a different order is desired
         * manually establish the order with {@see withSources} and the {@see PropertySource} and {@see EnvSource} instances
         * @return
         */
        Builder<T> useSystemAndEnv()
        {
            return this;
        }

        /**
         * Possible Sources in order of priority
         * @param <U>
         * @param sources
         * @return
         */
        @SafeVarargs
        @SuppressWarnings("java:S2333") // modifier required to satisfy @SafeVarargs validation
        public final <U extends HasProperties> Builder<T> withSources(U... sources)
        {
            return withSources(Arrays.asList(sources));
        }

        public Builder<T> withSources(List<? extends HasProperties> sources)
        {
            this.sources.addAll(sources);
            return this;
        }

        /**
         * Establish a default value. If not provided default is null, unless PropertySpec
         * is set, in which case that default is used.
         * @param value
         * @return
         */
        public Builder<T> withDefaultValue(T value)
        {
            this.defaultValue = value;
            return this;
        }

        /**
         * Establish matching property spec to handle conversions.
         * @param spec
         * @return
         */
        public Builder<T> withPropertySpec(PropertySpec spec)
        {
            this.propertySpec = spec;
            return this;
        }

        /**
         * Returned value will use the EnvExpander
         * @return
         */
        public Builder<T> expand()
        {
            this.mustExpand = true;
            return this;
        }

        public Builder<T> expand(Properties source)
        {
            this.expansionSource = source;
            return expand();
        }        

        public Builder<T> expand(Properties source, Date date)
        {
            this.expansionDate = date;
            return expand(source);
        }

        /**
         * For those occasions when you know what to String to T converter to use.
         * If not specified an attempt will be made to look one up.
         * @param converter
         * @return
         */
        public Builder<T> withConverter(PropertyConverter<T> converter)
        {
            this.converter = converter;
            return this;
        }
        
        public Property<T> build()
        {
            if (converter == null)
            {
                withConverter(lookupConverter());
            }

            return new Property<>(this);
        }


        private PropertyConverter<T> lookupConverter()
        {
            PropertyConverter<T> ret = PropertyConverter.forType(targetType);
            if (ret == null)
            {
                throw new NoPropertyConverterException("Unable to find property converter for type " + this.targetType.getName());
            }
            return ret;
        }
    }


    public static <T> Builder<T> property(String name, Class<T> type)
    {
        return new Builder<>(name, type);
    }

    public static <T> Builder<T> property(PropertySpec spec)
    {
        // should lookup the type from the spec
        return new Builder<>(spec.name(), null);
    }
}
