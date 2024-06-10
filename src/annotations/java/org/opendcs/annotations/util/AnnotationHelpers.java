package org.opendcs.annotations.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class AnnotationHelpers {
    
    private AnnotationHelpers()
    {
        // prevent instance of class
    }

    public static List<Field> getFieldsWithAnnotation(Class<?> clazz, Class<? extends Annotation> annotationType)
    {
        ArrayList<Field> ret = new ArrayList<>();
        Field[] fields = clazz.getFields();
        for (Field f: fields)
        {
            if (f.isAnnotationPresent(annotationType))
            {
                ret.add(f);
            }
        }
        return ret;
    }
}
