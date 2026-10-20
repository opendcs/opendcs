package org.opendcs.algorithms.update;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import ilex.util.Pair;

import org.opendcs.annotations.PropertySpec;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;
import org.opendcs.utils.AnnotationHelpers;

/** The installed Java class's declared roles and properties. */
public final class AlgorithmSpec
{
    private final List<Role> roles;
    private final List<Property> properties;

    public AlgorithmSpec(List<Role> roles, List<Property> properties)
    {
        this.roles = roles;
        this.properties = properties;
    }

    public List<Role> roles() { return roles; }

    public List<Property> properties() { return properties; }

    public static final class Role
    {
        private final String name;
        private final String type;
        private final List<String> formerNames;

        public Role(String name, String type, List<String> formerNames)
        {
            this.name = name;
            this.type = type;
            this.formerNames = formerNames;
        }

        public String name() { return name; }

        public String type() { return type; }

        public List<String> formerNames() { return formerNames; }

    }

    public static final class Property
    {
        private final String name;
        private final String defaultValue;
        private final List<String> formerNames;

        public Property(String name, String defaultValue, List<String> formerNames)
        {
            this.name = name;
            this.defaultValue = defaultValue;
            this.formerNames = formerNames;
        }

        public String name() { return name; }

        public String defaultValue() { return defaultValue; }

        public List<String> formerNames() { return formerNames; }

    }

    public static AlgorithmSpec read(String execClass) throws ClassNotFoundException
    {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null)
        {
            loader = AlgorithmSpec.class.getClassLoader();
        }
        Class<?> clazz = Class.forName(execClass, false, loader);
        List<Role> roles = new ArrayList<>();
        List<Property> properties = new ArrayList<>();
        for (Pair<Field, Input> pair : AnnotationHelpers.getFieldsWithAnnotation(clazz, Input.class))
        {
            Input annotation = pair.second;
            roles.add(new Role(name(annotation.name(), pair.first), annotation.typeCode(),
                    Arrays.asList(annotation.formerNames())));
        }
        for (Pair<Field, Output> pair : AnnotationHelpers.getFieldsWithAnnotation(clazz, Output.class))
        {
            Output annotation = pair.second;
            roles.add(new Role(name(annotation.name(), pair.first), annotation.typeCode(),
                    Arrays.asList(annotation.formerNames())));
        }
        for (Pair<Field, PropertySpec> pair : AnnotationHelpers.getFieldsWithAnnotation(clazz, PropertySpec.class))
        {
            PropertySpec annotation = pair.second;
            properties.add(new Property(name(annotation.name(), pair.first), annotation.value(),
                    Arrays.asList(annotation.formerNames())));
        }
        if (roles.isEmpty() && properties.isEmpty())
        {
            throw new IllegalArgumentException("No update annotations in " + execClass);
        }
        validate(roles.stream().map(Role::name).collect(Collectors.toList()),
                roles.stream().flatMap(role -> role.formerNames().stream()).collect(Collectors.toList()), execClass);
        validate(properties.stream().map(Property::name).collect(Collectors.toList()),
                properties.stream().flatMap(property -> property.formerNames().stream()).collect(Collectors.toList()), execClass);
        return new AlgorithmSpec(Collections.unmodifiableList(roles), Collections.unmodifiableList(properties));
    }

    private static String name(String annotated, Field field)
    {
        return annotated.isEmpty() ? field.getName() : annotated;
    }

    private static void validate(List<String> current, List<String> former, String execClass)
    {
        Set<String> seen = new HashSet<>();
        for (String name : current)
        {
            if (name.trim().isEmpty() || !seen.add(name.toLowerCase(Locale.ROOT)))
            {
                throw new IllegalArgumentException("Duplicate or empty update name in " + execClass + ": " + name);
            }
        }
        for (String name : former)
        {
            if (name.trim().isEmpty() || !seen.add(name.toLowerCase(Locale.ROOT)))
            {
                throw new IllegalArgumentException("Conflicting former name in " + execClass + ": " + name);
            }
        }
    }
}
