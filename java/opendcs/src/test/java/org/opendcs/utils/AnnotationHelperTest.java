package org.opendcs.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.opendcs.annotations.algorithm.Input;

import decodes.tsdb.algo.AverageAlgorithm;
import ilex.util.Pair;

class AnnotationHelperTest
{
    @Test
    void test_annotated_field_retrieval() throws Exception
    {
        List<Pair<Field, Input>> fieldsWithAnnotation = AnnotationHelpers.getFieldsWithAnnotation(AverageAlgorithm.class, Input.class);
        assertEquals(1, fieldsWithAnnotation.size());
    }
}
