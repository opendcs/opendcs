package org.opendcs.algorithms.update;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.opendcs.annotations.PropertySpec;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;
import org.opendcs.utils.AnnotationHelpers;

/** The installed Java class's declared roles and properties. */
public record AlgorithmSpec(List<Role> roles, List<Property> properties)
{
    public record Role(String name, String type, List<String> formerNames) {}
    public record Property(String name, String defaultValue, List<String> formerNames) {}

    public static AlgorithmSpec read(String execClass) throws ClassNotFoundException
    {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) loader = AlgorithmSpec.class.getClassLoader();
        Class<?> clazz = Class.forName(execClass, false, loader);
        List<Role> roles = new ArrayList<>();
        List<Property> properties = new ArrayList<>();
        for (var pair : AnnotationHelpers.getFieldsWithAnnotation(clazz, Input.class))
        {
            Input annotation = pair.second;
            roles.add(new Role(name(annotation.name(), pair.first), annotation.typeCode(),
                    List.of(annotation.formerNames())));
        }
        for (var pair : AnnotationHelpers.getFieldsWithAnnotation(clazz, Output.class))
        {
            Output annotation = pair.second;
            roles.add(new Role(name(annotation.name(), pair.first), annotation.typeCode(),
                    List.of(annotation.formerNames())));
        }
        for (var pair : AnnotationHelpers.getFieldsWithAnnotation(clazz, PropertySpec.class))
        {
            PropertySpec annotation = pair.second;
            properties.add(new Property(name(annotation.name(), pair.first), annotation.value(),
                    List.of(annotation.formerNames())));
        }
        if (roles.isEmpty() && properties.isEmpty())
        {
            throw new IllegalArgumentException("No update annotations in " + execClass);
        }
        validate(roles.stream().map(Role::name).toList(),
                roles.stream().flatMap(role -> role.formerNames().stream()).toList(), execClass);
        validate(properties.stream().map(Property::name).toList(),
                properties.stream().flatMap(property -> property.formerNames().stream()).toList(), execClass);
        return new AlgorithmSpec(List.copyOf(roles), List.copyOf(properties));
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
            if (name.isBlank() || !seen.add(name.toLowerCase(Locale.ROOT)))
            {
                throw new IllegalArgumentException("Duplicate or empty update name in " + execClass + ": " + name);
            }
        }
        for (String name : former)
        {
            if (name.isBlank() || !seen.add(name.toLowerCase(Locale.ROOT)))
            {
                throw new IllegalArgumentException("Conflicting former name in " + execClass + ": " + name);
            }
        }
    }
}
