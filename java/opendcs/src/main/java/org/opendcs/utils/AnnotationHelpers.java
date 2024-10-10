package org.opendcs.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import ilex.util.Pair;

public class AnnotationHelpers {
    
    private AnnotationHelpers()
    {
        // prevent instance of class
    }

    public static <AnnotationType extends Annotation> List<Pair<Field,AnnotationType>> getFieldsWithAnnotation(Class<?> clazz, Class<AnnotationType> annotationType)
    {
        ArrayList<Pair<Field,AnnotationType>> ret = new ArrayList<>();
        Field[] fields = clazz.getFields();
        for (Field f: fields)
        {
            if (f.isAnnotationPresent(annotationType))
            {
                ret.add(new Pair<>(f, (AnnotationType)f.getAnnotation(annotationType)));
            }
        }
        return ret;
    }
}
