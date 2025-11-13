package org.opendcs.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import ilex.util.Pair;

public final class AnnotationHelpers
{
    private AnnotationHelpers()
    {
        // prevent instance of class
    }

    /**
     * Get all of the requested Annotation for the given class, including it's parents
     * @param <AnnotationType> Which Annotation desired
     * @param clazz target Class to search for anntotations
     * @param annotationType Which Annotation desired
     * @return List of Annotation instance, may be empty.
     */
    public static <AnnotationType extends Annotation> List<Pair<Field,AnnotationType>> getFieldsWithAnnotation(Class<?> clazz, Class<AnnotationType> annotationType)
    {
        ArrayList<Pair<Field,AnnotationType>> ret = new ArrayList<>();
        return getFieldsWithAnnotation(ret, clazz, annotationType);
    }

    /**
     * Recursive method to retrieve annotations.
     * @param <AnnotationType>
     * @param currentList
     * @param clazz
     * @param annotationType
     * @return
     */
    private static <AnnotationType extends Annotation> List<Pair<Field,AnnotationType>> getFieldsWithAnnotation(ArrayList<Pair<Field,AnnotationType>> currentList, Class<?> clazz, Class<AnnotationType> annotationType)
    {
        if (clazz != null && clazz != Object.class)
        {
            Field[] fields = clazz.getDeclaredFields();
            for (Field f: fields)
            {
                if (f.isAnnotationPresent(annotationType))
                {
                    currentList.add(new Pair<>(f, (AnnotationType)f.getAnnotation(annotationType)));
                }
            }
            return getFieldsWithAnnotation(currentList, clazz.getSuperclass(), annotationType);
        }
        return currentList;
    }
}
